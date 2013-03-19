package vadimsuraev.PACK.FileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Map.Entry;


import vadimsuraev.ByteArrayScalarTypeConversionLib.ByteArrayScalarTypeConversionLib;
import vadimsuraev.PACK.ChunkChainDataTypes.FileAndOffset;
import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;

public class NonRamFileManager extends FileManager
{
    static byte[] GetArraySection(byte[] arr, int offset, int size)
    {
        byte[] buff = new byte[size];
        int idx;
        for (idx = 0; idx < size; idx++)
        {
            buff[idx] = arr[idx + offset];
        }
        return buff;
    }
    public void Restart()
    {
        try
        {
            GlobalsInitiated = false;
            RefCount--;
            allChunkDataFiles.clear();
            chainFilesMutex.lock();
            chainfileMap.clear();
            chainFilesMutex.unlock();
            chunkCtrlFilesMutex.lock();
            chunkCBfileMap.clear();
            chunkCtrlFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    protected static void Init(String workingDirectory)
    {
        File[] allFiles;
        Long temp;
        FileInputStream fs;
        long key;
        long bufSize;
        byte[] buff = null;
        int idx;
        File file;
        ReferencedLong rl = new ReferencedLong();

        RefCount++;

        if (GlobalsInitiated)
        {
            return;
        }

        InitGlobals(workingDirectory);

        if ((workingDirectory == null)||(workingDirectory == ""))
        {
        	LogUtility.LogFile("Getting from user.dir", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            workingDirectory = System.getProperty("user.dir");
        }
        LogUtility.LogFile("WorkingDirectory = " + workingDirectory, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        file = new File(workingDirectory);
        
        allFiles = file.listFiles();
        if(allFiles == null)
        {
        	return;
        }
        
        for (int fileIdx = 0;fileIdx < allFiles.length;fileIdx++)
        {
            String filename = allFiles[fileIdx].getName();
            if (filename.indexOf(".chunkdata") >= 0)
            {
                temp = Long.getLong(filename.substring(0, filename.indexOf(".chunkdata")));
                if(temp == null)
                {
                	continue;
                }
                allChunkDataFiles.add(file.getName());
                if (temp > LastFileId)
                {
                    LastFileId = temp;
                }
            }
            else if (filename.indexOf(".chunkctrl") >= 0)
            {
                try 
                {
					fs = new FileInputStream(WorkingDirectory + "\\" + filename);
					buff = new byte[fs.available()];
	                fs.read(buff,0,buff.length);
	                fs.close();
				}
                catch (Exception e) 
                {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                idx = 0;
                while (idx < buff.length)
                {
                	ByteArrayScalarTypeConversionLib.ByteArray2Long(buff, idx, rl);
                    key = rl.val;
                    idx += 8;
                    ByteArrayScalarTypeConversionLib.ByteArray2Uint(buff, idx, rl);
                    bufSize = rl.val;
                    idx += 4;
                    chunkCBfileMap.put(key,GetArraySection(buff,idx,(int)bufSize));
                    idx += (int)bufSize;
                }
            }
            else if (filename.indexOf(".chain") >= 0)
            {
                try 
                {
					fs = new FileInputStream(WorkingDirectory + "\\" +  filename);
					buff = new byte[fs.available()];
	                fs.read(buff, 0, buff.length);
	                fs.close();
				} 
                catch (Exception e) 
                {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                idx = 0;
                while (idx < buff.length)
                {
                	ByteArrayScalarTypeConversionLib.ByteArray2Long(buff, idx, rl);
                    key = rl.val;
                    idx += 8;
                    ByteArrayScalarTypeConversionLib.ByteArray2Uint(buff, idx, rl);
                    bufSize = rl.val;
                    idx += 4;
                    chainfileMap.put(key, GetArraySection(buff, idx, (int)bufSize));
                    if (key >= LastFileId)
                    {
                        LastFileId = key + 1;
                    }
                    idx += (int)bufSize;
                }
            }
        }
    }
    public NonRamFileManager(String workingDirectory)
    {
        Init(workingDirectory);
    }

    protected void finalize() throws Throwable 
    {
        RefCount--;
        if (RefCount == 0)
        {
            OnExiting();
        }
    }

    public byte[] GetChunk(FileAndOffset fo, long size)
    {
        String file;
        byte[] buf = null;
        file = Long.toString(fo.file);
        file += FileType2Extension((byte)FileTypes.CHUNK_DATA_FILE_TYPE.ordinal());
        file = WorkingDirectory + "\\" + file;
        try
        {
            chunkDataFilesMutex.lock();

            RandomAccessFile fs = new RandomAccessFile(file,"r");
            if (fs.length() <= fo.offset)
            {
                fs.close();
                chunkDataFilesMutex.unlock();
                return null;
            }
            buf = new byte[(int) size];
            fs.seek(fo.offset);
            fs.read(buf, 0, buf.length);
            fs.close();
            chunkDataFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return buf;
    }

    public FileAndOffset AddNewChunk(byte[] data, long offset, long size, long chunkId)
    {
    	FileAndOffset fo = new FileAndOffset();
        String file = "";
        RandomAccessFile fs;

        try
        {
            chunkDataFilesMutex.lock();
            if (allChunkDataFiles.size() > 0)
            {
                int idx;
                for (idx = 0; idx < allChunkDataFiles.size(); idx++)
                {
                	File fileInfo = new File(allChunkDataFiles.get(idx));
                    if ((fileInfo.length() + size) < FileMaxSize)
                    {
                        file = allChunkDataFiles.get(idx);
                        break;
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
                File f = new File(file);
                if(f.exists())
                {
                	f.delete();
                }
                f.createNewFile();
                fo.file = LastFileId;
                fo.offset = 0;
                fs = new RandomAccessFile(file,"rw");
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
                fs = new RandomAccessFile(file,"rw");
                fo.file = StringName2LongName(filename2);
                /*fo.offset = fs.length();*/
                fo.offset = fs.length();
                fs.seek(fs.length());
            }
            fs.write(data, (int)offset, (int)size);
            fs.close();
            chunkDataFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION IN AddNewChunk " + file,exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            fo.file = 0;
            fo.offset = 0;
        }
        return fo;
    }
    public long AddUpdateChainFile(byte[] data, long chainId)
    {
        String file = "";
        long chainId2Return = 0;

        try
        {
            chainFilesMutex.lock();
            
            file += FileType2Extension((byte)FileTypes.CHAIN_FILE_TYPE.ordinal());
            file = WorkingDirectory + "\\" + file;
            byte[] existingBuf = chainfileMap.get(chainId);
            if((chainId != 0)&&(existingBuf != null))
            {
                chainfileMap.remove(chainId);
                chainId2Return = chainId;
            }
            else if (chainId == 0)
            {
                chainId2Return = ++LastFileId;
            }
            chainfileMap.put(chainId2Return, data);
            chainFilesMutex.unlock();
            return chainId2Return;
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION: " ,exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return 0;
        }
    }

    public void AddUpdateChunkCtrlFile(long chunkId, byte[] buf, long offset, long size)
    {
        String file ="";
        //file = Convert.ToString(chunkId);
        file += FileType2Extension((byte)FileTypes.CHUNK_CTRL_FILE_TYPE.ordinal());
        file = WorkingDirectory + "\\" + file;
        try
        {
            chunkCtrlFilesMutex.lock();
            byte[] existingBuf = chunkCBfileMap.get(chunkId);
            if (existingBuf != null)
            {
                chunkCBfileMap.remove(chunkId);
            }
            chunkCBfileMap.put(chunkId, buf);
            chunkCtrlFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION " , exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }

    public  byte[] ReadChainFile(long chainId)
    {
    	byte[] buff = null;
        String file = WorkingDirectory + "\\" + LongName2StringName(chainId, (byte)FileTypes.CHAIN_FILE_TYPE.ordinal());
        try
        {
            chainFilesMutex.lock();
            buff = chainfileMap.get(chainId);
            if (buff == null)
            {
                chainFilesMutex.unlock();
                return null;
            }
            chainFilesMutex.unlock();
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION " , exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return buff;
    }
    public byte[] ReadFile(long chunkId)
    {
        try
        {
            chunkCtrlFilesMutex.lock();
            byte[] existingBuf = chunkCBfileMap.get(chunkId);
            if(existingBuf != null)
            {
                chunkCtrlFilesMutex.unlock();
                return existingBuf;
            }
            chunkCtrlFilesMutex.unlock();
            return null;
        }
        catch (Exception exc)
        {
            LogUtility.LogException("EXCEPTION: " , exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return null;
    }

    public static void OnExiting()
    {
    	ReferencedLong rl = new ReferencedLong();
    	
        try
        {
            LogUtility.LogFile("Entering OnExiting ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            String file = "";
            file += FileType2Extension((byte)FileTypes.CHAIN_FILE_TYPE.ordinal());
            file = WorkingDirectory + "\\" + file;
            chainFilesMutex.lock();
            File f = new File(file);
            if(f.exists())
            {
            	f.delete();
            }
            RandomAccessFile fs = new RandomAccessFile(file, "rw");
            LogUtility.LogFile("writing chains ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            Iterator itr = chainfileMap.entrySet().iterator();
            while(itr.hasNext())
            {
            	byte[] buff = new byte[8];
            	Entry<Long,byte[]> e = (Entry<Long, byte[]>) itr.next();
            	ByteArrayScalarTypeConversionLib.Long2ByteArray(buff, 0, e.getKey());
                fs.write(buff, 0, buff.length);
                buff = new byte[4];
                ByteArrayScalarTypeConversionLib.Uint2ByteArray(buff, 0, e.getValue().length);
                fs.write(buff, 0, buff.length);
                LogUtility.LogFile("key " + Long.toString(e.getKey()) + " len " + Long.toString(e.getValue().length), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                buff = e.getValue();
                fs.write(buff, 0, buff.length);
            }
            fs.close();
            chainFilesMutex.unlock();
            file = "";
            file += FileType2Extension((byte)FileTypes.CHUNK_CTRL_FILE_TYPE.ordinal());
            file = WorkingDirectory + "\\" + file;
            chunkCtrlFilesMutex.lock();
            fs = new RandomAccessFile(file, "rw");
            LogUtility.LogFile("writing chunks ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            itr = chunkCBfileMap.entrySet().iterator();
            while(itr.hasNext())
            {
            	byte[] buff = new byte[8];
            	Entry<Long,byte[]> e = (Entry<Long, byte[]>) itr.next();
            	ByteArrayScalarTypeConversionLib.Long2ByteArray(buff, 0, e.getKey());
                fs.write(buff, 0, buff.length);
                buff = new byte[4];
                ByteArrayScalarTypeConversionLib.Uint2ByteArray(buff, 0, e.getValue().length);
                fs.write(buff, 0, buff.length);
                LogUtility.LogFile("key " + Long.toString(e.getKey()) + " len " + Long.toString(e.getValue().length), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                buff = e.getValue();
                fs.write(buff, 0, buff.length);
            }
            fs.close();
            chunkCtrlFilesMutex.unlock();
            LogUtility.LogFile("Leaving OnExiting ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION: " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
}
