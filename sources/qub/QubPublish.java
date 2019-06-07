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

        if (shouldShowUsage(console))
        {
            console.writeLine("Usage: qub-publish [[-folder=]<folder-path-to-publish>] [-verbose]").await();
            console.writeLine("  Used to publish packaged source and compiled code to the qub folder.").await();
            console.writeLine("  -folder: The folder to publish. This can be specified either with the -folder").await();
            console.writeLine("           argument name or without it.").await();
            console.writeLine("  -verbose: Whether or not to show verbose logs.").await();
            console.setExitCode(-1);
        }
        else
        {
            final boolean profiler = Profiler.takeProfilerArgument(console);
            if (profiler)
            {
                Profiler.waitForProfiler(console, QubPublish.class).await();
            }

            final boolean showTotalDuration = getShowTotalDuration();
            final Stopwatch stopwatch = console.getStopwatch();
            if (showTotalDuration)
            {
                stopwatch.start();
            }
            try
            {
                final QubPack qubPack = getQubPack();
                qubPack.setShowTotalDuration(false);
                qubPack.main(console);

                if (console.getExitCode() == 0)
                {
                    final String qubHome = console.getEnvironmentVariable("QUB_HOME");
                    if (Strings.isNullOrEmpty(qubHome))
                    {
                        error(console, "Cannot publish without a QUB_HOME environment variable.").await();
                    }
                    else
                    {
                        final Folder folderToPublish = getFolderToPublish(console);
                        final Folder outputFolder = folderToPublish.getFolder("outputs").await();

                        final File projectJsonFile = folderToPublish.getFile("project.json").await();
                        final ProjectJSON projectJSON = ProjectJSON.parse(projectJsonFile).await();
                        final String publisher = projectJSON.getPublisher();
                        final String project = projectJSON.getProject();
                        final String version = projectJSON.getVersion();

                        final Folder qubFolder = console.getFileSystem().getFolder(qubHome).await();
                        final Folder publisherFolder = qubFolder.getFolder(publisher).await();
                        final Folder projectFolder = publisherFolder.getFolder(project).await();
                        final Folder versionFolder = projectFolder.getFolder(version).await();
                        if (versionFolder.exists().await())
                        {
                            error(console, "This package (" + publisher + "/" + project + ":" + version + ") can't be published because a package with that signature already exists.").await();
                        }
                        else
                        {
                            final File compiledSourcesJarFile = outputFolder.getFile(project + ".jar").await();
                            final File sourcesJarFile = outputFolder.getFile(project + ".sources.jar").await();

                            console.writeLine("Publishing...").await();
                            projectJsonFile.copyToFolder(versionFolder).await();
                            compiledSourcesJarFile.copyToFolder(versionFolder).await();
                            sourcesJarFile.copyToFolder(versionFolder).await();
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

    private static boolean shouldShowUsage(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        return console.getCommandLine().contains(
            (CommandLineArgument argument) ->
            {
                final String argumentString = argument.toString();
                return argumentString.equals("/?") || argumentString.equals("-?");
            });
    }

    private static Path getFolderPathToPublish(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        Path result = null;
        final CommandLine commandLine = console.getCommandLine();
        if (commandLine.any())
        {
            CommandLineArgument folderArgument = commandLine.get("folder");
            if (folderArgument == null)
            {
                folderArgument = commandLine.getArguments()
                    .first((CommandLineArgument argument) -> argument.getName() == null);
            }
            if (folderArgument != null)
            {
                result = Path.parse(folderArgument.getValue());
            }
        }

        if (result == null)
        {
            result = console.getCurrentFolderPath();
        }

        if (!result.isRooted())
        {
            result = console.getCurrentFolderPath().resolve(result).await();
        }

        PostCondition.assertNotNull(result, "result");
        PostCondition.assertTrue(result.isRooted(), "result.isRooted()");

        return result;
    }

    private static Folder getFolderToPublish(Console console)
    {
        PreCondition.assertNotNull(console, "console");

        final Path folderPathToPublish = getFolderPathToPublish(console);
        final FileSystem fileSystem = console.getFileSystem();
        final Folder result = fileSystem.getFolder(folderPathToPublish).await();

        PostCondition.assertNotNull(result, "result");

        return result;
    }

    public static boolean isVerbose(Console console)
    {
        boolean result = false;

        CommandLineArgument verboseArgument = console.getCommandLine().get("verbose");
        if (verboseArgument != null)
        {
            final String verboseArgumentValue = verboseArgument.getValue();
            result = Strings.isNullOrEmpty(verboseArgumentValue) ||
                Booleans.isTrue(java.lang.Boolean.valueOf(verboseArgumentValue));
        }

        return result;
    }

    public static Result<Void> verbose(Console console, String message)
    {
        return verbose(console, false, message);
    }

    public static Result<Void> verbose(Console console, boolean showTimestamp, String message)
    {
        PreCondition.assertNotNull(console, "console");
        PreCondition.assertNotNull(message, "message");

        Result<Void> result = Result.success();
        if (isVerbose(console))
        {
            result = console.writeLine("VERBOSE" + (showTimestamp ? "(" + System.currentTimeMillis() + ")" : "") + ": " + message)
                .then(() -> {});
        }

        PostCondition.assertNotNull(result, "result");

        return result;
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
}