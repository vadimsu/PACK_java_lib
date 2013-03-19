package vadimsuraev.PACK.ChunkChainDataTypes;

public class FileAndOffset
{
    public long file;
    public long offset;
    public static long GetSize() { return Long.SIZE/8 + Long.SIZE/8; }
};
