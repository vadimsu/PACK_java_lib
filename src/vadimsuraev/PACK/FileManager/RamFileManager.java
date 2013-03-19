package vadimsuraev.PACK.FileManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import vadimsuraev.PACK.ChunkChainDataTypes.FileAndOffset;
import vadimsuraev.LogUtility.*;

public class RamFileManager extends FileManager
{
    static private HashMap<String, ByteBuffer> RamFileSystem;

    public RamFileManager(String workingDirectory)
    {
        Init(workingDirectory);
    }
    public  void Restart() throws Exception
    {
        throw new Exception("Not implemented");
    }
    protected static void Init(String workingDirectory)
    {
        if (!GlobalsInitiated)
        {
            InitGlobals(workingDirectory);
            RamFileSystem = new HashMap<String, ByteBuffer>();
        }
    }

    public  byte[] GetChunk(FileAndOffset fo, long size)
    {
        String file;
        byte[] buf = null;
        file = Long.toString(fo.file);
        file += FileType2Extension((byte)FileTypes.CHUNK_DATA_FILE_TYPE.ordinal());
        file = WorkingDirectory + "\\" + file;
        try
        {
            chunkDataFilesMutex.lock();
            ByteBuffer stream = RamFileSystem.get(file);
            if (stream != null)
            {
                LogUtility.LogFile("GetChunk: found " + file,LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                long pos = stream.position();
                stream.position((int)fo.offset);
                buf = new byte[(int) size];
                stream.get(buf, 0, (int)size);
                stream.position((int)pos);
                chunkDataFilesMutex.unlock();
                return buf;
            }
            chunkDataFilesMutex.unlock();
            return null;
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return buf;
    }

    public FileAndOffset AddNewChunk(byte[] data, long offset, long size, long chunkId)
    {
    	FileAndOffset fo = null;
        String file = "";
        ByteBuffer stream = null;

        try
        {
            chunkDataFilesMutex.lock();
            if (allChunkDataFiles.size() > 0)
            {
                int idx;
                Iterator itr = allChunkDataFiles.iterator();
                while(itr.hasNext())
                {
                	String tempFile = (String) itr.next();
                	stream = RamFileSystem.get(tempFile);
                    if (stream != null)
                    {
                        LogUtility.LogFile("AddNewChunk: found " + tempFile,LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                        if ((stream.limit() + size) < FileMaxSize)
                        {
                            file = tempFile;
                            break;
                        }
                    }
                }
            }
            if (file == "")
            {
                file = Long.toString(++LastFileId);
                file += FileType2Extension((byte)FileTypes.CHUNK_DATA_FILE_TYPE.ordinal());
                file = WorkingDirectory + "\\" + file;
                LogUtility.LogFile("new file " + file, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                allChunkDataFiles.add(file);
                stream = ByteBuffer.allocate((int) size);
                RamFileSystem.put(file, stream);
                fo = new FileAndOffset();
                fo.file = LastFileId;
                fo.offset = 0;
            }
            else
            {
                int slash_idx;
                String filename2 = "";

                //slash_idx = file.IndexOf("\\");
                slash_idx = file.lastIndexOf("\\");
                if (slash_idx == -1)
                {
                    filename2 = file;
                }
                else
                {
                    filename2 = file.substring(slash_idx + 1);
                }
                LogUtility.LogFile("AddNewChunk2: found " + file + " " + filename2, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                fo = new FileAndOffset();
                fo.file = StringName2LongName(filename2);
                fo.offset = stream.position();
            }
            stream.put(data,(int)offset,(int)size);
            chunkDataFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION IN AddNewChunk " + file + " " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return fo;
    }
    public  long AddUpdateChainFile(byte[] data, long chainId)
    {
        String file;
        long chainId2Return;

        try
        {
            chainFilesMutex.lock();
            if (chainId != 0)
            {
                file = Long.toString(chainId);
                chainId2Return = chainId;
            }
            else
            {
                file = Long.toString(++LastFileId);
                chainId2Return = LastFileId;
            }
            file += FileType2Extension((byte)FileTypes.CHAIN_FILE_TYPE.ordinal());
            file = WorkingDirectory + "\\" + file;
            ByteBuffer stream = RamFileSystem.get(file);
            if (stream != null)
            {
                stream.position(0);
                stream.put(data, 0, data.length);
            }
            else
            {
                allChainFiles.add(file);
                stream = ByteBuffer.allocate(data.length);
                stream.put(data, 0, data.length);
                RamFileSystem.put(file, stream);
            }
            chainFilesMutex.unlock();
            return chainId2Return;
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION: " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return 0;
        }
    }

    public  void AddUpdateChunkCtrlFile(long chunkId, byte[] buf, long offset, long size)
    {
        String file;
        file = Long.toString(chunkId);
        file += FileType2Extension((byte)FileTypes.CHUNK_CTRL_FILE_TYPE.ordinal());
        file = WorkingDirectory + "\\" + file;
        try
        {
            chunkCtrlFilesMutex.lock();
            ByteBuffer stream = RamFileSystem.get(file);
            if (stream == null)
            {
                LogUtility.LogFile("AddNewChunkCtrlFile: NOT found " + file,LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                allChunkCtrlFiles.add(file);
                stream = ByteBuffer.allocate((int) size);
                stream.put(buf, (int)offset, (int)size);
                RamFileSystem.put(file, stream);
            }
            else
            {
                LogUtility.LogFile("AddNewChunkCtrlFile: found " + file,LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                stream.position(0);
                stream.put(buf, (int)offset, (int)size);
            }
            chunkCtrlFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }

    public  byte[] ReadChainFile(long chainId)
    {
    	byte[] buff = null;
        String file = WorkingDirectory + "\\" + LongName2StringName(chainId, (byte)FileTypes.CHAIN_FILE_TYPE.ordinal());
        try
        {
            chainFilesMutex.lock();
            ByteBuffer stream = RamFileSystem.get(file);
            if (stream == null)
            {
                chainFilesMutex.unlock();
                return buff;
            }
            long pos = stream.position();
            stream.position(0);
            buff = new byte[stream.limit()];
            stream.get(buff, 0, (int)stream.limit());
            stream.position((int) pos);
            chainFilesMutex.unlock();
            return buff;
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return null;
    }
    public byte[] ReadFile(long chunkId)
    {
        String file = "";
        try
        {
            chunkCtrlFilesMutex.lock();
            ByteBuffer stream = RamFileSystem.get(file);
            if (stream == null)
            {
                chunkCtrlFilesMutex.unlock();
                return null;
            }
            long pos = stream.position();
            stream.position(0);
            byte[] buff = new byte[stream.limit()];
            stream.get(buff, 0, buff.length);
            stream.position((int) pos);
            chunkCtrlFilesMutex.unlock();
            return buff;
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return null;
    }
}
