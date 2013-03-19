package vadimsuraev.PACK.ReceiverPackLib;
import vadimsuraev.LogUtility.*;
import vadimsuraev.PACK.PackChunking.PackChunking;
import vadimsuraev.PACK.PackMsg.PackMsg;
import vadimsuraev.PACK.PackMsg.PackMsg.MsgKind_e;
import vadimsuraev.ReferencedTypes.ReferencedByte;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.PACK.ProxyLib.Stream2Message.Stream2Message;
import vadimsuraev.ByteArrayScalarTypeConversionLib.ByteArrayScalarTypeConversionLib;
import vadimsuraev.PACK.ChunkAndChainManager.ChunkAndChainFileManager;
import vadimsuraev.PACK.ChunkChainDataTypes.*;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


public class ReceiverPackLib extends Stream2Message
{
    public static long m_ChunksProcessed = 0;
    public static long m_PredMsgSent = 0;
    public static long m_PredAckMsgReceived = 0;
    static PackChunking m_packChunking = new PackChunking(8);

    //private ChunkAndChainFileManager.ChunkAndChainFileManager chunkAndChainFileManager;
    //private packChunking;
    private long m_CurrentOffset;
    long m_TotalSaved;
    long m_TotalReceived;
    long m_TotalPredAckSize;
    long m_TotalPredSize;
    SocketAddress m_Id;
    List<Long> m_SentChainList;
    ReentrantLock m_libMutex;
    ClientSideCallbacks m_ClientSideCallbacks;
    ChunkAndChainFileManager m_chunkAndChainFileManager;
    
    static WriteChunkChainsThread storeChunksAndChainsThread = null;

    public static void InitGlobalObjects()
    {
        LogUtility.LogFile("ReceivePackLib:InitGlobalObjects", LogUtility.LogLevels.LEVEL_LOG_HIGH);
        ChunkAndChainFileManager.Init();
        storeChunksAndChainsThread = new WriteChunkChainsThread();
        storeChunksAndChainsThread.start();
    }

    public static void Flush()
    {
        ChunkAndChainFileManager.Flush();
    }

    void InitInstance()
    {
        Reset();
        m_TotalSaved = 0;
        m_TotalReceived = 0;
        m_TotalPredAckSize = 0;
        m_TotalPredSize = 0;
        m_CurrentOffset = 0;
        m_chunkAndChainFileManager = new ChunkAndChainFileManager();
        m_libMutex = new ReentrantLock();
        m_SentChainList = new LinkedList<Long>();
    }
    public long GetTotalData()
    {
        return m_CurrentOffset;
    }
    public long GetTotalDataSaved()
    {
        return m_TotalSaved;
    }

    public void Reset()
    {
        m_CurrentOffset = 0;
    }
    
    public ReceiverPackLib(SocketAddress id,ClientSideCallbacks clientSideCallbacks)
    {
    	super();
        m_Id = id;
        m_ClientSideCallbacks = clientSideCallbacks;
        InitInstance();
    }

    void ReceiverOnPredictionConfirm(List<ChunkMetaData> chunkMetaDataAndId,long chunksCount)
    {
        m_libMutex.lock();
        long idx;
        long savedInThisCall = 0;

        idx = 0;
        Iterator<ChunkMetaData> itr = chunkMetaDataAndId.iterator();
        while(itr.hasNext())
        {
        	ChunkMetaData chMetaData = (ChunkMetaData) itr.next();
            int ChunkLength;
            LogUtility.LogFile(m_Id.toString() + " writing acked " + Long.toString(chMetaData.chunk.val), ModuleLogLevel);
            ChunkLength = PackChunking.chunkToLen(chMetaData.chunk.val);
            byte[] buff = ChunkAndChainFileManager.GetChunkData(chMetaData.chunk.val);
            m_ClientSideCallbacks.OnDataReceived(buff, 0, buff.length);
            m_CurrentOffset += ChunkLength;
            m_TotalSaved += (long)ChunkLength;
            savedInThisCall += (long)ChunkLength;
            idx++;
            if (idx == chunksCount)
            {
                break;
            }
        }
        m_libMutex.unlock();
        LogUtility.LogFile(m_Id.toString() + " Saved in this call " + Long.toString(savedInThisCall), LogUtility.LogLevels.LEVEL_LOG_HIGH3);
    }

