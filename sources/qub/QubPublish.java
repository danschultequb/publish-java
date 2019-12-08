package qub;

public interface QubPublish
{
    static void main(String[] args)
    {
        Console.run(args, (Console console) -> QubPublish.main(console));
    }

    static CommandLineParameter<Folder> addFolderToPublishParameter(CommandLineParameters parameters, Process process)
    {
        PreCondition.assertNotNull(parameters, "parameters");
        PreCondition.assertNotNull(process, "process");

        return parameters.addPositionalFolder("folder", process)
            .setValueName("<folder-to-publish>")
            .setDescription("The folder to publish. Defaults to the current folder.");
    }

    static QubPublishParameters getParameters(Process process)
    {
        PreCondition.assertNotNull(process, "process");

        final CommandLineParameters parameters = process.createCommandLineParameters()
            .setApplicationName("qub-publish")
            .setApplicationDescription("Used to published packaged source and compiled code to the qub folder.");
        final CommandLineParameter<Folder> folderToPublishParameter = QubPublish.addFolderToPublishParameter(parameters, process);
        final CommandLineParameterBoolean packJsonParameter = QubPack.addPackJsonParameter(parameters);
        final CommandLineParameterBoolean testJsonParameter = QubTest.addTestJsonParameter(parameters);
        final CommandLineParameter<Coverage> coverageParameter = QubTest.addCoverageParameter(parameters);
        final CommandLineParameterBoolean buildJsonParameter = QubBuild.addBuildJsonParameter(parameters);
        final CommandLineParameter<Warnings> warningsParameter = QubBuild.addWarningsParameter(parameters);
        final CommandLineParameterVerbose verboseParameter = parameters.addVerbose(process);
        final CommandLineParameterProfiler profilerParameter = parameters.addProfiler(process, QubPublish.class);
        final CommandLineParameterHelp helpParameter = parameters.addHelp();

        QubPublishParameters result = null;
        if (!helpParameter.showApplicationHelpLines(process).await())
        {
            profilerParameter.await();
            profilerParameter.removeValue().await();

            final ByteWriteStream output = process.getOutputByteWriteStream();
            final ByteWriteStream error = process.getErrorByteWriteStream();
            final Folder folderToPublish = folderToPublishParameter.removeValue().await();
            final EnvironmentVariables environmentVariables = process.getEnvironmentVariables();
            final ProcessFactory processFactory = process.getProcessFactory();
            final DefaultApplicationLauncher defaultApplicationLauncher = process.getDefaultApplicationLauncher();
            final String jvmClassPath = process.getJVMClasspath().await();

            result = new QubPublishParameters(output, error, folderToPublish, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath)
                .setPackJson(packJsonParameter.removeValue().await())
                .setTestJson(testJsonParameter.removeValue().await())
                .setCoverage(coverageParameter.removeValue().await())
                .setBuildJson(buildJsonParameter.removeValue().await())
                .setWarnings(warningsParameter.removeValue().await())
                .setVerbose(verboseParameter.getVerboseCharacterWriteStream().await())
                .setProfiler(profilerParameter.removeValue().await());
        }

        return result;
    }

