package qub;

public interface QubPublishTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubPublish.class, () ->
        {
            runner.testGroup("main(String[])", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(new PreConditionFailure("arguments cannot be null."),
                        () -> QubPublish.main((String[])null));
                });
            });

            runner.testGroup("getParameters(QubProcess)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubPublish.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with -?", (Test test) ->
                {
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    try (final QubProcess process = QubProcess.create("-?"))
                    {
                        process.setOutputWriteStream(output);
                        process.setErrorWriteStream(error);

                        final QubPublishParameters parameters = QubPublish.getParameters(process);
                        test.assertNull(parameters);

                        test.assertEqual(-1, process.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-publish [[--folder=]<folder-to-publish>] [--packjson] [--testjson] [--coverage[=<None|Sources|Tests|All>]] [--buildjson] [--warnings=<show|error|hide>] [--verbose] [--profiler] [--help]",
                            "  Used to published packaged source and compiled code to the qub folder.",
                            "  --folder:      The folder to publish. Defaults to the current folder.",
                            "  --packjson:    Whether or not to read and write a pack.json file. Defaults to true.",
                            "  --testjson:    Whether or not to write the test results to a test.json file.",
                            "  --coverage(c): Whether or not to collect code coverage information while running tests.",
                            "  --buildjson:   Whether or not to read and write a build.json file. Defaults to true.",
                            "  --warnings:    How to handle build warnings. Can be either \"show\", \"error\", or \"hide\". Defaults to \"show\".",
                            "  --verbose(v):  Whether or not to show verbose logs.",
                            "  --profiler:    Whether or not this application should pause before it is run to allow a profiler to be attached.",
                            "  --help(?):     Show the help message for this application."),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                });

                runner.test("with no command line arguments", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create())
                    {
                        final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                        process.setOutputWriteStream(output);

                        final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                        process.setErrorWriteStream(error);

                        final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                        fileSystem.createRoot("C:/").await();
                        process.setFileSystem(fileSystem);

                        final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                        process.setCurrentFolder(currentFolder);

                        final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                        process.setDefaultApplicationLauncher(defaultApplicationLauncher);

                        final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                        final Folder qubBuildDataFolder = qubFolder.getProjectDataFolder("qub", "build-java").await();

                        final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                        environmentVariables.set("QUB_HOME", qubFolder.toString());
                        process.setEnvironmentVariables(environmentVariables);

                        final String jvmClassPath = "C:/fake-jvm-classpath";
                        process.setJVMClasspath(jvmClassPath);

                        final QubPublishParameters parameters = QubPublish.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertTrue(parameters.getBuildJson());
                        test.assertEqual(Coverage.None, parameters.getCoverage());
                        test.assertSame(defaultApplicationLauncher, parameters.getDefaultApplicationLauncher());
                        test.assertSame(environmentVariables, parameters.getEnvironmentVariables());
                        test.assertSame(error, parameters.getErrorWriteStream());
                        test.assertEqual(currentFolder, parameters.getFolderToBuild());
                        test.assertEqual(currentFolder, parameters.getFolderToPack());
                        test.assertEqual(currentFolder, parameters.getFolderToTest());
                        test.assertSame(process.getInputReadStream(), parameters.getInputReadStream());
                        test.assertEqual(jvmClassPath, parameters.getJvmClassPath());
                        test.assertSame(output, parameters.getOutputWriteStream());
                        test.assertNull(parameters.getPattern());
                        test.assertTrue(parameters.getPackJson());
                        test.assertSame(process.getProcessFactory(), parameters.getProcessFactory());
                        test.assertFalse(parameters.getProfiler());
                        test.assertEqual(qubBuildDataFolder, parameters.getQubBuildDataFolder());
                        test.assertTrue(parameters.getTestJson());
                        final VerboseCharacterWriteStream verbose = parameters.getVerbose();
                        test.assertNotNull(verbose);
                        test.assertFalse(verbose.isVerbose());
                        test.assertEqual(Warnings.Show, parameters.getWarnings());

                        test.assertEqual("", output.getText().await());
                        test.assertEqual("", error.getText().await());
                    }
                });
            });

            runner.testGroup("run(QubPublishParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubPublish.run(null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("with no QUB_HOME specified", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder);
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "ERROR: Can't publish without a QUB_HOME environment variable."),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(1, exitCode);
                });

                runner.test("with failed QubPack", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create())
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aJavaFile = currentFolder.getFile("sources/A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension()))
                            .setFunction(1));
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            ""
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(1, exitCode);
                });

                runner.test("with already existing version folder", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    projectVersionFolder.create().await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "ERROR: This package (me/my-project:1) can't be published because a package with that signature already exists."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(1, exitCode);

                    test.assertFalse(projectVersionFolder.getCompiledSourcesFile().await().exists().await());
                    test.assertFalse(projectVersionFolder.getSourcesFile().await().exists().await());
                    test.assertFalse(projectVersionFolder.getProjectJSONFile().await().exists().await());
                    test.assertFalse(qubFolder.fileExists("my-project.cmd").await());
                });

                runner.test("with simple success scenario", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1..."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertFalse(qubFolder.fileExists("my-project.cmd").await());
                });

                runner.test("with mainClass in project.json", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A"))
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final File manifestFile = outputsFolder.getFile("META-INF/MANIFEST.MF").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addManifestFile(manifestFile)
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1..."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Manifest File:",
                            "C:/current/folder/outputs/META-INF/MANIFEST.MF",
                            "",
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A"))
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -classpath %~dp0me/my-project/versions/1/my-project.jar A %*"),
                        Strings.getLines(qubFolder.getFileContentsAsString("my-project.cmd").await()));
                });

                runner.test("with mainClass and dependencies in project.json", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final QubProjectVersionFolder meMyOtherProject5Folder = qubFolder.getProjectVersionFolder("me", "my-other-project", "5").await();
                    meMyOtherProject5Folder.getCompiledSourcesFile().await().create().await();
                    final QubProjectVersionFolder youStuff731Folder = qubFolder.getProjectVersionFolder("you", "stuff", "7.3.1").await();
                    youStuff731Folder.getCompiledSourcesFile().await().create().await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher(projectVersionFolder.getPublisherName().await())
                            .setProject(projectVersionFolder.getProjectName().await())
                            .setVersion(projectVersionFolder.getVersion().await())
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setDependencies(Iterable.create(
                                    meMyOtherProject5Folder.getProjectSignature().await(),
                                    youStuff731Folder.getProjectSignature().await())))
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final File manifestFile = outputsFolder.getFile("META-INF/MANIFEST.MF").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(Iterable.create(
                                outputsFolder.toString(),
                                meMyOtherProject5Folder.getCompiledSourcesFile().await().toString(),
                                youStuff731Folder.getCompiledSourcesFile().await().toString()))
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(
                                outputsFolder.toString(),
                                youStuff731Folder.getCompiledSourcesFile().await().toString(),
                                meMyOtherProject5Folder.getCompiledSourcesFile().await().toString(),
                                jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addManifestFile(manifestFile)
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1..."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Manifest File:",
                            "C:/current/folder/outputs/META-INF/MANIFEST.MF",
                            "",
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setDependencies(Iterable.create(
                                    meMyOtherProject5Folder.getProjectSignature().await(),
                                    youStuff731Folder.getProjectSignature().await())))
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -classpath %~dp0me/my-project/versions/1/my-project.jar;%~dp0you/stuff/versions/7.3.1/stuff.jar;%~dp0me/my-other-project/versions/5/my-other-project.jar A %*"),
                        Strings.getLines(qubFolder.getFileContentsAsString("my-project.cmd").await()));
                });

                runner.test("with mainClass and transitive dependencies in project.json", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder youStuff731Folder = qubFolder.getProjectVersionFolder("you", "stuff", "7.3.1").await();
                    youStuff731Folder.getCompiledSourcesFile().await().create().await();
                    youStuff731Folder.getProjectJSONFile().await()
                        .setContentsAsString(
                            ProjectJSON.create()
                                .setPublisher(youStuff731Folder.getPublisherName().await())
                                .setProject(youStuff731Folder.getProjectName().await())
                                .setVersion(youStuff731Folder.getVersion().await())
                                .setJava(ProjectJSONJava.create())
                                .toString())
                        .await();
                    final QubProjectVersionFolder meMyOtherProject5Folder = qubFolder.getProjectVersionFolder("me", "my-other-project", "5").await();
                    meMyOtherProject5Folder.getProjectJSONFile().await()
                        .setContentsAsString(
                            ProjectJSON.create()
                                .setPublisher(meMyOtherProject5Folder.getPublisherName().await())
                                .setProject(meMyOtherProject5Folder.getProjectName().await())
                                .setVersion(meMyOtherProject5Folder.getVersion().await())
                                .setJava(ProjectJSONJava.create()
                                    .setDependencies(Iterable.create(
                                        youStuff731Folder.getProjectSignature().await())))
                                .toString())
                        .await();
                    meMyOtherProject5Folder.getCompiledSourcesFile().await().create().await();
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher(projectVersionFolder.getPublisherName().await())
                            .setProject(projectVersionFolder.getProjectName().await())
                            .setVersion(projectVersionFolder.getVersion().await())
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setDependencies(Iterable.create(
                                    meMyOtherProject5Folder.getProjectSignature().await())))
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final File manifestFile = outputsFolder.getFile("META-INF/MANIFEST.MF").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(Iterable.create(
                                outputsFolder.toString(),
                                meMyOtherProject5Folder.getCompiledSourcesFile().await().toString(),
                                youStuff731Folder.getCompiledSourcesFile().await().toString()))
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(
                                outputsFolder.toString(),
                                meMyOtherProject5Folder.getCompiledSourcesFile().await().toString(),
                                youStuff731Folder.getCompiledSourcesFile().await().toString(),
                                jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addManifestFile(manifestFile)
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1..."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Manifest File:",
                            "C:/current/folder/outputs/META-INF/MANIFEST.MF",
                            "",
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setDependencies(Iterable.create(
                                    meMyOtherProject5Folder.getProjectSignature().await())))
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -classpath %~dp0me/my-project/versions/1/my-project.jar;%~dp0me/my-other-project/versions/5/my-other-project.jar;%~dp0you/stuff/versions/7.3.1/stuff.jar A %*"),
                        Strings.getLines(qubFolder.getFileContentsAsString("my-project.cmd").await()));
                });

                runner.test("with mainClass and shortcutName in project.json", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setShortcutName("foo"))
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final File manifestFile = outputsFolder.getFile("META-INF/MANIFEST.MF").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addManifestFile(manifestFile)
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1..."
                        ),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Manifest File:",
                            "C:/current/folder/outputs/META-INF/MANIFEST.MF",
                            "",
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setMainClass("A")
                                .setShortcutName("foo"))
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -classpath %~dp0me/my-project/versions/1/my-project.jar A %*"),
                        Strings.getLines(qubFolder.getFileContentsAsString("foo.cmd").await()));
                });

                runner.test("with dependent and non-dependent published project", (Test test) ->
                {
                    final InMemoryCharacterToByteStream input = InMemoryCharacterToByteStream.create().endOfStream();
                    final InMemoryCharacterToByteStream output = InMemoryCharacterToByteStream.create();
                    final InMemoryCharacterToByteStream error = InMemoryCharacterToByteStream.create();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("C:/").await();
                    final Folder currentFolder = fileSystem.getFolder("C:/current/folder/").await();
                    final File projectJsonFile = currentFolder.getFile("project.json").await();
                    projectJsonFile.setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                            .toString());
                    final Folder outputsFolder = currentFolder.getFolder("outputs").await();
                    final File aClassFile = outputsFolder.getFile("A.class").await();
                    final Folder sourcesFolder = currentFolder.getFolder("sources").await();
                    final File aJavaFile = sourcesFolder.getFile("A.java").await();
                    aJavaFile.setContentsAsString("A.java source").await();
                    final FakeDefaultApplicationLauncher defaultApplicationLauncher = new FakeDefaultApplicationLauncher();
                    final String jvmClassPath = "C:/fake-jvm-classpath";
                    final FakeProcessFactory processFactory = new FakeProcessFactory(test.getParallelAsyncRunner(), currentFolder)
                        .add(new FakeJavacProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addOutputFolder(outputsFolder)
                            .addXlintUnchecked()
                            .addXlintDeprecation()
                            .addClasspath(outputsFolder)
                            .addSourceFile(aJavaFile.relativeTo(currentFolder))
                            .setFunctionAutomatically())
                        .add(new FakeConsoleTestRunnerProcessRun()
                            .setWorkingFolder(currentFolder)
                            .addClasspath(Iterable.create(outputsFolder.toString(), jvmClassPath))
                            .addConsoleTestRunnerFullClassName()
                            .addProfiler(false)
                            .addVerbose(false)
                            .addTestJson(true)
                            .addOutputFolder(outputsFolder)
                            .addCoverage(Coverage.None)
                            .addFullClassNamesToTest(Iterable.create(aJavaFile.getNameWithoutFileExtension())))
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(sourcesFolder)
                            .addCreate()
                            .addJarFile("my-project.sources.jar")
                            .addContentFilePath(aJavaFile.relativeTo(sourcesFolder))
                            .setFunctionAutomatically())
                        .add(new FakeJarProcessRun()
                            .setWorkingFolder(outputsFolder)
                            .addCreate()
                            .addJarFile("my-project.jar")
                            .addContentFilePath(aClassFile.relativeTo(outputsFolder))
                            .setFunctionAutomatically());
                    final QubFolder qubFolder = QubFolder.get(fileSystem.getFolder("C:/qub/").await());
                    final QubProjectVersionFolder projectVersionFolder = qubFolder.getProjectVersionFolder("me", "my-project", "1").await();
                    final QubProjectVersionFolder meOtherProject5Folder = qubFolder.getProjectVersionFolder("me", "other-project", "5").await();
                    meOtherProject5Folder.getProjectJSONFile().await()
                        .setContentsAsString(
                            ProjectJSON.create()
                                .setPublisher(meOtherProject5Folder.getPublisherName().await())
                                .setProject(meOtherProject5Folder.getProjectName().await())
                                .setVersion(meOtherProject5Folder.getVersion().await())
                                .setJava(ProjectJSONJava.create()
                                    .setDependencies(Iterable.create(projectVersionFolder.getProjectSignature().await())))
                                .toString())
                        .await();
                    final QubProjectVersionFolder youStuff731Folder = qubFolder.getProjectVersionFolder("you", "stuff", "7.3.1").await();
                    youStuff731Folder.getProjectJSONFile().await()
                        .setContentsAsString(
                            ProjectJSON.create()
                                .setPublisher(youStuff731Folder.getPublisherName().await())
                                .setProject(youStuff731Folder.getProjectName().await())
                                .setVersion(youStuff731Folder.getVersion().await())
                                .setJava(ProjectJSONJava.create())
                                .toString())
                        .await();

                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    environmentVariables.set("QUB_HOME", qubFolder.toString());
                    final QubPublishParameters parameters = new QubPublishParameters(input, output, error, currentFolder, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);

                    final int exitCode = QubPublish.run(parameters);

                    test.assertEqual(
                        Iterable.create(
                            "Compiling 1 file...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing me/my-project@1...",
                            "The following projects should be updated to use me/my-project@1:",
                            "  me/other-project@5"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        Iterable.create(),
                        Strings.getLines(error.getText().await()));
                    test.assertEqual(0, exitCode);

                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.class"),
                        Strings.getLines(projectVersionFolder.getCompiledSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        Iterable.create(
                            "Content Files:",
                            "A.java"),
                        Strings.getLines(projectVersionFolder.getSourcesFile().await().getContentsAsString().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("me")
                            .setProject("my-project")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                            .toString(),
                        projectVersionFolder.getProjectJSONFile().await().getContentsAsString().await());
                    test.assertFalse(qubFolder.fileExists("my-project.cmd").await());
                });
            });
        });
    }
}
