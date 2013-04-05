/*using System;
using System.Collections.Generic;
using System.Collections;
using System.Linq;
using System.Text;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using RxTxStateMachine;
using ProxyLibTypes;
using PackMsg;*/
//using System.Runtime.Remoting.Contexts;

package vadimsuraev.PACK.ProxyLib;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedList;
import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.ReferencedTypes.ReferencedBoolean;
import vadimsuraev.ReferencedTypes.ReferencedByte;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.PACK.ProxyLib.RxTxStateMachine.*;
import vadimsuraev.MyMemoryStream.*;
import vadimsuraev.PACK.PackMsg.*;
//import vadimsuraev.PACK.PackReceiverSide.*;
import vadimsuraev.PACK.ReceiverPackLib.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Proxy implements SocketCallbacks, IOnGotResults
{
    public static LogUtility.LogLevels ModuleLogLevel = LogUtility.LogLevels.LEVEL_LOG_MEDIUM;

    protected LinkedList<byte []> m_clientTxQueue;
    protected LinkedList<byte []> m_destinationTxQueue;
    protected ReentrantLock m_clientTxQueueMutex;
    protected ReentrantLock m_destinationTxQueueMutex;
    protected ProxySocket m_clientSideSocket;
    protected ProxySocket m_destinationSideSocket;
    protected RxStateMachine m_rxStateMachine;
    protected TxStateMachine m_txStateMachine;
    protected ReentrantLock m_ProprietarySegmentTxMutex;
    protected ReentrantLock m_NonProprietarySegmentTxMutex;
    protected ReentrantLock m_ProprietarySegmentRxMutex;
    protected ReentrantLock m_NonProprietarySegmentRxMutex;
    protected ReentrantLock m_disposeMutex;
    static protected ReentrantLock m_proprietaryLibMutex = new ReentrantLock();

    protected ReentrantLock m_clientStreamMutex;
    protected ReentrantLock m_destinationStreamMutex;

    protected Boolean  m_ProprietarySegmentTxInProgress;
    protected Boolean  m_ProprietarySegmentRxInProgress;
    protected Boolean  m_NonProprietarySegmentTxInProgress;
    protected Boolean  m_NonProprietarySegmentRxInProgress;
    protected Boolean   m_IsMsgBeingTransmitted2Client;
    protected Boolean m_IsMsgBeingTransmitted2Destination;
    protected SocketAddress m_Id;
    protected byte []m_NonProprietarySegmentRxBuf;
    protected byte[] m_ProprietarySementRxBuf;
    protected Boolean m_ShutDownFlag;
    protected long m_TransmittedClient;
    protected long m_ReceivedClient;
    protected long m_TransmittedServer;
    protected long m_ReceivedServer;
    protected long m_TransmittedMsgsClient;
    protected long m_ReceivedMsgs;
    protected long m_SubmittedMsgsClient;
    protected long m_SubmittedMsgsServer;
    protected long m_SubmittedClient;
    protected long m_SubmittedServer;
    protected long m_Saved;
    protected MyMemoryStream m_clientStream;
    protected MyMemoryStream m_destinationStream;
    protected IOnGotResults m_OnGotResults;
    public static void InitGlobalObjects()
    {
        //PackClientSide.InitGlobalObjects();
    }
    public Proxy(IOnGotResults onGotResults)
    {
        m_clientTxQueue = new LinkedList<byte[]>();
        m_destinationTxQueue = new LinkedList<byte[]>();
        m_clientTxQueueMutex = new ReentrantLock();
        m_destinationTxQueueMutex = new ReentrantLock();
        m_ProprietarySegmentTxMutex = new ReentrantLock();
        m_NonProprietarySegmentTxMutex = new ReentrantLock();
        m_ProprietarySegmentRxMutex = new ReentrantLock();
        m_NonProprietarySegmentRxMutex = new ReentrantLock();
        m_clientStreamMutex = new ReentrantLock();
        m_destinationStreamMutex = new ReentrantLock();
        m_disposeMutex = new ReentrantLock();
        m_ProprietarySegmentTxInProgress = false;
        m_ProprietarySegmentRxInProgress = false;
        m_NonProprietarySegmentTxInProgress = false;
        m_NonProprietarySegmentRxInProgress = false;
        m_IsMsgBeingTransmitted2Client = false;
        m_IsMsgBeingTransmitted2Destination = false;
        m_NonProprietarySegmentRxBuf = new byte[8192 * 4];
        m_ProprietarySementRxBuf = new byte[8192*4];
        m_rxStateMachine = new RxStateMachine(m_Id);
        m_txStateMachine = new TxStateMachine(m_Id);
        m_ShutDownFlag = false;
        m_TransmittedClient = 0;
        m_ReceivedClient = 0;
        m_TransmittedServer = 0;
        m_ReceivedServer = 0;
        m_TransmittedMsgsClient = 0;
        m_ReceivedMsgs = 0;
        m_SubmittedMsgsClient = 0;
        m_SubmittedMsgsServer = 0;
        m_SubmittedClient = 0;
        m_SubmittedServer = 0;
        m_Saved = 0;
        m_clientStream = new MyMemoryStream();
        m_destinationStream = new MyMemoryStream();
        m_OnGotResults = onGotResults;
        LogUtility.LogFile("Started at " + Long.toString((new Date().getTime())), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    }

    protected void OnProprietarySegmentReceived(int Received)
    {
    }
    protected void OnProprietarySegmentTransmitted(int Transmitted)
    {
    }
    protected void OnNonProprietarySegmentReceived(int Received)
    {
    }
    protected void OnNonProprietarySegmentTransmitted(int Transmitted)
    {
    }
    public void OnRead(Object data)
    {
    }
    public void OnWritten(Object data)
    {
    }
    public void SetRemoteEndpoint(InetSocketAddress ipEndpoint)
    {
    }
    
    public Object GetResults()
    {
        return null;
    }
    public boolean ProprietaryTxMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_ProprietarySegmentTxMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_ProprietarySegmentTxMutex.unlock();
        }
        return ret;
    }
    public boolean ProprietaryRxMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_ProprietarySegmentRxMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_ProprietarySegmentRxMutex.unlock();
        }
        return ret;
    }
    public boolean NonProprietaryTxMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_NonProprietarySegmentTxMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_NonProprietarySegmentTxMutex.unlock();
        }
        return ret;
    }
    public boolean NonProprietaryRxMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_NonProprietarySegmentRxMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_NonProprietarySegmentRxMutex.unlock();
        }
        return ret;
    }
    public boolean ClientMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_clientStreamMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_clientStreamMutex.unlock();
        }
        return ret;
    }
    public boolean DestinationMutexAvailable()
    {
        boolean ret = false;
		try {
			ret = m_destinationStreamMutex.tryLock(100,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (ret)
        {
            m_destinationStreamMutex.unlock();
        }
        return ret;
    }
    protected abstract boolean ClientTxInProgress();
    protected void CheckConnectionAndShutDownIfGone()
    {
        return;
    }
    protected boolean IsAlive()
    {
        try
        {
            if (!m_destinationSideSocket.isConnected())
            {
                LogUtility.LogFile(m_Id.toString() +" Socket disconnected ", ModuleLogLevel);
            }
            return m_destinationSideSocket.isConnected();
        }
        catch (Exception exc)
        {
            return false;
        }
    }
    public boolean EnterProprietarySegmentTxCriticalArea(boolean wait)
    {
        try
        {
            boolean ret;

            if (wait)
            {
            	m_ProprietarySegmentTxMutex.lock();
            	ret = true;
            }
            else
            {
            	ret = m_ProprietarySegmentTxMutex.tryLock();
            }
            //Monitor.Enter(m_ProprietarySegmentTxMutex);
            return ret;
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return false;
        }
    }
    public void LeaveProprietarySegmentTxCriticalArea()
    {
        try
        {
            //ProprietarySegmentTxMutex.ReleaseMutex();
            m_ProprietarySegmentTxMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public boolean EnterNonProprietarySegmentTxCriticalArea(boolean wait)
    {
        try
        {
            boolean ret;

            if (wait)
            {
            	m_NonProprietarySegmentTxMutex.lock();
                ret = true;
            }
            else
            {
                ret = m_NonProprietarySegmentTxMutex.tryLock();
            }
            //Monitor.Enter(m_NonProprietarySegmentTxMutex);
            return ret;
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return false;
        }
    }
    public void LeaveNonProprietarySegmentTxCriticalArea()
    {
        try
        {
            m_NonProprietarySegmentTxMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public boolean EnterProprietarySegmentRxCriticalArea(boolean wait)
    {
        try
        {
            boolean ret;

            if (wait)
            {
            	m_ProprietarySegmentRxMutex.lock();
                ret = true;
            }
            else
            {
                ret = m_ProprietarySegmentRxMutex.tryLock();
            }
            //Monitor.Enter(m_ProprietarySegmentRxMutex);
            return ret;
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return false;
        }
    }
    public void LeaveProprietarySegmentRxCriticalArea()
    {
        try
        {
            //ProprietarySegmentRxMutex.ReleaseMutex();
            m_ProprietarySegmentRxMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public boolean EnterNonProprietarySegmentRxCriticalArea(boolean wait)
    {
        try
        {
            boolean ret;

            if (wait)
            {
            	m_NonProprietarySegmentRxMutex.lock();
                ret = true;
            }
            else
            {
                ret = m_NonProprietarySegmentRxMutex.tryLock();
            }
            //Monitor.Enter(m_NonProprietarySegmentRxMutex);
            return ret;
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return false;
        }
    }
    public void LeaveNonProprietarySegmentRxCriticalArea()
    {
        try
        {
            //NonProprietarySegmentRxMutex.ReleaseMutex();
            m_NonProprietarySegmentRxMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void EnterClientStreamCriticalArea()
    {
        try
        {
            //ProprietarySegmentTxMutex.WaitOne();
            m_clientStreamMutex.lock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void LeaveClientStreamCriticalArea()
    {
        try
        {
            //ProprietarySegmentTxMutex.ReleaseMutex();
            m_clientStreamMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void EnterDestinationStreamCriticalArea()
    {
        try
        {
            m_destinationStreamMutex.lock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void LeaveDestinationStreamCriticalArea()
    {
        try
        {
            m_destinationStreamMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void EnterProprietaryLibCriticalArea()
    {
        try
        {
            m_proprietaryLibMutex.lock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void LeaveProprietaryLibCriticalArea()
    {
        try
        {
            //ProprietarySegmentTxMutex.ReleaseMutex();
            m_proprietaryLibMutex.unlock();
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Convert.ToString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }

    void EnterClientMsgTxQueue()
    {
        try
        {
            m_clientTxQueueMutex.lock();
        }
        catch (Exception exc)
        {
        }
    }

    void LeaveClientMsgTxQueue()
    {
        try
        {
            m_clientTxQueueMutex.unlock();
        }
        catch (Exception exc)
        {
        }
    }

    void EnterDestinationMsgTxQueue()
    {
        try
        {
            m_destinationTxQueueMutex.lock();
        }
        catch (Exception exc)
        {
        }
    }

    void LeaveDestinationMsgTxQueue()
    {
        try
        {
            m_destinationTxQueueMutex.unlock();
        }
        catch (Exception exc)
        {
        }
    }
    
    public void SubmitStream4ClientTx(byte []data)
    {
        EnterClientStreamCriticalArea();
        try
        {
            m_clientStream.AddBytes((byte[])data);
            m_SubmittedMsgsClient++;
            m_SubmittedClient += (long)((byte[])data).length;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LeaveClientStreamCriticalArea();
    }
    public void SubmitMsg4ClientTx(byte[] data,boolean submit2Head)
    {
        try
        {
            EnterClientMsgTxQueue();
            if (submit2Head)
            {
                m_clientTxQueue.addFirst(data);
            }
            else
            {
                m_clientTxQueue.addLast(data);
            }
            LeaveClientMsgTxQueue();
            m_SubmittedMsgsClient++;
            m_SubmittedClient += (long)data.length;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public boolean IsClientTxQueueEmpty()
    {
        try
        {
            int count;
            EnterClientMsgTxQueue();
            count = m_clientTxQueue.size();
            LeaveClientMsgTxQueue();
            return ((m_clientStream.Length() == 0) && (count == 0));
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        return true;
    }
    public void SubmitStream4DestinationTx(byte []data)
    {
        EnterDestinationStreamCriticalArea();
        try
        {
        	LogUtility.LogFile("In SubmitStream4DestinationTx: " + Long.toString(data.length), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            m_destinationStream.AddBytes(data);
            m_SubmittedMsgsServer++;
            m_SubmittedServer += (long)data.length;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LeaveDestinationStreamCriticalArea();
    }
    public void SubmitMsg4DestinationTx(byte []data)
    {
        try
        {
            EnterDestinationMsgTxQueue();
            m_destinationTxQueue.addFirst(data);
            LeaveDestinationMsgTxQueue();
            m_SubmittedMsgsServer++;
            m_SubmittedServer += (long)data.length;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public boolean ProcessDownStreamData(byte[] data,boolean isInvokedOnTransmit)
    {
        return false;/* message is not intercepted */
    }
    public byte []DebugGetClient2Transmit(ReferencedLong streamLength, ReferencedBoolean isMsg)
    {
    	byte []data = null;
    	
    	EnterClientMsgTxQueue();
        if (!m_clientTxQueue.isEmpty())
        {
            data  = m_clientTxQueue.removeFirst();
            isMsg.val = true;
            m_IsMsgBeingTransmitted2Client = true;
            streamLength.val = (long)data.length;
        }
        LeaveClientMsgTxQueue();
        return data;
    }
    public byte []GetClient2Transmit(ReferencedLong streamLength, ReferencedBoolean isMsg)
    {
        LogUtility.LogFile("Entering GetClient2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        try
        {
            byte []data;
            
            boolean isSecondPass = false;

	        do
	        {
	        	data = null;
	        	EnterClientMsgTxQueue();
	            if (!m_clientTxQueue.isEmpty())
	            {
	                data  = m_clientTxQueue.removeFirst();
	            }
	            if(data != null)
	            {
	                if (!isSecondPass)
	                {
	                	ReferencedLong DataSize = new ReferencedLong();
	                	ReferencedByte DummyFlags = new ReferencedByte();
	                    ReferencedByte MsgKind = new ReferencedByte();
	                    ReferencedInteger Offset = new ReferencedInteger();
	                    PackMsg.DecodeMsg(data,DataSize,DummyFlags,MsgKind,Offset);
	                    if (MsgKind.val == (byte)PackMsg.MsgKind_e.PACK_DATA_MSG_KIND.ordinal())
	                    {
	                        byte []msg = new byte[(int) DataSize.val];
	                        CopyBytesFromOffset(data,Offset.val,msg,(int) DataSize.val);
	                        isSecondPass = true;
	                        LogUtility.LogFile("Reprocess " + Long.toString(DataSize.val) + " of data", ModuleLogLevel);
	                        if (ProcessDownStreamData(msg, true))
	                        {
	                            LogUtility.LogFile("Data message has been intercepted on tx!!!", ModuleLogLevel);
	                            msg = null;
	                            continue;
	                        }
	                        msg = null;
	                        //isSecondPass = true;
	                    }
	                }
	                LogUtility.LogFile("Queue is not empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
	                streamLength.val = (long)data.length;
	                isMsg.val = true;
	                m_IsMsgBeingTransmitted2Client = true;
	                LogUtility.LogFile("Leaving GetClient2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
	                return data;
	            }
	            LeaveClientMsgTxQueue();
	        }while(false);
            
            
            LogUtility.LogFile("Queue is empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            EnterClientStreamCriticalArea();
            if (m_clientStream.Length() == 0)
            {
                streamLength.val = 0;
                isMsg.val = false;
                LogUtility.LogFile("Stream is empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                LogUtility.LogFile("Leaving GetClient2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                LeaveClientStreamCriticalArea();
                return null;
            }
            LogUtility.LogFile("Stream is not empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);

            isMsg.val = false;
            m_IsMsgBeingTransmitted2Client = false;
            streamLength.val = (long)m_clientStream.Length();
            byte []bytes = m_clientStream.GetBytes();
            LogUtility.LogFile("Leaving GetClient2Transmit " + Long.toString(m_clientStream.Length()), LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            LeaveClientStreamCriticalArea();
            return bytes;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            streamLength.val = 0;
            isMsg.val = false;
            LogUtility.LogFile("Leaving GetClient2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            return null;
        }
    }
    public byte []GetDestination2Transmit(ReferencedLong streamLength, ReferencedBoolean isMsg)
    {
        try
        {
            byte[] data;
            data = null;
            EnterDestinationMsgTxQueue();
            if (!m_destinationTxQueue.isEmpty())
            {
                data = m_destinationTxQueue.removeFirst();
            }
            LeaveDestinationMsgTxQueue();
            if (data != null)
            {
                LogUtility.LogFile("Queue is not empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                streamLength.val = (long)data.length;
                isMsg.val = true;
                m_IsMsgBeingTransmitted2Destination = true;
                LogUtility.LogFile("Leaving GetDestination2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                return data;
            }
            LogUtility.LogFile("Queue is empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            EnterDestinationStreamCriticalArea();
            if (m_destinationStream.Length() == 0)
            {
                streamLength.val = 0;
                isMsg.val = false;
                LogUtility.LogFile("Stream is empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                LogUtility.LogFile("Leaving GetClient2Transmit", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
                LeaveDestinationStreamCriticalArea();
                return null;
            }
            LogUtility.LogFile("Stream is not empty", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            streamLength.val = (long)m_destinationStream.Length();
            isMsg.val = false;
            m_IsMsgBeingTransmitted2Destination = false;
            byte[] bytes = m_destinationStream.GetBytes();
            LogUtility.LogFile("Leaving GetDestination2Transmit " + Long.toString(m_destinationStream.Length()), LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
            LeaveDestinationStreamCriticalArea();
            return bytes;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            streamLength.val = 0;
            isMsg.val = false;
            return null;
        }
    }
    public void OnDestinationTransmitted(int sent)
    {
        if (!m_IsMsgBeingTransmitted2Destination)
        {
            LogUtility.LogFile("incrementing dest stream  " + Integer.toString(sent), ModuleLogLevel);
            EnterDestinationStreamCriticalArea();
            m_destinationStream.IncrementOffset((long)sent);
            LeaveDestinationStreamCriticalArea();
            LogUtility.LogFile("done  ", ModuleLogLevel);
        }
    }
    public void OnClientTransmitted(int sent)
    {
        if (!m_IsMsgBeingTransmitted2Client)
        {
            LogUtility.LogFile("incrementing client stream  " + Integer.toString(sent), ModuleLogLevel);
            EnterClientStreamCriticalArea();
            m_clientStream.IncrementOffset((long)sent);
            LeaveClientStreamCriticalArea();
            LogUtility.LogFile("done  ", ModuleLogLevel);
        }
    }
    public void CopyBytes(byte[] src, byte[] dst, int Count)
    {
        for (int i = 0; i < Count; i++)
        {
            dst[i] = src[i];
        }
    }
    public void CopyBytesFromOffset(byte[] src, int src_offset,byte[] dst, int Count)
    {
        for (int i = 0; i < Count; i++)
        {
            dst[i] = src[i+src_offset];
        }
    }
    
    public abstract void Start();
    protected void NonProprietarySegmentTransmit()
    {
    }
    protected void ProprietarySegmentTransmit()
    {
    }
    protected void NonProprietarySegmentReceive()
    {
    }
    protected void ProprietarySegmentReceive()
    {
    }

    protected void ReStartAllOperations(boolean skipCheckIsAlive)
    {
        if (!skipCheckIsAlive)
        {
            if (!IsAlive())
            {
                return;
            }
        }
        NonProprietarySegmentTransmit();
        ProprietarySegmentTransmit();
        NonProprietarySegmentReceive();
        ProprietarySegmentReceive();
    }

    protected void OnDestinationDisconnected()
    {
    	String idStr = "";
    	if(m_Id != null)
    	{
    		idStr = m_Id.toString();
    	}
        LogUtility.LogFile(idStr + " OnDestinationDisconnected", LogUtility.LogLevels.LEVEL_LOG_HIGH);
        
        try
        {
            //m_clientSideDiscAsyncArgs.DisconnectReuseSocket = false;
            //clientSideSocket.DisconnectAsync(m_clientSideDiscAsyncArgs);
        }
        catch (Exception exc)
        {
            LogUtility.LogException(idStr,exc , LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        try
        {
//            clientSideSocket.BeginDisconnect(false, new AsyncCallback(OnClientDisconnected), null);
            m_clientSideSocket.Disconnect();
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(idStr,exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }

    protected void OnClientDisconnected()
    {
        LogUtility.LogFile(m_Id.toString() + " OnClientDisconnected", LogUtility.LogLevels.LEVEL_LOG_HIGH);
        try
        {
            CleanUp();
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    protected void OnBeginShutdown()
    {
    }
    protected void Disposing()
    {
    }
    protected void Disposed()
    {
    }
    protected long GetSaved()
    {
        return m_Saved;
    }
    protected long GetPreSaved()
    {
        return 0;
    }
    protected long GetPostSaved()
    {
        return 0;
    }
    void CleanUp()
    {
        try
        {
            Disposing();
            LogUtility.LogFile(m_Id.toString() + " *****************************destroying all******************************", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            m_NonProprietarySegmentRxBuf = null;
            m_ProprietarySementRxBuf = null;
            if (m_clientStream != null)
            {
                m_clientStream.Clear();
                m_clientStream = null;
            }
            m_ProprietarySegmentTxMutex = null;
            m_ProprietarySegmentRxMutex = null;
            m_NonProprietarySegmentRxMutex = null;
            m_NonProprietarySegmentTxMutex = null;
            if (m_clientTxQueue != null)
            {
                m_clientTxQueue.clear();
                m_clientTxQueue = null;
            }
            if (m_destinationTxQueue != null)
            {
                m_destinationTxQueue.clear();
                m_destinationTxQueue = null;
            }
            m_txStateMachine = null;
            m_rxStateMachine = null;
            m_destinationSideSocket = null;
            m_clientSideSocket = null;
            LogUtility.LogFile("ID " + m_Id.toString() + " TransmittedClient " + Long.toString(m_TransmittedClient) + " ReceivedClient " + Long.toString(m_ReceivedClient) + " TransmittedServer " + Long.toString(m_TransmittedServer) + " ReceivedServer " + Long.toString(m_ReceivedServer) + " TransmittedMsgsClient " + Long.toString(m_TransmittedMsgsClient) + " ReceivedMsgs " + Long.toString(m_ReceivedMsgs) + " SubmittedMsgsClient " + Long.toString(m_SubmittedMsgsClient) + " SubmittedMsgsServer " + Long.toString(m_SubmittedMsgsServer) + " SubmittedClient " + Long.toString(m_SubmittedClient) + " SubmittedServer " + Long.toString(m_SubmittedServer) + " Saved " + GetSaved() + " PreSaved " + GetPreSaved() + " PostSaved " + GetPostSaved(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            Disposed();
            m_disposeMutex = null;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    public void OnConnectionBroken(Object data)
    {
    }
    public void Dispose()
    {
        try
        {
            m_destinationSideSocket.Disconnect();
            LogUtility.LogFile(m_Id.toString() + "  connection is broken - DONE ", ModuleLogLevel);
        }
        catch (Exception exc)
        {
        }
    }
    public static void Flush()
    {
        ReceiverPackLib.Flush();
    }
    public void OnGotResults(Object results) 
    {
    	if(m_OnGotResults != null)
    	{
	        m_OnGotResults.OnGotResults(results);
    	}
	}
}