    long GetChainsListSize(List<ChunkListAndChainId> chainChunkList)
    {
        long size = Long.SIZE/8+Long.SIZE/8;
        Iterator<ChunkListAndChainId> itr = chainChunkList.iterator();
        while(itr.hasNext())
        {
        	ChunkListAndChainId chunkListAndChainId = (ChunkListAndChainId) itr.next();
            long chunkCount = (long)(chunkListAndChainId.chunks.length - chunkListAndChainId.firstChunkIdx);
            //if (chunkListAndChainId.chunks.Length > 50)
            //{
              //  if ((chunkListAndChainId.chunks.Length - chunkListAndChainId.firstChunkIdx) > 50)
                //{
                  //  chunkCount = 50;
                //}
            //}
            LogUtility.LogFile("chunkListAndChainId.chunks.Length=" + Integer.toString(chunkListAndChainId.chunks.length) + " chunkListAndChainId.firstChunkIdx=" + Long.toString(chunkListAndChainId.firstChunkIdx), LogUtility.LogLevels.LEVEL_LOG_HIGH3);
            size += (long)(Long.SIZE/8 + Long.SIZE/8 + (chunkCount*(Byte.SIZE/8+Long.SIZE/8)));
        }

        return size;
    }

    void EncodePredictionMessage(byte[] buffer, int offset, List<ChunkListAndChainId> chainChunkList,long chainOffset)
    {
        long buffer_idx = (long)offset;

        LogUtility.LogFile(m_Id.toString() + " Composing Pred msg: chainOffset " + Long.toString(chainOffset) + "chains " + Long.toString(chainChunkList.size()), ModuleLogLevel);

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer, buffer_idx, chainOffset);

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer, buffer_idx, (long)chainChunkList.size());
        Iterator<ChunkListAndChainId> itr = chainChunkList.iterator();
        while(itr.hasNext())
        {
        	ChunkListAndChainId chunkListAndChainId = (ChunkListAndChainId) itr.next();
            buffer_idx +=
                ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer, buffer_idx, (long)(chunkListAndChainId.chunks.length - chunkListAndChainId.firstChunkIdx));
            long firstChunk = chunkListAndChainId.firstChunkIdx;
     //       if(chunkListAndChainId.chunks.Length > 50)
       //     {
         //       if(firstChunk < (chunkListAndChainId.chunks.Length - 50))
           //     {
             //       firstChunk = (long)(chunkListAndChainId.chunks.Length - 50);
               // }
            //}
            LogUtility.LogFile(m_Id.toString() + " chunks count " + Long.toString(chunkListAndChainId.chunks.length), ModuleLogLevel);
            for (long idx = /*chunkListAndChainId.firstChunkIdx*/firstChunk;idx < chunkListAndChainId.chunks.length; idx++)
            {
                buffer[(int) buffer_idx++] = ChunkAndChainFileManager.GetChunkHint(chunkListAndChainId.chunks[(int) idx]);
                buffer_idx +=
                ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer, buffer_idx, chunkListAndChainId.chunks[(int) idx]);
                LogUtility.LogFile(m_Id.toString() + " chunk " + Long.toString(chunkListAndChainId.chunks[(int)idx]) + " len " + Integer.toString(PackChunking.chunkToLen(chunkListAndChainId.chunks[(int)idx])) + " is written to PRED", ModuleLogLevel);
            }
        }
    }

    List<ChunkMetaData> DecodePredictionAckMessage(byte[] buffer, int offset, ReferencedLong chunksCount)
    {
        long buffer_idx = (long)offset;
        List<ChunkMetaData> chunkMetaDataAndId;

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Uint(buffer, buffer_idx, chunksCount);

        chunkMetaDataAndId = new LinkedList<ChunkMetaData>();

        for (int idx = 0; idx < chunksCount.val;idx++ )
        {
            ChunkMetaData chunkMetaData = new ChunkMetaData();

            chunkMetaData.hint = buffer[(int) buffer_idx++];
            buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, buffer_idx, chunkMetaData.chunk);
            chunkMetaDataAndId.add(chunkMetaData);
        }
        return chunkMetaDataAndId;
    }

    long ReceiverOnDataMsg(byte[] packet, int packet_offset, byte flag, List<ChunkListAndChainId> chainChunkList, ReferencedLong chainOffset)
    {
    	m_ClientSideCallbacks.OnDataReceived(packet, packet_offset, packet.length - packet_offset);

        List<Long> chunkList = new LinkedList<Long>();
        m_libMutex.lock();
        /* process the stream (+reminder) to get chunks */
        int processed_bytes = m_packChunking.getChunks(chunkList, packet, packet_offset, packet.length, /*is_last*/true, true);
        long offset = (long)packet_offset;
        int idx = 0;
        int lastNonMatchingChunk = chunkList.size();
        int firstNonMatchingChunk = chunkList.size();
        int firstNonMatchingChunkOffset = 0;
        //chainChunkList = new LinkedList<ChunkListAndChainId>();
        int rc;
        boolean foundMatch = false;

        LogUtility.LogFile(m_Id.toString() + " processing " + Integer.toString(chunkList.size()) + " chunks, CurrentOffset " + Long.toString(m_CurrentOffset) + " processed bytes " + Long.toString(processed_bytes) + " packet offset " + Long.toString(packet_offset) + " packet length " + Long.toString(packet.length), ModuleLogLevel);

        if (chunkList.size() > 0)
        {
            rc = m_chunkAndChainFileManager.ChainMatch(chunkList, chunkList.size()-1, chainChunkList, m_SentChainList);
            if (rc > 0)
            {
                m_SentChainList.add(chainChunkList.get(0).chainId);
                foundMatch = true;
                LogUtility.LogFile(m_Id.toString() + " found long chain (fast path) ", ModuleLogLevel);
            }
            else if (rc == 0)
            {
                foundMatch = true;
                LogUtility.LogFile(m_Id.toString() + " found equal chain (fast path) ", ModuleLogLevel);
            }
        }
        if (foundMatch)
        {
            chainOffset.val = (long)(m_CurrentOffset + processed_bytes);
            m_CurrentOffset += (long)(packet.length - packet_offset);
            m_libMutex.unlock();
            return GetChainsListSize(chainChunkList);
        }
        Iterator<Long> itr = chunkList.iterator();
        while(itr.hasNext())
        {
        	Long chunk = (Long) itr.next();
            rc = m_chunkAndChainFileManager.ChainMatch(chunkList, idx, chainChunkList, m_SentChainList);
           
            if (rc < 0)
            {
                lastNonMatchingChunk = idx;
                if (firstNonMatchingChunk == chunkList.size())
                {
//                    LogUtility.LogFile("starting non-mactching range " + Integer.toString(idx), ModuleLogLevel);
                    firstNonMatchingChunk = idx;
                    firstNonMatchingChunkOffset = (int)offset;
                }
                else
                {
//                    LogUtility.LogFile("updating non-matching range " + Integer.toString(idx), ModuleLogLevel);
                }
            }
            else if(lastNonMatchingChunk != chunkList.size())
            {
                LogUtility.LogFile("end of non-matching range " + Integer.toString(lastNonMatchingChunk) + " " + Integer.toString(firstNonMatchingChunk), ModuleLogLevel);
                Chains2Save chain2Save = new Chains2Save(chunkList, firstNonMatchingChunk, lastNonMatchingChunk, packet, firstNonMatchingChunkOffset, m_chunkAndChainFileManager);
                WriteChunkChainsThread.AddChain2Save(chain2Save, m_Id);
                firstNonMatchingChunk = chunkList.size();
                lastNonMatchingChunk = chunkList.size();
            }
            m_ChunksProcessed++;
            offset += (long)PackChunking.chunkToLen(chunkList.get(idx));
            idx++;
            LogUtility.LogFile(m_Id.toString() + " AddUpdateChunk: " + Long.toHexString(chunk) + " offset=" + Long.toString(m_CurrentOffset+offset) + " chainChunkList " + Integer.toString(chainChunkList.size()), ModuleLogLevel);
            if (rc > 0)
            {
                m_SentChainList.add(chainChunkList.get(0).chainId);
                break;
            }
            else if (rc == 0)
            {
                break;
            }
        }
        if (lastNonMatchingChunk != chunkList.size())
        {
            LogUtility.LogFile("end of non-matching range (last) " + Integer.toString(lastNonMatchingChunk) + " " + Integer.toString(firstNonMatchingChunk), ModuleLogLevel);
            Chains2Save chain2Save = new Chains2Save(chunkList, firstNonMatchingChunk, lastNonMatchingChunk, packet, firstNonMatchingChunkOffset,m_chunkAndChainFileManager);
            LogUtility.LogFile("end of non-matching range (last)# ", ModuleLogLevel);
            WriteChunkChainsThread.AddChain2Save(chain2Save, m_Id);
            LogUtility.LogFile("end of non-matching range (last)## ", ModuleLogLevel);
        }
        //Vadim 10/01/13 onDataReceived(packet, packet_offset, packet.Length - packet_offset);
        chainOffset.val = (long)(m_CurrentOffset + processed_bytes);
        m_CurrentOffset += (long)(packet.length - packet_offset);
        if (chainChunkList.size() == 0)
        {
            m_libMutex.unlock();
            LogUtility.LogFile("Leaving ReceiverOnDataMsg (not found) ", ModuleLogLevel);
            return 0;
        }
        m_libMutex.unlock();
        LogUtility.LogFile("Leaving ReceiverOnDataMsg (found) ", ModuleLogLevel);
        return GetChainsListSize(chainChunkList);
    }

    public void OnMsgRead4Tx(byte[] msg,boolean dummy)
	{
    	m_ClientSideCallbacks.OnMsgRead4Tx(msg, dummy);
	}
    public byte []ProcessDataMsg(byte []packet,int offset,byte Flags,int room_space)
    {
        long predMsgSize;
        byte[] predMsg;
        List<ChunkListAndChainId> chainChunkList;
        ReferencedLong chainOffset = new ReferencedLong();
        ReferencedInteger refOffset = new ReferencedInteger();
        long received = (long)(packet.length - offset);
        m_TotalReceived += received;
        chainOffset.val = 0;
        LogUtility.LogFile(m_Id.toString() + " ProcessDataMsg: Received: " + Long.toString(received) + " Total received " + Long.toString(m_TotalReceived) + " Total saved "+ Long.toString(m_TotalSaved), LogUtility.LogLevels.LEVEL_LOG_HIGH3);
        chainChunkList = new LinkedList<ChunkListAndChainId>();
        predMsgSize = ReceiverOnDataMsg(packet, offset, Flags,chainChunkList,chainOffset);
        if (predMsgSize == 0)
        {
        	LogUtility.LogFile("Leaving ProcessDataMsg (not found) ", ModuleLogLevel);
            return null;
        }
        LogUtility.LogFile("PredMsgSize=" + Long.toString(predMsgSize) + " Total PredMsgSize " + Long.toString(m_TotalPredSize), LogUtility.LogLevels.LEVEL_LOG_HIGH3);
        refOffset.val = offset;
        predMsg = PackMsg.AllocateMsgAndBuildHeader((long)(predMsgSize + room_space),(byte)0, (byte)PackMsg.MsgKind_e.PACK_PRED_MSG_KIND.ordinal(), refOffset);
        offset = refOffset.val;
        EncodePredictionMessage(predMsg, offset + room_space, chainChunkList,chainOffset.val);
        m_PredMsgSent++;
        LogUtility.LogFile("Leaving ProcessDataMsg (found) ", ModuleLogLevel);
        return predMsg;
    }
    public byte []ProcessFinallyProcessedDataMsg(byte []packet,int offset,byte Flags,int room_space)
    {
    	return ProcessDataMsg(packet,offset,Flags,room_space);
    }
    public byte []ProcessPredMsg(byte []packet,int offset,byte Flags,int room_space)
    {
    	try
    	{
    		throw new Exception("Exception in ProcessPredMsg");
    	}
    	catch(Exception exc)
    	{
    		exc.printStackTrace();
    	}
    	return null;
    }
    byte[] TryGeneratePredMsgOnPredAck(long chunk, int dummy_room_space)
    {
        List<ChunkListAndChainId> chainsChunksList = new LinkedList<ChunkListAndChainId>();
        if (m_chunkAndChainFileManager.GetChainAfterChunk(chunk, chainsChunksList) != 0)
        {
            chainsChunksList = null;
            return null;
        }
        ReferencedInteger offset = new ReferencedInteger();
        long predMsgSize = GetChainsListSize(chainsChunksList);
        offset.val = 0;
        byte[] predMsg = PackMsg.AllocateMsgAndBuildHeader((long)(predMsgSize + dummy_room_space), (byte)0, (byte)PackMsg.MsgKind_e.PACK_PRED_MSG_KIND.ordinal(), offset);
        EncodePredictionMessage(predMsg, offset.val + dummy_room_space, chainsChunksList, m_CurrentOffset);
        m_PredMsgSent++;
        LogUtility.LogFile(m_Id.toString() + " Generated PredMsg, size " + Long.toString(predMsgSize), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        return predMsg;
    }
    public byte []ProcessPredAckMsg(byte[] packet, int offset,byte Flags,int dummy_room_space)
    {
        List<ChunkMetaData> chunkMetaDataAndId;
        ReferencedLong chunksCount = new ReferencedLong();
        LogUtility.LogFile("PRED ACK message", LogUtility.LogLevels.LEVEL_LOG_HIGH);
        m_TotalPredAckSize += (long)(packet.length - offset);
        chunksCount.val = 0;
        chunkMetaDataAndId = DecodePredictionAckMessage(packet, offset, chunksCount);
        LogUtility.LogFile(m_Id.toString() + " ProcessPredAckMsg: Received msg size " + Integer.toString(packet.length - offset) + " Total PredAck size " + Long.toString(m_TotalPredAckSize) + " Chunks in msg " + Long.toString(chunksCount.val), LogUtility.LogLevels.LEVEL_LOG_HIGH3);
        ReceiverOnPredictionConfirm(chunkMetaDataAndId, chunksCount.val);
        byte[] predMsg = null;
        if ((chunkMetaDataAndId.size() >= chunksCount.val)&&(chunksCount.val > 0))
        {
            //predMsg = TryGeneratePredMsgOnPredAck(chunkMetaDataAndId[(int)chunksCount - 1].chunk, dummy_room_space);
        }
        if ((Flags & PackMsg.LastChunkFlag) == PackMsg.LastChunkFlag)
        {
        	m_ClientSideCallbacks.OnEnd();
        }
        LogUtility.LogFile(m_Id.toString() + " ProcessPredAckMsg: Total  " + Long.toString(m_CurrentOffset) , ModuleLogLevel);
        
        m_PredAckMsgReceived++;
        return predMsg;
    }

    public void OnDispose()
    {
    	WriteChunkChainsThread.OnComplete(m_Id,storeChunksAndChainsThread);
        m_chunkAndChainFileManager.OnDispose();
    }

    public byte[] ReceiverOnData(byte[] packet, int room_space)
    {
        ReferencedLong DataSize = new ReferencedLong();
        ReferencedByte MsgKind = new ReferencedByte();
        ReferencedByte Flags = new ReferencedByte();
        ReferencedInteger offset = new ReferencedInteger();
        
        if (PackMsg.DecodeMsg(packet, DataSize, Flags,MsgKind, offset) != 0)
        {
            return null;
        }
        switch (MsgKind_e.values()[MsgKind.val])
        {
            case PACK_DATA_MSG_KIND:
            case PACK_FINALLY_PROCESSED_DATA_MSG_KIND:
                return ProcessDataMsg(packet,offset.val,Flags.val,room_space);
            case PACK_PRED_ACK_MSG_KIND:
                ProcessPredAckMsg(packet, offset.val,Flags.val,0);
                break;
            default:
                return null;
        }
        return null;
    }

    public String GetDebugInfo()
    {
        String debugInfo = "ChunksProcessed " + Long.toString(m_ChunksProcessed) + " PredMsgSent " + Long.toString(m_PredMsgSent) + " PredAckMsgReceived " + Long.toString(m_PredAckMsgReceived) + m_chunkAndChainFileManager.GetDebugInfo();
        debugInfo += " Total " + Long.toString(m_CurrentOffset) + " TotalSaved " + Long.toString(m_TotalSaved) + " " + super.GetDebugInfo();
        return debugInfo;
    }
}

