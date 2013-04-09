package vadimsuraev.MyMemoryStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import vadimsuraev.LogUtility.LogUtility;

public class MyMemoryStream extends ByteArrayOutputStream 
{
	int m_ReadPosition;
	int m_WritePosition;
        
    public MyMemoryStream()
    {
    	super();
    	m_ReadPosition = 0;
    	m_WritePosition = 0;
    }

    public byte[] GetBytes()
    {
    	if(m_WritePosition == m_ReadPosition)
    	{
    		return null;
    	}
    	byte []arr = new byte[m_WritePosition - m_ReadPosition];
    	for(int idx = m_ReadPosition;idx < m_WritePosition;idx++)
    	{
    		arr[idx-m_ReadPosition] = super.buf[idx];
    	}
        return arr;
    }

    public void AddBytes(byte[] buff)
    {
    	m_WritePosition += buff.length;
        super.write(buff, 0, buff.length);
    }

    public long Length()
    {
        return (m_WritePosition - m_ReadPosition);
    }

    public void IncrementOffset(long increment) throws Exception
    {
    	if((m_ReadPosition + increment) > m_WritePosition)
    	{
    		Exception exc = new Exception();
    		throw exc;
    	}
    	m_ReadPosition += increment;
    }
    public void Reset()
    {
    	m_ReadPosition = 0;
    	m_WritePosition = 0;
    	super.reset();
    }
    public void Clear()
    {
    	Reset();
    }
}
