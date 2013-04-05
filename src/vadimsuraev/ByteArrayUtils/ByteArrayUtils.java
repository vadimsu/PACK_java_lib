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
    
    static public void CopyBytes(byte []srcArray,int srcOffset,byte []dstArray,int dstOffset,int length)
    {
    	int idx;
    	
    	for(idx = srcOffset;(idx < srcArray.length)&&((idx-srcOffset) < length);idx++)
    	{
    		dstArray[dstOffset+(idx-srcOffset)] = srcArray[idx];
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
