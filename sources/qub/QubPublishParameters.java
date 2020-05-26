package qub;

public class QubPublishParameters extends QubPackParameters
{
    /**
     * Create a new QubPublishParameters object.
     * @param inputReadStream The ByteReadStream that input should be read from.
     * @param outputWriteStream      The ByteWriteStream that output should be written to.
     * @param errorWriteStream       The ByteWriteStream that errors should be written to.
     * @param folderToPack               The folder that should have its tests run.
     * @param environmentVariables       The environment variables of the running process.
     * @param processFactory             The factory that will be used to create new processes.
     * @param defaultApplicationLauncher The object that will launch the default application for
     */
    public QubPublishParameters(CharacterToByteReadStream inputReadStream, CharacterToByteWriteStream outputWriteStream, CharacterToByteWriteStream errorWriteStream, Folder folderToPack, EnvironmentVariables environmentVariables, ProcessFactory processFactory, DefaultApplicationLauncher defaultApplicationLauncher, String jvmClassPath)
    {
        super(inputReadStream, outputWriteStream, errorWriteStream, folderToPack, environmentVariables, processFactory, defaultApplicationLauncher, jvmClassPath);
    }

    public Folder getFolderToPublish()
    {
        return this.getFolderToPack();
    }

    @Override
    public QubPublishParameters setPackJson(boolean packJson)
    {
        return (QubPublishParameters)super.setPackJson(packJson);
    }

    @Override
    public QubPublishParameters setPattern(String pattern)
    {
        return (QubPublishParameters)super.setPattern(pattern);
    }

    @Override
    public QubPublishParameters setCoverage(Coverage coverage)
    {
        PreCondition.assertNotNull(coverage, "coverage");

        return (QubPublishParameters)super.setCoverage(coverage);
    }

    @Override
    public QubPublishParameters setTestJson(boolean testJson)
    {
        return (QubPublishParameters)super.setTestJson(testJson);
    }

    @Override
    public QubPublishParameters setJvmClassPath(String jvmClassPath)
    {
        return (QubPublishParameters)super.setJvmClassPath(jvmClassPath);
    }

    @Override
    public QubPublishParameters setProfiler(boolean profiler)
    {
        return (QubPublishParameters)super.setProfiler(profiler);
    }

    @Override
    public QubPublishParameters setWarnings(Warnings warnings)
    {
        return (QubPublishParameters)super.setWarnings(warnings);
    }

    @Override
    public QubPublishParameters setBuildJson(boolean buildJson)
    {
        return (QubPublishParameters)super.setBuildJson(buildJson);
    }

    @Override
    public QubPublishParameters setVerbose(VerboseCharacterWriteStream verbose)
    {
        return (QubPublishParameters)super.setVerbose(verbose);
    }
}
