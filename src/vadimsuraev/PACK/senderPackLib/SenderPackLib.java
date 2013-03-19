package vadimsuraev.PACK.senderPackLib;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import vadimsuraev.ByteArrayScalarTypeConversionLib.ByteArrayScalarTypeConversionLib;
import vadimsuraev.PACK.ChunkChainDataTypes.ChunkMetaData;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.PackChunking.PackChunking;
import vadimsuraev.PACK.PackMsg.PackMsg;
import vadimsuraev.PACK.PackMsg.PackMsg.MsgKind_e;
import vadimsuraev.ReferencedTypes.ReferencedByte;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.PACK.ProxyLib.Stream2Message.*;

class LongestMatch
{
    boolean m_ChainNotFound;
    long m_longestChunkCount;
    long m_longestChainLen;
    long m_longestProcessedBytes;
    LinkedList<Long> m_longestChunkList;
    long m_longestChainSenderFirstChunkIdx;
    long m_longestChainReceiverFirstChunkIdx;
    int m_Offset;
    LinkedList<ChunkMetaData> m_predMsg;
    private void SetFields(LinkedList<ChunkMetaData> predMsg,int offset,long ChunkCount,long ChainLen,long ProcessedBytes,LinkedList<Long> ChunkList,long SenderFirstChunkIdx,long ReceiverFirstChunkIdx)
    {
        LogUtility.LogFile("Setting fields offset " + Long.toString(offset) + " ChunkCount " + Long.toString(ChunkCount) + " ChainLen " + Long.toString(ChainLen) + " ProcessedBytes " + Long.toString(ProcessedBytes) + " Sender first " + Long.toString(SenderFirstChunkIdx) + " Receiver first " + Long.toString(ReceiverFirstChunkIdx), LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        m_predMsg = predMsg;
        m_Offset = offset;
        m_longestChunkCount = ChunkCount;
        m_longestChainLen = ChainLen;
        m_longestProcessedBytes = ProcessedBytes;
        m_longestChunkList = ChunkList;
        m_longestChainSenderFirstChunkIdx = SenderFirstChunkIdx;
        m_longestChainReceiverFirstChunkIdx = ReceiverFirstChunkIdx;
    }
    public void UpdateFields(LinkedList<ChunkMetaData> predMsg, int offset, long ChunkCount,long ChainLen, long ProcessedBytes,LinkedList<Long> ChunkList, long SenderFirstChunkIdx, long ReceiverFirstChunkIdx)
    {
        SetFields(predMsg, offset,ChunkCount,ChainLen, ProcessedBytes, ChunkList, SenderFirstChunkIdx, ReceiverFirstChunkIdx);
        m_ChainNotFound = false;
    }
    public boolean IsLonger(long matchLen)
    {
        LogUtility.LogFile("IsLonger " + Long.toString(m_longestChainLen) + " " + Long.toString(matchLen), LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        return (m_longestChainLen < matchLen);
    }
    public boolean IsMatchFound()
    {
        return (!m_ChainNotFound);
    }
    public long GetFirstSenderChunkIdx()
    {
        return m_longestChainSenderFirstChunkIdx;
    }
    public long GetFirstReceiverChunkIdx()
    {
        return m_longestChainReceiverFirstChunkIdx;
    }
    public List<Long> GetChunkList()
    {
        return m_longestChunkList;
    }
    public long GetLongestChainLen()
    {
        return m_longestChainLen;
    }
    public long GetLongestChunkCount()
    {
        return m_longestChunkCount;
    }
    public long GetLongestProcessedBytes()
    {
        return m_longestProcessedBytes;
    }
    public int GetRemainder(int length)
    {
        return (length - (int)(m_longestChainLen + m_Offset));
    }
    public int GetPrecedingBytesCount()
    {
        return m_Offset;
    }
    public List<ChunkMetaData> GetPredMsg()
    {
        return m_predMsg;
    }
    public LongestMatch()
    {
        m_ChainNotFound = true;
        SetFields(null,0,0, 0, 0, null, 0, 0);
    }
    public int GetOffset()
    {
        return m_Offset;
    }
}
class MatchStateMachine
{
    public static LogUtility.LogLevels ModuleLogLevel = LogUtility.LogLevels.LEVEL_LOG_MEDIUM;
    LongestMatch m_LongestMatch;
    PackChunking m_packChunking;
    SocketAddress m_Id;
    byte[] m_data;
    int m_ProcessedBytes;
    LinkedList<Long> m_SenderChunkList;
    LinkedList<Long> m_SenderChunkListWithSha1;        
    LinkedList<LinkedList<ChunkMetaData>> m_PredMsg;
    
    public MatchStateMachine(SocketAddress Id, LongestMatch longestMatch, PackChunking packChunking, byte[] data, LinkedList<LinkedList<ChunkMetaData>> predMsg)
    {
        m_LongestMatch = longestMatch;
        m_packChunking = packChunking;
        m_Id = Id;
        m_data = data;
        m_PredMsg = predMsg;
        m_SenderChunkList = new LinkedList<Long>();

        m_ProcessedBytes = m_packChunking.getChunks(m_SenderChunkList, m_data, 0, m_data.length, true, false);
        m_SenderChunkListWithSha1 = new LinkedList<Long>();
        m_packChunking.getChunks(m_SenderChunkListWithSha1, m_data, (int)0, m_data.length, true, true);
        LogUtility.LogFile(m_Id.toString() + " processedBytes " + Long.toString(m_ProcessedBytes) + " chunk count " + Long.toString(m_SenderChunkList.size()), ModuleLogLevel);
    }
    long GetMatchLen(long firstChunkIdx, long matchLen)
    {
        long len = 0;
        Iterator<Long> itr = m_SenderChunkList.subList((int)firstChunkIdx, (int)(firstChunkIdx + matchLen)).iterator();
        while(itr.hasNext())
        {
        	Long l = itr.next();
            len += (long)PackChunking.chunkToLen(l);
        }
        return len;
    }
    void OnEndOfMatch(LinkedList<ChunkMetaData> predMsg,long matchLen,long matchChunkCount,long firstSenderIdx,long firstReceiverIdx,int offset)
    {
        if (m_LongestMatch.IsLonger(matchLen))
        {
            /*matchLen*/
            matchChunkCount = IsSha1Match(firstSenderIdx, predMsg, firstReceiverIdx, /*matchLen*/matchChunkCount);
            if (matchChunkCount > 0)
            {
                matchLen = GetMatchLen(firstSenderIdx, matchChunkCount);
                LogUtility.LogFile(m_Id.toString() + " chain longer " + Long.toString(m_LongestMatch.GetLongestChainLen()), ModuleLogLevel);
                m_LongestMatch.UpdateFields(predMsg, offset, matchChunkCount, matchLen,(long)m_ProcessedBytes, m_SenderChunkList, firstSenderIdx, firstReceiverIdx);
            }
        }
    }
    void MatchChain(LinkedList<ChunkMetaData> predMsg)
    {
        try
        {
            long matchLen = 0;
            long matchChunkCount = 0;
            long firstSenderIdx = 0;
            long firstReceiverIdx = 0;
            int senderChunkIdx = 0;
            int receiverChunkIdx = 0;
            boolean match = false;
            int offset = 0;
            int savedOffset = 0;
            
            Iterator<Long> senderItr = m_SenderChunkList.iterator();
            Iterator<ChunkMetaData> recvItr = predMsg.iterator();

            while (senderItr.hasNext() && recvItr.hasNext())
            {
            	Long sndChunk = senderItr.next();
            	ChunkMetaData recvChunk = recvItr.next();
                byte senderHint = PackChunking.GetChunkHint(m_data, (long)offset, (long)PackChunking.chunkToLen(sndChunk));
                long senderChunk = PackChunking.chunkCode(0, PackChunking.chunkToLen(sndChunk));
                if (match)
                {
                	if ((senderChunk != PackChunking.chunkCode(0, PackChunking.chunkToLen(recvChunk.chunk.val))) ||
                			(senderHint != recvChunk.hint))
                	{
                		match = false;
                		LogUtility.LogFile(m_Id.toString() + "stopped. matching sha1 ", ModuleLogLevel);
                		OnEndOfMatch(predMsg, matchLen, matchChunkCount, firstSenderIdx, firstReceiverIdx, savedOffset);
                		if (matchLen >= (m_data.length / 3))
                		{
                			//return;
                		}
                	}
                	else
                	{
                		matchLen += (long)PackChunking.chunkToLen(sndChunk);
                		matchChunkCount++;
                		receiverChunkIdx++;
                		if (senderChunkIdx == (m_SenderChunkList.size() - 1))
                		{
                			LogUtility.LogFile(m_Id.toString() + "stopped (end). matching sha1 ", ModuleLogLevel);
                			OnEndOfMatch(predMsg, matchLen, matchChunkCount, firstSenderIdx, firstReceiverIdx, savedOffset);
                			if (matchLen >= (m_data.length / 3))
                			{
                				//return;
                			}
                		}
                	}
                }
                else
                {
                	receiverChunkIdx = (int)FindFirstMatchingChunk(predMsg, senderChunk, senderHint);
                	if (receiverChunkIdx != predMsg.size())
                	{
                		//       LogUtility.LogFile(m_Id.toString() + " match " + Long.toString(PackChunking.chunkToLen(predMsg[(int)receiverChunkIdx].chunk)), ModuleLogLevel);
                		match = true;
                		firstReceiverIdx = (long)receiverChunkIdx;
                		firstSenderIdx = (long)senderChunkIdx;
                		matchLen = (long)PackChunking.chunkToLen(sndChunk);
                		matchChunkCount = 1;
                		savedOffset = offset;
                		receiverChunkIdx++;
                	}
                	else
                	{
                		receiverChunkIdx = 0;
                	}
                }                   
                //LogUtility.LogFile("Sender's offset " + Long.toString(offset), ModuleLogLevel);
                offset += (int)PackChunking.chunkToLen(sndChunk);
                senderChunkIdx++;
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION: " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void Run()
    {
    	Iterator<LinkedList<ChunkMetaData>> itr = m_PredMsg.iterator();
        while(itr.hasNext())
        {
        	LinkedList<ChunkMetaData> chunkMetaDataList = itr.next();
            MatchChain(chunkMetaDataList);
        }
    }
    long FindFirstMatchingChunk(List<ChunkMetaData> chunksList, long chunk, byte hint)
    {
    	int idx = 0;
        Iterator<ChunkMetaData> itr = chunksList.iterator();
        while(itr.hasNext())
        {
        	ChunkMetaData chunkMetaData = itr.next();
            if ((PackChunking.chunkCode(0, PackChunking.chunkToLen(chunkMetaData.chunk.val)) == chunk) &&
                (chunkMetaData.hint == hint))
            {
                //LogUtility.LogFile(m_Id.toString() + "match " + Long.toString(idx), ModuleLogLevel);
                break;
            }
            idx++;
        }
        return idx;
    }

    long IsSha1Match(long senderFirstIdx, List<ChunkMetaData> receiverChunksList, long receiverFirstIdx, long matchLength)
    {
        long sha1;
        long idx = 0;

        Iterator<Long> sndItr = m_SenderChunkListWithSha1.subList((int) senderFirstIdx, m_SenderChunkListWithSha1.size()).iterator();
        Iterator<ChunkMetaData> rcvItr = receiverChunksList.subList((int) receiverFirstIdx, receiverChunksList.size()).iterator();
        while(sndItr.hasNext()&&rcvItr.hasNext()&&(idx < matchLength))
        {
        	Long sndChunk = sndItr.next();
        	ChunkMetaData rcvChunk = rcvItr.next();
            sha1 = PackChunking.chunkToSha1(sndChunk);
            if (sha1 != PackChunking.chunkToSha1(rcvChunk.chunk.val))
            {
                LogUtility.LogFile(m_Id.toString() + " sha mismatch " + Long.toString(idx) + " " + Long.toString(senderFirstIdx), ModuleLogLevel);
                return idx;
            }
            //LogUtility.LogFile(m_Id.toString() + " sha match " + Long.toString(idx) + " " + Long.toString(senderFirstIdx), ModuleLogLevel);
            senderFirstIdx++;
            receiverFirstIdx++;
        }
        return idx;
    }
}
public class SenderPackLib extends Stream2Message
{
    enum Sender_State_e
    {
        SENDER_HEADER_STATE,
        SENDER_MSG_BODY_STATE
    };
    
    private PackChunking packChunking;

    // for statistics & logging only
    long m_TotalDataReceived;
    long m_TotalDataSent;
    long m_TotalSavedData;
    //long m_TotalPreSaved;
    //long m_TotalPostSaved;
    //long m_TotalRawSent;
    long m_PredMsgReceived;
    long m_PredAckMsgSent;
    long m_DataMsgSent;
    SocketAddress m_Id;

    ReentrantLock m_libMutex;
    OnMsgReady4Tx m_OnMsgReady4Tx;
    Object m_onTxMessageParam;

    LinkedList<LinkedList<ChunkMetaData>> m_PredMsg;
    public void AddPredMsg(LinkedList<LinkedList<ChunkMetaData>> predMsg)
    {
        try
        {
            if (m_PredMsg != null)
            {
                LogUtility.LogFile(m_Id.toString() + " Concatinating to PRED " + Long.toString(m_PredMsg.size()) + " " + Long.toString(predMsg.size()), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                m_PredMsg.addAll(predMsg);
            }
            else
            {
                m_PredMsg = predMsg;
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    LinkedList<LinkedList<ChunkMetaData>> GetPredMsg()
    {
        return m_PredMsg;
    }
    void InitInstance(byte[]data)
    {
        m_PredMsg = null;
        packChunking = new PackChunking(8);
        m_TotalDataReceived = 0;
        m_TotalDataSent = 0;
        m_TotalSavedData = 0;
        //m_TotalRawSent = 0;
        m_PredMsgReceived = 0;
        m_PredAckMsgSent = 0;
        m_DataMsgSent = 0;
        m_libMutex = new ReentrantLock();
        LogUtility.LogFile("SenderLib:InitInstance", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    }
    public void SetOnMsgReady4TxParam(Object onTxMsgParam)
    {
    	m_onTxMessageParam = onTxMsgParam;
    }

    public long GetTotalAdded()
    {
        return m_TotalDataReceived;
    }
    public long GetTotalSent()
    {
        return m_TotalDataSent;
    }
    public long GetTotalSavedData()
    {
        return m_TotalSavedData;
    }
    public SenderPackLib(byte[] data,OnMsgReady4Tx onMsgReady4Tx)
    {
    	super();
        m_Id = new InetSocketAddress(0);
        m_OnMsgReady4Tx = onMsgReady4Tx;
        InitInstance(data);
    }
    
    void CopyBytesFromOffset(byte[] src,byte[] dst,  int dst_offset,int Count)
    {
        for (int i = 0; i < Count; i++)
        {
            dst[dst_offset+i] = src[i];
        }
    }

    void ForwardData(byte[] data)
    {
        ReferencedInteger offset = new ReferencedInteger();
        byte []msg = PackMsg.AllocateMsgAndBuildHeader((long)data.length, (byte)0, (byte)PackMsg.MsgKind_e.PACK_DATA_MSG_KIND.ordinal(), offset);
        CopyBytesFromOffset(data,msg,offset.val,(int)data.length);
        m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, msg,false);
        m_TotalDataSent += (long)data.length;
        //m_TotalRawSent += (long)data.Length;
        m_DataMsgSent++;
        LogUtility.LogFile(m_Id.toString() + /*" PreSaved " + Long.toString(m_TotalPreSaved) +*/ " Saved " + Long.toString(m_TotalSavedData) /*+ " PostSaved " + Long.toString(m_TotalPostSaved)*/ + " Received from server " + Long.toString(m_TotalDataReceived) + " Total sent to client " + Long.toString(m_TotalDataSent) /*+ " Sent raw " + Long.toString(m_TotalRawSent)*/, ModuleLogLevel);
    }

    void SendChunksData(byte []data,int count,boolean submit2Head)
    {
        int idx;
        ReferencedInteger offset_in_msg = new ReferencedInteger();
        byte[] msg;
        byte Msgkind;

        if (submit2Head)
        {
            Msgkind = (byte)PackMsg.MsgKind_e.PACK_FINALLY_PROCESSED_DATA_MSG_KIND.ordinal();
        }
        else
        {
            Msgkind = (byte)PackMsg.MsgKind_e.PACK_DATA_MSG_KIND.ordinal();
        }

        LogUtility.LogFile(m_Id.toString() + " Sending pre-PredAck DATA " + Long.toString(count), ModuleLogLevel);
        msg = PackMsg.AllocateMsgAndBuildHeader((long)count, (byte)0, Msgkind, offset_in_msg);
        for (idx = 0; idx < count; idx++)
        {
            msg[idx + offset_in_msg.val] = data[idx];
        }
        m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, msg, submit2Head);
        m_TotalDataSent += (long)count;
        //m_TotalPreSaved += (long)count;
        m_DataMsgSent++;
    }

    public boolean AddData(byte[] data,boolean invokedOnTransmit)
    {
        ReferencedInteger offset = new ReferencedInteger();
        boolean ret = true;
        byte[] msg;
        
        m_libMutex.lock();
        
        long offset_in_stream = m_TotalDataSent + m_TotalSavedData;
        
        m_TotalDataReceived += (long)data.length;
        LogUtility.LogFile("AddData: " + Long.toString(data.length) + " bytes received, isInvokedOnTx " + Boolean.toString(invokedOnTransmit), ModuleLogLevel);
        
        LongestMatch longestMatch = new LongestMatch();
        LinkedList<LinkedList<ChunkMetaData>> predMsgs = GetPredMsg();

        if (predMsgs != null)
        {
            LogUtility.LogFile(m_Id.toString() + " AddData: process " + Long.toString(predMsgs.size()) + " chains total received " + Long.toString(m_TotalDataReceived) + " total sent " + Long.toString(m_TotalDataSent) + " total saved " + Long.toString(m_TotalSavedData) + " data len " + Long.toString(data.length), ModuleLogLevel);
            MatchStateMachine matchStateMachine = new MatchStateMachine(m_Id, longestMatch, packChunking, data, predMsgs);
            matchStateMachine.Run();
        }
        
        if (!longestMatch.IsMatchFound())
        {
            LogUtility.LogFile(m_Id.toString() + " no match at all", ModuleLogLevel);

            if (!invokedOnTransmit)
            {
                ForwardData(data);
            }
            else
            {
                LogUtility.LogFile(m_Id.toString() + " not submitting - tx case", ModuleLogLevel);
                ret = false;
            }
            m_libMutex.unlock();
            return ret;
        }
        if (invokedOnTransmit)
        {
            m_TotalDataSent -= (long)data.length;
            //m_TotalPreSaved -= (long)count;
            m_DataMsgSent--;
            int remainder = longestMatch.GetRemainder(data.length);
            int alreadySent = (int)longestMatch.GetPrecedingBytesCount() + (int)longestMatch.GetLongestChainLen();
            if ((alreadySent + remainder) != data.length)
            {
                LogUtility.LogFile(m_Id.toString() + " something wrong: prec+chainLen+remainder " + Long.toString(longestMatch.GetPrecedingBytesCount()) + " " + Long.toString(longestMatch.GetLongestChainLen()) + " " + Long.toString(remainder) + " does not equal data.length " + Long.toString(data.length), ModuleLogLevel);
            }

            if (remainder > 0)
            {
                LogUtility.LogFile(m_Id.toString() + " sending pos-PredAck data " + Long.toString(remainder), ModuleLogLevel);
                msg = PackMsg.AllocateMsgAndBuildHeader((long)remainder, (byte)0, (byte)PackMsg.MsgKind_e.PACK_FINALLY_PROCESSED_DATA_MSG_KIND.ordinal(), offset);
                for (int i = alreadySent; i < data.length; i++)
                {
                    msg[(i - alreadySent) + offset.val] = data[i];
                }
                m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, msg, invokedOnTransmit);
                m_TotalDataSent += (long)remainder;
              //  m_TotalPostSaved += (long)remainder;
                m_DataMsgSent++;
            }
            byte[] buff = PackMsg.AllocateMsgAndBuildHeader(GetPredictionAckMessageSize((long)longestMatch.GetLongestChunkCount()), (byte)0, (byte)PackMsg.MsgKind_e.PACK_PRED_ACK_MSG_KIND.ordinal(), offset);
            EncodePredictionAckMessage(buff, offset.val, longestMatch.GetPredMsg(), (long)longestMatch.GetLongestChunkCount(), longestMatch.GetFirstReceiverChunkIdx());
            m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, buff, invokedOnTransmit);
            m_PredAckMsgSent++;
            if (longestMatch.GetPrecedingBytesCount() > 0)
            {
                SendChunksData(data, (int)longestMatch.GetPrecedingBytesCount(), invokedOnTransmit);
            }
        }
        else
        {
            if (longestMatch.GetPrecedingBytesCount() > 0)
            {
                SendChunksData(data, (int)longestMatch.GetPrecedingBytesCount(), invokedOnTransmit);
            }

            byte[] buff = PackMsg.AllocateMsgAndBuildHeader(GetPredictionAckMessageSize((long)longestMatch.GetLongestChunkCount()), (byte)0, (byte)PackMsg.MsgKind_e.PACK_PRED_ACK_MSG_KIND.ordinal(), offset);
            EncodePredictionAckMessage(buff, offset.val, longestMatch.GetPredMsg(), (long)longestMatch.GetLongestChunkCount(), longestMatch.GetFirstReceiverChunkIdx());
            m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, buff, invokedOnTransmit);
            m_PredAckMsgSent++;
            int remainder = longestMatch.GetRemainder(data.length);
            int alreadySent = (int)longestMatch.GetPrecedingBytesCount() + (int)longestMatch.GetLongestChainLen();
            if ((alreadySent + remainder) != data.length)
            {
                LogUtility.LogFile(m_Id.toString() + " something wrong: prec+chainLen+remainder " + Long.toString(longestMatch.GetPrecedingBytesCount()) + " " + Long.toString(longestMatch.GetLongestChainLen()) + " " + Long.toString(remainder) + " does not equal data.length " + Long.toString(data.length), ModuleLogLevel);
            }

            if (remainder > 0)
            {
                LogUtility.LogFile(m_Id.toString() + " sending pos-PredAck data " + Long.toString(remainder), ModuleLogLevel);
                msg = PackMsg.AllocateMsgAndBuildHeader((long)remainder, (byte)0, (byte)PackMsg.MsgKind_e.PACK_DATA_MSG_KIND.ordinal(), offset);
                for (int i = alreadySent; i < data.length; i++)
                {
                    msg[(i - alreadySent) + offset.val] = data[i];
                }
                m_OnMsgReady4Tx.OnMessageReadyToTx(m_onTxMessageParam, msg, invokedOnTransmit);
                m_TotalDataSent += (long)remainder;
                //m_TotalPostSaved += (long)remainder;
                m_DataMsgSent++;
            }
        }
        List<ChunkMetaData> listChunkMeta = longestMatch.GetPredMsg().subList((int)longestMatch.GetFirstReceiverChunkIdx(), (int)longestMatch.GetLongestChunkCount());
        longestMatch.GetPredMsg().removeAll(listChunkMeta);
        m_libMutex.unlock();
        LogUtility.LogFile(m_Id.toString() /*+ " PreSaved " + Long.toString(m_TotalPreSaved)*/ + " Saved " + Long.toString(m_TotalSavedData) /*+ " PostSaved " + Long.toString(m_TotalPostSaved)*/ + " Received from server " + Long.toString(m_TotalDataReceived) + " Total sent to client " + Long.toString(m_TotalDataSent) /*+ " Sent raw " + Long.toString(m_TotalRawSent)*/, ModuleLogLevel);
        return ret;
    }

    public void ClearData()
    {
        LogUtility.LogFile("SenderLib:ClearData", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    }

    public SenderPackLib(SocketAddress id, OnMsgReady4Tx onMsgReady4Tx)
    {
    	super();
        m_Id = id;
        m_OnMsgReady4Tx = onMsgReady4Tx;
        InitInstance(null);
    }    
    LinkedList<LinkedList<ChunkMetaData>> DecodePredictionMessage(byte[] buffer, int offset, ReferencedLong decodedOffsetInStream)
    {
        long buffer_idx = (long)offset;
        ReferencedLong chainsListSize = new ReferencedLong();
        LinkedList<LinkedList<ChunkMetaData>> chainsList;

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, buffer_idx, decodedOffsetInStream);

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, buffer_idx, chainsListSize);

        chainsList = new  LinkedList<LinkedList<ChunkMetaData>>();

        for (int chain_idx = 0; chain_idx < chainsListSize.val;chain_idx++ )
        {
        	ReferencedLong chunkListSize = new ReferencedLong();
            
            buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, buffer_idx, chunkListSize);
            LinkedList<ChunkMetaData> chunkMetaDataList = new LinkedList<ChunkMetaData>();
            for (long idx = 0; idx < chunkListSize.val; idx++)
            {
                ChunkMetaData chunkMetaData = new ChunkMetaData();
                chunkMetaData.hint = buffer[(int) buffer_idx++];
                buffer_idx +=
                ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, buffer_idx, chunkMetaData.chunk);
                chunkMetaDataList.add(chunkMetaData);
            }
            chainsList.add(chunkMetaDataList);
        }
        return chainsList;
    }
    long GetPredictionAckMessageSize(long chunksCount)
    {
        return (Long.SIZE/8 + Long.SIZE/8 /*+ sizeof(long)*/ + (long)chunksCount * ChunkMetaData.GetSize());
    }
    void EncodePredictionAckMessage(byte[] buffer, int offset, List<ChunkMetaData> chunkMetaDataList,long chunksCount, long firstChunk)
    {
        long buffer_idx = (long)offset;
        long chunkCounter = 0;
        long thisTimeSaved = 0;

        buffer_idx +=
                ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer, buffer_idx, chunksCount);

        Iterator<ChunkMetaData> itr = chunkMetaDataList.iterator();
        while(itr.hasNext())
        {
        	ChunkMetaData chunkMetaData = itr.next();
            if (chunkCounter >= firstChunk)
            {
                buffer[(int) buffer_idx++] = chunkMetaData.hint;
                buffer_idx +=
                    ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer, buffer_idx, chunkMetaData.chunk.val);
                thisTimeSaved += (long)PackChunking.chunkToLen(chunkMetaData.chunk.val);
            }
            chunkCounter++;
            if ((chunkCounter-firstChunk) == chunksCount)
            {
                break;
            }
        }
        LogUtility.LogFile(m_Id.toString() + " sending PRED ACK saved now " + Long.toString(thisTimeSaved) + " chunks " + Long.toString((chunkCounter - firstChunk)), ModuleLogLevel);
        m_TotalSavedData += (long)thisTimeSaved;
    }
    @Override
    public byte []ProcessPredMsg(byte []packet,int offset,byte Flags,int room_space)
    {
        LinkedList<LinkedList<ChunkMetaData>> chunkMetaDataList;
        ReferencedLong decodedOffsetInStream = new ReferencedLong();
        m_libMutex.lock();
        long offset_in_stream = m_TotalDataSent + m_TotalSavedData;
        m_PredMsgReceived++;
        LogUtility.LogFile("PRED Message", ModuleLogLevel);
        chunkMetaDataList = DecodePredictionMessage(packet, offset, decodedOffsetInStream);
        LogUtility.LogFile("chainOffset " + Long.toString(decodedOffsetInStream.val) + " TotalSent " + Long.toString(m_TotalDataSent) + " TotalSaved " + Long.toString(m_TotalSavedData), ModuleLogLevel);
        if (chunkMetaDataList == null)
        {
            m_libMutex.unlock();
            return null;
        }
        LogUtility.LogFile(m_Id.toString() + " processing " + Long.toString(chunkMetaDataList.size()) + " chains, offset " + Long.toString(decodedOffsetInStream.val) + " offset in stream " + Long.toString(offset_in_stream), ModuleLogLevel);
        AddPredMsg(chunkMetaDataList);
        m_libMutex.unlock();
        return null;
    }
    public byte[] SenderOnData(byte[] packet,int room_space)
    {
        ReferencedLong DataSize = new ReferencedLong();
        ReferencedByte MsgKind = new ReferencedByte();
        ReferencedByte Flags = new ReferencedByte();
        ReferencedInteger offset = new ReferencedInteger();
        int rc;

        if ((rc = PackMsg.DecodeMsg(packet, DataSize, Flags,MsgKind, offset)) != 0)
        {
            LogUtility.LogFile(m_Id.toString() + " Message cannot be decoded", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return null;
        }
        switch (MsgKind_e.values()[MsgKind.val])
        {
            case PACK_PRED_MSG_KIND:
                return ProcessPredMsg(packet, offset.val, Flags.val, room_space);
            default:
                return null;
        }
    }

    public String GetDebugInfo()
    {
    	String debugInfo = "";

        debugInfo += " TotalDataReceived " + Long.toString(m_TotalDataReceived) + " TotalSent " + Long.toString(m_TotalDataSent) + " TotalSaved " + Long.toString(m_TotalSavedData) /*+ " TotalPreSaved " + Long.toString(m_TotalPreSaved) + " TotalPostSaved " + Long.toString(m_TotalPostSaved)*/ + " PredMsgReceived " + Long.toString(m_PredMsgReceived) + " PredAckSent" + Long.toString(m_PredAckMsgSent) + " DataMsgSent " + Long.toString(m_DataMsgSent) + " " + super.GetDebugInfo();
        return debugInfo;
    }
}
