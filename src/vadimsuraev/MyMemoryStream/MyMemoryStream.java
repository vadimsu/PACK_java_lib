package vadimsuraev.MyMemoryStream;
import java.nio.ByteBuffer;

import vadimsuraev.LogUtility.LogUtility;

public class MyMemoryStream
{
    long current_offset;
    ByteBuffer memoryStream;
    
    public MyMemoryStream()
    {
    	memoryStream = ByteBuffer.allocate(1024);
        current_offset = 0;
    }

    public byte[] GetBytes()
    {
        byte[] buff = new byte[(int) (memoryStream.position() - current_offset)];
        long position = memoryStream.position();
        memoryStream.position((int) current_offset);
        ByteBuffer read = memoryStream.get(buff, 0, buff.length);
        memoryStream.position((int) position);
        if (read == null)
        {
            return null;
        }
        return buff;
    }
    public byte[] GetBytesLimited(int Limit)
    {
        int bufSize;
        if((memoryStream.position() - current_offset) > Limit)
        {
            bufSize = Limit;
        }
        else
        {
            bufSize = (int)(memoryStream.position() - current_offset);
        }
        byte[] buff = new byte[bufSize];
        long position = memoryStream.position();
        memoryStream.position((int) current_offset);
        ByteBuffer read = memoryStream.get(buff, 0, buff.length);
        memoryStream.position((int) position);
        if (read == null)
        {
            return null;
        }
        return buff;
    }

    public void AddBytes(byte[] buff)
    {
    	if((memoryStream == null)||(buff == null))
    	{
    		if(memoryStream == null)
    		{
    			LogUtility.LogFile("Stream  is null", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    		}
    		else
    		{
    		    LogUtility.LogFile("buff is null  ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    		}
    	}
    	if(memoryStream.limit() < (memoryStream.position() + buff.length))
    	{
    		ByteBuffer temp = ByteBuffer.allocate((memoryStream.position() + buff.length)*2);
    	    temp.put(memoryStream.array(), 0, memoryStream.position());
    	    memoryStream = temp;
    	}
        memoryStream.put(buff, 0, buff.length);
    }

    public void AddBytes(byte[] buff, int count)
    {
        memoryStream.put(buff, 0, count);
    }

    public long Length()
    {
        return ((long)memoryStream.position() - current_offset);
    }

    public long WholeLength()
    {
        return memoryStream.position();
    }

    public void GetBytes(byte []buff,int idx,int length)
    {
        long position = memoryStream.position();
        memoryStream.position((int) current_offset);
        ByteBuffer read = memoryStream.get(buff, idx, length);
        memoryStream.position((int) position);
    }

    public byte[] GetBytes(int position)
    {
        long current_position = memoryStream.position();
        if (current_position < position)
        {
            return null;
        }
        try
        {
            memoryStream.position(memoryStream.position() -position);
            byte[] buff = new byte[memoryStream.limit() - memoryStream.position()];
            memoryStream.get(buff, 0, (int)memoryStream.limit() - (int)memoryStream.position());
            memoryStream.position((int) current_position);
            return buff;
        }
        catch (Exception exc)
        {
            //memoryStream.Position = current_position;
            LogUtility.LogException("position " + Integer.toString(position) + " Length " + Integer.toString(memoryStream.limit()) + " pos " + Integer.toString(memoryStream.position()),exc,LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return null;
        }
    }

    public void IncrementOffset(long increment)
    {
        current_offset += increment;
        if (memoryStream.limit() == current_offset)
        {
            memoryStream.rewind();
            current_offset = 0;
        }
    }
    public void Reset()
    {
        current_offset = 0;
    }
    public void Clear()
    {
        current_offset = 0;
        memoryStream = null;
    }
}
