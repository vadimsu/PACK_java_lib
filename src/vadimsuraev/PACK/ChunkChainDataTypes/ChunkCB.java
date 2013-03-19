package vadimsuraev.PACK.ChunkChainDataTypes;

import vadimsuraev.PACK.ChunkChainDataTypes.FileAndOffset;

public class ChunkCB
{
    public long size;
    public long sha1;
    public byte hint;
    public FileAndOffset fo;
    public long ChainsListSize;
    public long[] chains;
    public ChunkCB()
    {
    	fo = new FileAndOffset();
    }
    public static long GetSize() { return Long.SIZE/8 + Long.SIZE/8 + Byte.SIZE/8 + + FileAndOffset.GetSize() + Long.SIZE/8; }
    public static long GetSize(long ChainEntriesNumber) { return GetSize() + (Long.SIZE/8) * ChainEntriesNumber; }
};