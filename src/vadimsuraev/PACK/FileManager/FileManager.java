package vadimsuraev.PACK.FileManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.ChunkChainDataTypes.FileAndOffset;

public abstract class FileManager
{
	public enum FileTypes
	{
	    DUMMY_FILE_TYPE,
	    CHUNK_CTRL_FILE_TYPE,
	    CHUNK_DATA_FILE_TYPE,
	    CHAIN_FILE_TYPE
	};
    protected static HashMap<Long, byte[]> chunkCBfileMap;
    protected static HashMap<Long, byte []> chainfileMap;
    protected static long LastFileId;
    protected static List<String> allChunkDataFiles;
    protected static List<String> allChunkCtrlFiles;
    protected static List<String> allChainFiles;
    protected static final long FileMaxSize = 1024 * 1000;
    protected static boolean GlobalsInitiated = false;
    protected static long RefCount = 0;

    protected static ReentrantLock chunkDataFilesMutex;
    protected static ReentrantLock chunkCtrlFilesMutex;
    protected static ReentrantLock chainFilesMutex;
    protected static String WorkingDirectory;

    protected static void InitGlobals(String workingDirectory)
    {
        chunkCtrlFilesMutex = new ReentrantLock();
        chunkDataFilesMutex = new ReentrantLock();
        chainFilesMutex = new ReentrantLock();
        allChunkDataFiles = new LinkedList<String>();
        allChunkCtrlFiles = new LinkedList<String>();
        allChainFiles = new LinkedList<String>();
        WorkingDirectory = workingDirectory;
        LastFileId = 0;
        GlobalsInitiated = true;
        chunkCBfileMap = new HashMap<Long, byte[]>();
        chainfileMap = new HashMap<Long, byte []>();
    }
    public FileManager()
    {
    }
    public String[] GetAllChunkDataFiles()
    {
        return (String[]) allChunkDataFiles.toArray();
    }

    public String[] GetAllChunkCtrlFiles()
    {
        return (String[]) allChunkCtrlFiles.toArray();
    }
    protected String GetFilePathById(long fileId)
    {
        String filePath;
        return null;
    }

    public abstract byte[] GetChunk(FileAndOffset fo, long size);
    public abstract FileAndOffset AddNewChunk(byte[] data, long offset, long size, long chunkId);
    public abstract long AddUpdateChainFile(byte[] data, long chainId);
    public abstract void AddUpdateChunkCtrlFile(long chunkId, byte[] buf, long offset, long size);
    public abstract byte[] ReadChainFile(long chainId);
    public abstract byte[] ReadFile(long chunkId);
    public abstract void Restart() throws Exception;
    
    public static String FileType2Extension(byte fileType)
    {
        String ext;
        switch (FileTypes.values()[fileType])
        {
            case CHAIN_FILE_TYPE:
                ext = ".chain";
                break;
            case CHUNK_CTRL_FILE_TYPE:
                ext = ".chunkctrl";
                break;
            case CHUNK_DATA_FILE_TYPE:
                ext = ".chunkdata";
                break;
            default:
                ext = "";
                break;
        }
        return ext;
    }

    public static String LongName2StringName(long fileId,byte fileType)
    {
        return Long.toString(fileId) + FileType2Extension(fileType);
    }
    public static long StringName2LongName(String FileName)
    {
    	Long ret;
        int idx = FileName.indexOf(".");
        if (idx == -1)
        {
            return 0;
        }
        String strNum = FileName.substring(0,idx);
        ret  = new Long(strNum);
        return ret;
    }
    public static byte GetFileType(String FileName)
    {
        int idx = FileName.indexOf(".");
        if (idx == -1)
        {
            return (byte)FileTypes.DUMMY_FILE_TYPE.ordinal();
        }
        String leftPart = FileName.substring(idx);
        if(leftPart == "chunkdata")
            return (byte)FileTypes.CHUNK_DATA_FILE_TYPE.ordinal();
        if(leftPart == "chunkctrl")
            return (byte)FileTypes.CHUNK_CTRL_FILE_TYPE.ordinal();
        if(leftPart == "chain")
            return (byte)FileTypes.CHAIN_FILE_TYPE.ordinal();
        return (byte)FileTypes.DUMMY_FILE_TYPE.ordinal();
    }
}
