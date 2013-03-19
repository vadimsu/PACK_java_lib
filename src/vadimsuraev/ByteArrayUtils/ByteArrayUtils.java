package vadimsuraev.ByteArrayUtils;

public class ByteArrayUtils {
    static public void CopyBytes(byte []srcArray,byte []dstArray,int dstOffset)
    {
    	int idx;
    	
    	for(idx = 0;idx < srcArray.length;idx++)
    	{
    		dstArray[dstOffset+idx] = srcArray[idx];
    	}
    }

	public static void CopyBytes(long []srcArray,long []dstArray,int dstOffset) 
	{
       int idx;
    	
    	for(idx = 0;idx < srcArray.length;idx++)
    	{
    		dstArray[dstOffset+idx] = srcArray[idx];
    	}
		
	}
}
