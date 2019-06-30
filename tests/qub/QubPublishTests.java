package qub;

public interface QubPublishTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubPublish.class, () ->
        {
            runner.testGroup("setQubPack(QubPack)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    final QubPublish qubPublish = new QubPublish();
                    test.assertSame(qubPublish, qubPublish.setQubPack(null));
                    final QubPack qubPack = qubPublish.getQubPack();
                    test.assertNotNull(qubPack);
                    test.assertSame(qubPack, qubPublish.getQubPack());
                });

                runner.test("with non-null", (Test test) ->
                {
                    final QubPublish qubPublish = new QubPublish();
                    final QubPack qubPack = qubPublish.getQubPack();
                    test.assertSame(qubPublish, qubPublish.setQubPack(qubPack));
                    test.assertSame(qubPack, qubPublish.getQubPack());
                });
            });

            runner.testGroup("setShowTotalDuration(boolean)", () ->
            {
                runner.test("with false", (Test test) ->
                {
                    final QubPublish qubPublish = new QubPublish();
                    test.assertSame(qubPublish, qubPublish.setShowTotalDuration(false));
                    test.assertFalse(qubPublish.getShowTotalDuration());
                });

                runner.test("with true", (Test test) ->
                {
                    final QubPublish qubPublish = new QubPublish();
                    test.assertSame(qubPublish, qubPublish.setShowTotalDuration(true));
                    test.assertTrue(qubPublish.getShowTotalDuration());
                });
            });

            runner.testGroup("getShowTotalDuration()", () ->
            {
                runner.test("when no value has been set", (Test test) ->
                {
                    final QubPublish qubPublish = new QubPublish();
                    test.assertTrue(qubPublish.getShowTotalDuration());
                    test.assertTrue(qubPublish.getShowTotalDuration());
                });
            });

            runner.testGroup("main(String[])", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(new PreConditionFailure("args cannot be null."),
                        () -> QubPublish.main((String[])null));
                });
            });

            runner.testGroup("main(Console)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(new PreConditionFailure("console cannot be null."),
                        () -> main((Console)null));
                });

                runner.test("with \"-?\"", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    try (final Console console = new Console(CommandLineArguments.create("-?")))
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);

                        main(console);
                        test.assertEqual(-1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-publish [[--folder=]<folder-to-publish>] [--profiler] [--help]",
                            "  Used to published packaged source and compiled code to the qub folder.",
                            "  --folder: The folder to publish. Defaults to the current folder.",
                            "  --profiler: Whether or not this application should pause before it is run to allow a profiler to be attached.",
                            "  --help(?): Show the help message for this application."),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with no QUB_HOME specified", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava());
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.create());

                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "ERROR: Cannot publish without a QUB_HOME environment variable."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with failed QubPack", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava());
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        final QubBuild qubBuild = new QubBuild();
                        qubBuild.setJavaCompiler(new FakeJavaCompiler());

                        final QubTest qubTest = new QubTest();
                        final FakeJavaRunner javaRunner = new FakeJavaRunner();
                        javaRunner.setExitCode(1);
                        qubTest.setJavaRunner(javaRunner);
                        qubTest.setQubBuild(qubBuild);

                        final QubPack qubPack = new QubPack();
                        qubPack.setQubTest(qubTest);
                        qubPack.setJarCreator(new FakeJarCreator());

                        main(console, qubPack);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            ""
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with already existing version folder", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/me/my-project/1/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava());
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "ERROR: This package (me/my-project:1) can't be published because a package with that signature already exists."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertFalse(fileSystem.fileExists("/qub/me/my-project/1/my-project.jar").await());
                    test.assertFalse(fileSystem.fileExists("/qub/me/my-project/1/my-project.sources.jar").await());
                    test.assertFalse(fileSystem.fileExists("/qub/me/my-project/1/project.json").await());
                    test.assertFalse(fileSystem.fileExists("/qub/my-project.cmd").await());
                });

                runner.test("with simple success scenario", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava());
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.class",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.jar").await()));
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.java",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.sources.jar").await()));
                    test.assertEqual(
                        "{\"publisher\":\"me\",\"project\":\"my-project\",\"version\":\"1\",\"java\":{}}",
                        fileSystem.getFileContentAsString("/qub/me/my-project/1/project.json").await());
                    test.assertFalse(fileSystem.fileExists("/qub/my-project.cmd").await());
                });

                runner.test("with mainClass in project.json", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava()
                        .setMainClass("MyProject"));
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertEqual(
                        Iterable.create(
                            "Manifest file:",
                            "META-INF/MANIFEST.MF",
                            "",
                            "Files:",
                            "MyProject.class",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.jar").await()));
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.java",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.sources.jar").await()));
                    test.assertEqual(
                        "{\"publisher\":\"me\",\"project\":\"my-project\",\"version\":\"1\",\"java\":{\"mainClass\":\"MyProject\"}}",
                        fileSystem.getFileContentAsString("/qub/me/my-project/1/project.json").await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -cp %~dp0me/my-project/1/my-project.jar MyProject %*"),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/my-project.cmd").await()));
                });

                runner.test("with mainClass and dependencies in project.json", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava()
                        .setMainClass("MyProject")
                        .setDependencies(Iterable.create(
                            new Dependency()
                                .setPublisher("me")
                                .setProject("my-other-project")
                                .setVersion("5"),
                            new Dependency()
                                .setPublisher("you")
                                .setProject("stuff")
                                .setVersion("7.3.1")
                        )));
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    fileSystem.setFileContentAsString("/qub/me/my-other-project/5/my-other-project.jar", "hello").await();
                    fileSystem.setFileContentAsString("/qub/you/stuff/7.3.1/stuff.jar", "hello2").await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertEqual(
                        Iterable.create(
                            "Manifest file:",
                            "META-INF/MANIFEST.MF",
                            "",
                            "Files:",
                            "MyProject.class",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.jar").await()));
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.java",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.sources.jar").await()));
                    test.assertEqual(
                        "{\"publisher\":\"me\",\"project\":\"my-project\",\"version\":\"1\",\"java\":{\"mainClass\":\"MyProject\",\"dependencies\":[{\"publisher\":\"me\",\"project\":\"my-other-project\",\"version\":\"5\"},{\"publisher\":\"you\",\"project\":\"stuff\",\"version\":\"7.3.1\"}]}}",
                        fileSystem.getFileContentAsString("/qub/me/my-project/1/project.json").await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -cp %~dp0me/my-project/1/my-project.jar;%~dp0me/my-other-project/5/my-other-project.jar;%~dp0you/stuff/7.3.1/stuff.jar MyProject %*"),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/my-project.cmd").await()));
                });

                runner.test("with mainClass and transitive dependencies in project.json", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON aProjectJSON = new ProjectJSON()
                        .setProject("a")
                        .setPublisher("me")
                        .setVersion("1")
                        .setJava(new ProjectJSONJava()
                            .setMainClass("MyProject")
                            .setDependencies(Iterable.create(
                                new Dependency()
                                    .setPublisher("me")
                                    .setProject("b")
                                    .setVersion("5"))));
                    final ProjectJSON bProjectJSON = new ProjectJSON()
                        .setProject("b")
                        .setPublisher("me")
                        .setVersion("5")
                        .setJava(new ProjectJSONJava()
                            .setDependencies(Iterable.create(
                                new Dependency()
                                    .setPublisher("me")
                                    .setProject("c")
                                    .setVersion("7"))));
                    final ProjectJSON cProjectJSON = new ProjectJSON()
                        .setProject("c")
                        .setPublisher("me")
                        .setVersion("7");
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(aProjectJSON::write).toString()).await();
                    fileSystem.setFileContentAsString("/qub/me/b/5/b.jar", "hello").await();
                    fileSystem.setFileContentAsString("/qub/me/b/5/project.json", JSON.object(bProjectJSON::write).toString()).await();
                    fileSystem.setFileContentAsString("/qub/me/c/7/c.jar", "hello").await();
                    fileSystem.setFileContentAsString("/qub/me/c/7/project.json", JSON.object(cProjectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertEqual(
                        Iterable.create(
                            "Manifest file:",
                            "META-INF/MANIFEST.MF",
                            "",
                            "Files:",
                            "MyProject.class",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/a/1/a.jar").await()));
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.java",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/a/1/a.sources.jar").await()));
                    test.assertEqual(
                        "{\"publisher\":\"me\",\"project\":\"a\",\"version\":\"1\",\"java\":{\"mainClass\":\"MyProject\",\"dependencies\":[{\"publisher\":\"me\",\"project\":\"b\",\"version\":\"5\"}]}}",
                        fileSystem.getFileContentAsString("/qub/me/a/1/project.json").await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -cp %~dp0me/a/1/a.jar;%~dp0me/b/5/b.jar;%~dp0me/c/7/c.jar MyProject %*"),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/a.cmd").await()));
                });

                runner.test("with mainClass and shortcutName in project.json", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    fileSystem.createFolder("/qub/").await();
                    fileSystem.setFileContentAsString("/sources/MyProject.java", "hello").await();
                    final ProjectJSON projectJSON = new ProjectJSON();
                    projectJSON.setProject("my-project");
                    projectJSON.setPublisher("me");
                    projectJSON.setVersion("1");
                    projectJSON.setJava(new ProjectJSONJava()
                        .setMainClass("MyProject")
                        .setShortcutName("foo"));
                    fileSystem.setFileContentAsString(
                        "/project.json",
                        JSON.object(projectJSON::write).toString()).await();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);
                        console.setFileSystem(fileSystem);
                        console.setCurrentFolderPath(Path.parse("/"));
                        console.setEnvironmentVariables(Map.<String,String>create()
                            .set("QUB_HOME", "/qub/"));

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating sources jar file...",
                            "Creating compiled sources jar file...",
                            "Publishing..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                    test.assertEqual(
                        Iterable.create(
                            "Manifest file:",
                            "META-INF/MANIFEST.MF",
                            "",
                            "Files:",
                            "MyProject.class",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.jar").await()));
                    test.assertEqual(
                        Iterable.create(
                            "Files:",
                            "MyProject.java",
                            ""),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/me/my-project/1/my-project.sources.jar").await()));
                    test.assertEqual(
                        "{\"publisher\":\"me\",\"project\":\"my-project\",\"version\":\"1\",\"java\":{\"mainClass\":\"MyProject\",\"shortcutName\":\"foo\"}}",
                        fileSystem.getFileContentAsString("/qub/me/my-project/1/project.json").await());
                    test.assertFalse(fileSystem.fileExists("/qub/my-project.cmd").await());
                    test.assertEqual(
                        Iterable.create(
                            "@echo OFF",
                            "java -cp %~dp0me/my-project/1/my-project.jar MyProject %*"),
                        Strings.getLines(fileSystem.getFileContentAsString("/qub/foo.cmd").await()));
                });
            });
        });
    }

    static void main(Console console)
    {
        final QubBuild qubBuild = new QubBuild();
        qubBuild.setJavaCompiler(new FakeJavaCompiler());

        final QubTest qubTest = new QubTest();
        qubTest.setJavaRunner(new FakeJavaRunner());
        qubTest.setQubBuild(qubBuild);

        final QubPack qubPack = new QubPack();
        qubPack.setJarCreator(new FakeJarCreator());
        qubPack.setQubTest(qubTest);

        main(console, qubPack);
    }

    static void main(Console console, QubPack qubPack)
    {
        final QubPublish qubPublish = new QubPublish();
        qubPublish.setQubPack(qubPack);
        qubPublish.setShowTotalDuration(false);

        qubPublish.main(console);
    }
}
