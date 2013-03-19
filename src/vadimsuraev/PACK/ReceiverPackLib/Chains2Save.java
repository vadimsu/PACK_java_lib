package vadimsuraev.PACK.ReceiverPackLib;

import java.util.List;

import vadimsuraev.PACK.ChunkAndChainManager.ChunkAndChainFileManager;

public class Chains2Save
{
    /**
	 * 
	 */
	List<Long> m_ChunList;
    int m_FirstNonMatchingChunk;
    int m_LastNonMatchingChunk;
    byte[] m_Packet;
    int m_FirstNonMatchingChunkOffset;
    ChunkAndChainFileManager m_ChunkAndChainFileManager;
    public Chains2Save(List<Long> chunkList, int firstNonMatchingChunk, int lastNonMatchingChunk, byte[] packet, int firstNonMatchingChunkOffset, ChunkAndChainFileManager chunkAndChainFileManager)
    {
        m_ChunList = chunkList;
        m_FirstNonMatchingChunk = firstNonMatchingChunk;
        m_LastNonMatchingChunk = lastNonMatchingChunk;
        m_Packet = packet;
        m_FirstNonMatchingChunkOffset = firstNonMatchingChunkOffset;
        m_ChunkAndChainFileManager = chunkAndChainFileManager;
    }
    public List<Long> GetChunkList()
    {
        return m_ChunList;
    }
    public int GetFirstNonMatchingChunk()
    {
        return m_FirstNonMatchingChunk;
    }
    public int GetLastNonMatchingChunk()
    {
        return m_LastNonMatchingChunk;
    }
    public byte[] GetPacket()
    {
        return m_Packet;
    }
    public int GetFirstNonMatchingChunkOffset()
    {
        return m_FirstNonMatchingChunkOffset;
    }
    public ChunkAndChainFileManager GetChunkAndChainFileManager()
    {
        return m_ChunkAndChainFileManager;
    }
}