    static void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final QubPublishParameters parameters = QubPublish.getParameters(console);
        if (parameters != null)
        {
            final Stopwatch stopwatch = console.getStopwatch();
            stopwatch.start();
            try
            {
                console.setExitCode(QubPublish.run(parameters));
            }
            finally
            {
                final Duration compilationDuration = stopwatch.stop().toSeconds();
                console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
            }
        }
    }

    static int run(QubPublishParameters parameters)
    {
        PreCondition.assertNotNull(parameters, "parameters");

        final CharacterWriteStream output = parameters.getOutputCharacterWriteStream();
        final EnvironmentVariables environmentVariables = parameters.getEnvironmentVariables();
        final Folder folderToPublish = parameters.getFolderToPublish();

        int exitCode = 0;
        try
        {
            final String qubHome = environmentVariables.get("QUB_HOME")
                .convertError(NotFoundException.class, () -> new NotFoundException("Can't publish without a QUB_HOME environment variable."))
                .await();

            exitCode = QubPack.run(parameters);
            if (exitCode == 0)
            {
                final Folder outputFolder = folderToPublish.getFolder("outputs").await();

                final File projectJsonFile = folderToPublish.getFile("project.json").await();
                final ProjectJSON projectJSON = ProjectJSON.parse(projectJsonFile).await();
                final String publisher = projectJSON.getPublisher();
                final String project = projectJSON.getProject();
                String version = projectJSON.getVersion();
                final Folder qubFolder = folderToPublish.getFileSystem().getFolder(qubHome).await();

                if (Strings.isNullOrEmpty(version))
                {
                    version = getLatestVersion(qubFolder, publisher, project)
                        .catchError(NotFoundException.class)
                        .await();
                    if (!Strings.isNullOrEmpty(version))
                    {
                        final Integer intVersion = Integers.parse(version).catchError().await();
                        if (intVersion == null)
                        {
                            version = null;
                        }
                        else
                        {
                            version = Integers.toString(intVersion + 1);
                        }
                    }
                    if (Strings.isNullOrEmpty(version))
                    {
                        version = "1";
                    }
                }

                final Folder versionFolder = qubFolder
                    .getFolder(publisher).await()
                    .getFolder(project).await()
                    .getFolder(version).await();
                if (versionFolder.exists().await())
                {
                    throw new AlreadyExistsException("This package (" + publisher + "/" + project + ":" + version + ") can't be published because a package with that signature already exists.");
                }

                final File compiledSourcesJarFile = outputFolder.getFile(project + ".jar").await();
                final File sourcesJarFile = outputFolder.getFile(project + ".sources.jar").await();
                final File testsJarFile = outputFolder.getFile(project + ".tests.jar").await();

                output.writeLine("Publishing " + publisher + "/" + project + "@" + version + "...").await();
                projectJsonFile.copyToFolder(versionFolder).await();
                final File versionFolderCompiledSourcesJarFile = versionFolder.getFile(compiledSourcesJarFile.getName()).await();
                compiledSourcesJarFile.copyTo(versionFolderCompiledSourcesJarFile).await();
                sourcesJarFile.copyToFolder(versionFolder).await();
                testsJarFile.copyToFolder(versionFolder)
                    .catchError(FileNotFoundException.class)
                    .await();

                final ProjectJSONJava projectJsonJava = projectJSON.getJava();
                if (projectJsonJava != null)
                {
                    final String mainClass = projectJsonJava.getMainClass();
                    if (mainClass != null)
                    {
                        String shortcutName = projectJsonJava.getShortcutName();
                        if (Strings.isNullOrEmpty(shortcutName))
                        {
                            shortcutName = projectJSON.getProject();
                        }

                        String classpath = "%~dp0" + versionFolderCompiledSourcesJarFile.relativeTo(qubFolder);
                        final Iterable<Dependency> dependencies = QubBuild.getAllDependencies(qubFolder, projectJsonJava.getDependencies()).getKeys();
                        if (!Iterable.isNullOrEmpty(dependencies))
                        {
                            for (final Dependency dependency : dependencies)
                            {
                                classpath += ";%~dp0" + dependency.getPublisher() + "/" + dependency.getProject() + "/" + dependency.getVersion() + "/" + dependency.getProject() + ".jar";
                            }
                        }

                        final File shortcutFile = qubFolder.getFile(shortcutName + ".cmd").await();
                        try (final CharacterWriteStream shortcutFileStream = shortcutFile.getContentCharacterWriteStream().await())
                        {
                            shortcutFileStream.writeLine("@echo OFF").await();
                            shortcutFileStream.writeLine("java -classpath " + classpath + " " + mainClass + " %*").await();
                        }
                    }
                }

                final List<String> projectsToUpdate = List.create();
                final Iterable<Folder> publisherFolders = qubFolder.getFolders().await();
                for (final Folder publisherFolder : publisherFolders)
                {
                    final Iterable<Folder> projectFolders = publisherFolder.getFolders().await();
                    for (final Folder projectFolder : projectFolders)
                    {
                        final Iterable<Folder> versionFolders = projectFolder.getFolders().await();
                        final Folder latestVersionFolder = versionFolders.maximum(QubPublish::compareVersionFolders);
                        if (latestVersionFolder != null)
                        {
                            final File publishedProjectJsonFile = latestVersionFolder.getFile("project.json").await();
                            final ProjectJSON publishedProjectJson = ProjectJSON.parse(publishedProjectJsonFile)
                                .catchError(FileNotFoundException.class)
                                .await();
                            if (publishedProjectJson != null)
                            {
                                final ProjectJSONJava publishedProjectJsonJava = publishedProjectJson.getJava();
                                if (publishedProjectJsonJava != null)
                                {
                                    final Iterable<Dependency> dependencies = publishedProjectJsonJava.getDependencies();
                                    if (!Iterable.isNullOrEmpty(dependencies))
                                    {
                                        final Dependency dependency = dependencies.first((Dependency d) ->
                                            Comparer.equal(d.getPublisher(), publisher) &&
                                                Comparer.equal(d.getProject(), project));
                                        if (dependency != null)
                                        {
                                            projectsToUpdate.add(publishedProjectJson.getPublisher() + "/" + publishedProjectJson.getProject() + "@" + publishedProjectJson.getVersion());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (!Iterable.isNullOrEmpty(projectsToUpdate))
                {
                    output.writeLine("The following projects should be updated to use " + publisher + "/" + project + "@" + version + ":").await();
                    for (final String projectToUpdate : projectsToUpdate)
                    {
                        output.writeLine("  " + projectToUpdate).await();
                    }
                }
            }
        }
        catch (Throwable error)
        {
            final Throwable unwrappedError = Exceptions.unwrap(error);
            if (unwrappedError instanceof PreConditionFailure ||
                unwrappedError instanceof PostConditionFailure ||
                unwrappedError instanceof NullPointerException)
            {
                throw error;
            }
            final String message = unwrappedError.getMessage();
            output.writeLine("ERROR: " + message).await();
            ++exitCode;
        }

        return exitCode;
    }

    static Comparison compareVersionFolders(Folder lhs, Folder rhs)
    {
        final Integer lhsValue = Integers.parse(lhs.getName()).catchError().await();
        final Integer rhsValue = Integers.parse(rhs.getName()).catchError().await();
        return Comparer.compare(lhsValue, rhsValue);
    }

    static Result<Folder> getLatestVersionFolder(Folder qubFolder, String publisher, String project)
    {
        PreCondition.assertNotNull(qubFolder, "qubFolder");
        PreCondition.assertNotNullAndNotEmpty(publisher, "publisher");
        PreCondition.assertNotNullAndNotEmpty(project, "project");

        return Result.create(() ->
        {
            final Folder projectFolder = qubFolder
                .getFolder(publisher).await()
                .getFolder(project).await();
            final Iterable<Folder> projectVersionFolders = projectFolder.getFolders().await();
            final Folder maximumVersionFolder = projectVersionFolders.maximum(QubPublish::compareVersionFolders);
            if (maximumVersionFolder == null)
            {
                throw new NotFoundException("No project has been published for " + Strings.quote(publisher) + " with the name " + Strings.quote(project) + ".");
            }
            return maximumVersionFolder;
        });
    }

    static Result<String> getLatestVersion(Folder qubFolder, String publisher, String project)
    {
        return getLatestVersionFolder(qubFolder, publisher, project)
            .then(Folder::getName);
    }
}