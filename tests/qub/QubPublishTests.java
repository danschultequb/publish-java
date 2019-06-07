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

                runner.test("with \"/?\"", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    try (final Console console = new Console(Iterable.create("/?")))
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);

                        main(console);
                        test.assertEqual(-1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-publish [[-folder=]<folder-path-to-publish>] [-verbose]",
                            "  Used to publish packaged source and compiled code to the qub folder.",
                            "  -folder: The folder to publish. This can be specified either with the -folder",
                            "           argument name or without it.",
                            "  -verbose: Whether or not to show verbose logs."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with \"-?\"", (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    try (final Console console = new Console(Iterable.create("-?")))
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);

                        main(console);
                        test.assertEqual(-1, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Usage: qub-publish [[-folder=]<folder-path-to-publish>] [-verbose]",
                            "  Used to publish packaged source and compiled code to the qub folder.",
                            "  -folder: The folder to publish. This can be specified either with the -folder",
                            "           argument name or without it.",
                            "  -verbose: Whether or not to show verbose logs."
                        ),
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
