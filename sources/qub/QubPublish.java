package qub;

public class QubPublish
{
    private QubPack qubPack;
    private Boolean showTotalDuration;

    /**
     * Set the QubPack object that will be used to package the source code.
     * @param qubPack The QubPack object that will be used to package the source code.
     * @return This object for method chaining.
     */
    public QubPublish setQubPack(QubPack qubPack)
    {
        this.qubPack = qubPack;
        return this;
    }

    /**
     * Get the QubPack object that will be used to package the source code. If no QubPack object has
     * been set, a default one will be created and returned.
     * @return The QubPack object that will be used to test the source code.
     */
    public QubPack getQubPack()
    {
        if (qubPack == null)
        {
            qubPack = new QubPack();
        }
        final QubPack result = qubPack;

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    /**
     * Set whether or not the total duration to publish will be written to the console.
     * @param showTotalDuration Whether or not the total duration to publish will be written to the
     *                          console.
     * @return This object for method chaining.
     */
    public QubPublish setShowTotalDuration(boolean showTotalDuration)
    {
        this.showTotalDuration = showTotalDuration;
        return this;
    }

    /**
     * Get whether or not the total duration to publish will be written to the console.
     * @return Whether or not the total duration to publish will be written to the console.
     */
    public boolean getShowTotalDuration()
    {
        if (showTotalDuration == null)
        {
            showTotalDuration = true;
        }
        return showTotalDuration;
    }

    public void main(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final CommandLineParameters parameters = console.createCommandLineParameters();
        final CommandLineParameter<Folder> folderToPublishParameter = parameters.addPositionalFolder("folder", console)
            .setValueName("<folder-to-publish>")
            .setDescription("The folder to publish. Defaults to the current folder.");
        final CommandLineParameterProfiler profiler = parameters.addProfiler(console, QubPublish.class);
        final CommandLineParameterBoolean help = parameters.addHelp();

        if (help.getValue().await())
        {
            parameters.writeHelpLines(console, "qub-publish", "Used to published packaged source and compiled code to the qub folder.").await();
            console.setExitCode(-1);
        }
        else
        {
            profiler.await();
            profiler.removeValue().await();

            final boolean showTotalDuration = getShowTotalDuration();
            final Stopwatch stopwatch = console.getStopwatch();
            if (showTotalDuration)
            {
                stopwatch.start();
            }
            try
            {
                final String qubHome = console.getEnvironmentVariable("QUB_HOME")
                    .catchError(NotFoundException.class)
                    .await();
                if (Strings.isNullOrEmpty(qubHome))
                {
                    error(console, "Can't publish without a QUB_HOME environment variable.").await();
                }
                else
                {
                    final QubPack qubPack = getQubPack();
                    qubPack.setShowTotalDuration(false);
                    qubPack.main(console);

                    if (console.getExitCode() == 0)
                    {
                        final Folder folderToPublish = folderToPublishParameter.getValue().await();
                        final Folder outputFolder = folderToPublish.getFolder("outputs").await();

                        final File projectJsonFile = folderToPublish.getFile("project.json").await();
                        final ProjectJSON projectJSON = ProjectJSON.parse(projectJsonFile).await();
                        final String publisher = projectJSON.getPublisher();
                        final String project = projectJSON.getProject();
                        String version = projectJSON.getVersion();
                        final Folder qubFolder = console.getFileSystem().getFolder(qubHome).await();

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
                            error(console, "This package (" + publisher + "/" + project + ":" + version + ") can't be published because a package with that signature already exists.").await();
                        }
                        else
                        {
                            final File compiledSourcesJarFile = outputFolder.getFile(project + ".jar").await();
                            final File sourcesJarFile = outputFolder.getFile(project + ".sources.jar").await();
                            final File testsJarFile = outputFolder.getFile(project + ".tests.jar").await();

                            console.writeLine("Publishing " + publisher + "/" + project + "@" + version + "...").await();
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

                                    String capturedJVMClasspath = "";
                                    if (Booleans.isTrue(projectJsonJava.getCaptureVMArguments()))
                                    {
                                        capturedJVMClasspath = " --jvm.classpath=" + classpath;
                                    }

                                    final File shortcutFile = qubFolder.getFile(shortcutName + ".cmd").await();
                                    final String shortcutFileContents =
                                        "@echo OFF\n" +
                                        "java -classpath " + classpath + " " + mainClass + capturedJVMClasspath + " %*\n";
                                    shortcutFile.setContentsAsString(shortcutFileContents).await();
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
                                console.writeLine("The following projects should be updated to use " + publisher + "/" + project + "@" + version + ":").await();
                                for (final String projectToUpdate : projectsToUpdate)
                                {
                                    console.writeLine("  " + projectToUpdate).await();
                                }
                            }
                        }
                    }
                }
            }
            finally
            {
                if (showTotalDuration)
                {
                    final Duration compilationDuration = stopwatch.stop().toSeconds();
                    console.writeLine("Done (" + compilationDuration.toString("0.0") + ")").await();
                }
            }
        }
    }

    private static Comparison compareVersionFolders(Folder lhs, Folder rhs)
    {
        final Integer lhsValue = Integers.parse(lhs.getName()).catchError().await();
        final Integer rhsValue = Integers.parse(rhs.getName()).catchError().await();
        return Comparer.compare(lhsValue, rhsValue);
    }

    public static Result<Void> error(Console console, String message)
    {
        return error(console, false, message);
    }

    public static Result<Void> error(Console console, boolean showTimestamp, String message)
    {
        PreCondition.assertNotNull(console, "console");
        PreCondition.assertNotNull(message, "message");

        final Result<Void> result = console.writeLine("ERROR" + (showTimestamp ? "(" + System.currentTimeMillis() + ")" : "") + ": " + message).then(() -> {});
        console.incrementExitCode();

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static void main(String[] args)
    {
        Console.run(args, (Console console) -> new QubPublish().main(console));
    }

    private static Result<Folder> getLatestVersionFolder(Folder qubFolder, String publisher, String project)
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

    private static Result<String> getLatestVersion(Folder qubFolder, String publisher, String project)
    {
        return getLatestVersionFolder(qubFolder, publisher, project)
            .then(Folder::getName);
    }
}