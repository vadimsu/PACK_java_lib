package vadimsuraev.ByteArrayScalarTypeConversionLib;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.ReferencedTypes.ReferencedLong;

public class ByteArrayScalarTypeConversionLib
{
    public static long ByteArray2Long(byte[] array, long offset, ReferencedLong l)
    {
    	try
    	{
    	    ByteBuffer bb = ByteBuffer.wrap(array, (int)offset,8);
    	    bb.order(ByteOrder.nativeOrder());
    	    l.val = bb.getLong();
    	}
    	catch(Exception e)
    	{
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            e.printStackTrace(ps);
            LogUtility.LogFile("EXCEPTION: " + os.toString(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	/*ByteBuffer bb = ByteBuffer.allocate(8);
    	bb.order(ByteOrder.nativeOrder());
    	byte []tempArray = new byte[8];
    	for(int i = 0;i < 8;i++)
    	{
    		tempArray[i] = array[(int) (offset+i)];
    	}
		bb.put(tempArray);
		bb.position(0);
		l.val = bb.getLong();
		bb = null;
		tempArray = null;*/
        return 8;
    }
    public static long ByteArray2Uint(byte[] array, long offset, ReferencedLong ui)
    {
    	try
    	{
    	    ByteBuffer bb = ByteBuffer.wrap(array, (int)offset, 4);
    	    bb.order(ByteOrder.nativeOrder());
    	    ui.val = bb.getInt();
    	}
    	catch(Exception e)
    	{
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            e.printStackTrace(ps);
            LogUtility.LogFile("EXCEPTION: " + os.toString(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	/*ByteBuffer bb = ByteBuffer.allocate(4);
    	bb.order(ByteOrder.nativeOrder());
    	byte []tempArray = new byte[4];
    	for(int i = 0;i < 4;i++)
    	{
    		tempArray[i] = array[(int) (offset+i)];
    	}
		bb.put(tempArray);
		bb.position(0);
		ui.val = (int) bb.getInt();
		bb = null;
		tempArray = null;*/
        return 4;
    }
    public static long Long2ByteArray(byte[] array, long offset, long l)
    {
    	try
    	{
    	    ByteBuffer bb = ByteBuffer.wrap(array, (int)offset, 8);
    	    bb.order(ByteOrder.nativeOrder());
    	    bb.putLong(l);
    	}
    	catch(Exception e)
    	{
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            e.printStackTrace(ps);
            LogUtility.LogFile("EXCEPTION: " + os.toString(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	/*ByteBuffer bb = ByteBuffer.allocate(8);
    	bb.order(ByteOrder.nativeOrder());
		bb.putLong(l);
	    byte []temp = bb.array();
	    for(int i = 0;i < 8;i++)
	    {
	    	array[(int) (i+offset)] = temp[i];
	    }
	    bb = null;*/
        return 8;
    }
    public static long Uint2ByteArray(byte[] array, long offset, long ui)
    {
    	try
    	{
    	    ByteBuffer bb = ByteBuffer.wrap(array, (int)offset, 4);
    	    bb.order(ByteOrder.nativeOrder());
    	    bb.putInt((int)ui);
    	}
    	catch(Exception e)
    	{
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            e.printStackTrace(ps);
            LogUtility.LogFile("EXCEPTION: " + os.toString(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	/*ByteBuffer bb = ByteBuffer.allocate(4);
    	bb.order(ByteOrder.nativeOrder());
		bb.putInt((int) ui);
	    byte []temp = bb.array();
	    for(int i = 0;i < 4;i++)
	    {
	    	array[(int) (i+offset)] = temp[i];
	    }
	    bb = null;*/
        return 4;
    }
}
