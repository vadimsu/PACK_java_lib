package vadimsuraev.PACK.ChunkChainDataTypes;
import vadimsuraev.ReferencedTypes.*;

public class ChunkMetaData
{
    public ReferencedLong chunk;
    public byte hint;
    public ChunkMetaData()
    {
    	chunk = new ReferencedLong();
    }
    public static long GetSize() { return (Long.SIZE/8 + Byte.SIZE/8); }
};
