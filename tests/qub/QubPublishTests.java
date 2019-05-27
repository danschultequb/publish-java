package qub;

public interface QubPublishTests
{
    static void test(TestRunner runner)
    {
        runner.testGroup(QubPublish.class, () ->
        {
            runner.testGroup("setQubTest(QubTest)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    final QubPublish qubPack = new QubPublish();
                    test.assertSame(qubPack, qubPack.setQubTest(null));
                    final QubTest qubTest = qubPack.getQubTest();
                    test.assertNotNull(qubTest);
                    test.assertSame(qubTest, qubPack.getQubTest());
                });

                runner.test("with non-null", (Test test) ->
                {
                    final QubPublish qubPack = new QubPublish();
                    final QubTest qubTest = qubPack.getQubTest();
                    test.assertSame(qubPack, qubPack.setQubTest(qubTest));
                    test.assertSame(qubTest, qubPack.getQubTest());
                });
            });

            runner.testGroup("setJarCreator(JarCreator)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    final QubPublish qubPack = new QubPublish();
                    test.assertSame(qubPack, qubPack.setJarCreator(null));
                    final JarCreator jarCreator = qubPack.getJarCreator();
                    test.assertNotNull(jarCreator);
                    test.assertTrue(jarCreator instanceof JavaJarCreator);
                    test.assertSame(jarCreator, qubPack.getJarCreator());
                });

                runner.test("with non-null", (Test test) ->
                {
                    final QubPublish qubPack = new QubPublish();
                    final JarCreator jarCreator = new FakeJarCreator();
                    test.assertSame(qubPack, qubPack.setJarCreator(jarCreator));
                    test.assertSame(jarCreator, qubPack.getJarCreator());
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
                            "Usage: qub-pack [[-folder=]<folder-path-to-pack>] [-verbose]",
                            "  Used to package source and compiled code in source code projects.",
                            "  -folder: The folder to pack. This can be specified either with the -folder",
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
                            "Usage: qub-pack [[-folder=]<folder-path-to-pack>] [-verbose]",
                            "  Used to package source and compiled code in source code projects.",
                            "  -folder: The folder to pack. This can be specified either with the -folder",
                            "           argument name or without it.",
                            "  -verbose: Whether or not to show verbose logs."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with exit code 0 after tests", runner.skip(), (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    try (final Console console = new Console())
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }
                    test.assertEqual(
                        Iterable.create(
                            "Compiling...",
                            "Running tests...",
                            "",
                            "Creating compiled sources jar file...",
                            "Creating sources jar file..."
                        ),
                        Strings.getLines(output.asCharacterReadStream().getText().await()));
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });

                runner.test("with exit code 0 after tests with -verbose", runner.skip(), (Test test) ->
                {
                    final InMemoryByteStream output = new InMemoryByteStream();
                    final InMemoryByteStream error = new InMemoryByteStream();
                    try (final Console console = new Console(Iterable.create("-verbose")))
                    {
                        console.setOutputByteWriteStream(output);
                        console.setErrorByteWriteStream(error);

                        main(console);
                        test.assertEqual(0, console.getExitCode());
                    }

                    final String outputText = output.asCharacterReadStream().getText().await();
                    test.assertContains(outputText, "Compiling...");
                    test.assertContains(outputText, "VERBOSE: Parsing project.json...");
                    test.assertContains(outputText, "VERBOSE: Parsing outputs/parse.json...");
                    test.assertContains(outputText, "VERBOSE: Updating outputs/parse.json...");
                    test.assertContains(outputText, "VERBOSE: Setting project.json...");
                    test.assertContains(outputText, "VERBOSE: Setting source files...");
                    test.assertContains(outputText, "VERBOSE: Writing parse.json file...");
                    test.assertContains(outputText, "VERBOSE: Done writing parse.json file...");
                    test.assertContains(outputText, "VERBOSE: Detecting java source files to compile...");
                    test.assertContains(outputText, "VERBOSE: No source files need compilation.");
                    test.assertContains(outputText, "Running tests...");
                    test.assertContains(outputText, "VERBOSE: java.exe -classpath C:/Users/dansc/Sources/qub-java-pack/outputs;C:/qub/qub/qub-java/72/qub-java.jar;C:/qub/qub/qub-build/72/qub-build.jar;C:/qub/qub/qub-test/36/qub-test.jar qub.ConsoleTestRunner ");
                    test.assertContains(outputText, "VERBOSE: Tests exited with exit code 0.");
                    test.assertContains(outputText, "Creating compiled sources jar file...");
                    test.assertContains(outputText, "VERBOSE: Created C:/Users/dansc/Sources/qub-java-pack/outputs/qub-java-pack.jar.");
                    test.assertContains(outputText, "Creating sources jar file...");
                    test.assertContains(outputText, "VERBOSE: Created C:/Users/dansc/Sources/qub-java-pack/outputs/qub-java-pack.sources.jar.");
                    test.assertEqual("", error.asCharacterReadStream().getText().await());
                });
            });
        });
    }

    static void main(Console console)
    {
        final QubBuild build = new QubBuild();
        build.setJavaCompiler(new FakeJavaCompiler());

        final QubTest test = new QubTest();
        test.setJavaRunner(new FakeJavaRunner());
        test.setQubBuild(build);

        final QubPublish pack = new QubPublish();
        pack.setQubTest(test);
        pack.setJarCreator(new FakeJarCreator());
        pack.setShowTotalDuration(false);

        pack.main(console);
    }
}
