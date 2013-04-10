package vadimsuraev.PACK.ProxyLib.Server;

import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.*;
import vadimsuraev.MyMemoryStream.MyMemoryStream;
import vadimsuraev.PACK.ProxyLib.Proxy;
import vadimsuraev.PACK.ProxyLib.ProxySocket;
import vadimsuraev.PACK.ProxyLib.ProxyLibTypes.*;
import vadimsuraev.ReferencedTypes.ReferencedBoolean;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.PACK.ProxyLib.RxTxStateMachine.*;

public abstract class ServerSideProxy extends Proxy implements OnMessageCallback
{
    protected boolean m_ErrorSent;

    public ServerSideProxy(SocketChannel sock)
    {
    	super(null);
    	String temp = Integer.toString(sock.socket().getPort());
        m_clientSideSocket = new ProxySocket("ClientProp" + temp,sock,true,this);
        m_Id = m_clientSideSocket.GetlocalEndPoint();
        m_rxStateMachine.SetEndPoint(m_Id);
        m_txStateMachine.SetEndPoint(m_Id);
        m_destinationSideSocket = null;
        m_ErrorSent = false;
    }
    
    protected void _OnProprietarySegmentTransmitted(int transmitted)
    {
    	/*if (m_txStateMachine.IsInBody())
    	{
    		if (m_txStateMachine.IsWholeMessage())
    		{
    			OnClientTransmitted(transmitted - m_txStateMachine.GetHeaderLength());
    		}
    		else
    		{
    			OnClientTransmitted(transmitted);
    		}
    	}*/
    	m_TransmittedClient += (long)transmitted;
    	LogUtility.LogFile(m_Id.toString() + " sent (proprietary segment) " + Integer.toString(transmitted), ModuleLogLevel);
    	/*if(!m_txStateMachine.OnTxComplete((long)transmitted))
    	{
    		LogUtility.LogFile(m_Id.toString() + " OnTxComplete returned FALSE!!!! ", ModuleLogLevel);
    	}
    	else
    	{
    		LogUtility.LogFile(m_Id.toString() + " OnTxComplete returned TRUE ", ModuleLogLevel);
    	}
    	if (m_txStateMachine.IsTransactionCompleted())
    	{
    		m_txStateMachine.ClearMsgBody();
    	}*/
    }

    protected void OnProprietarySegmentTransmitted(int Ret)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentTransmitted", ModuleLogLevel);
        EnterProprietarySegmentTxCriticalArea(true);
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (!m_ProprietarySegmentTxInProgress)
            {
                LogUtility.LogFile("OnProprietarySegmentTransmitted: tx is not in progress,return", ModuleLogLevel);
                LeaveProprietarySegmentTxCriticalArea();
                return;
            }
            
            if (Ret <= 0)
            {
                LogUtility.LogFile("!!!Proprietary: transferred < 0", LogUtility.LogLevels.LEVEL_LOG_HIGH3);
                LeaveProprietarySegmentTxCriticalArea();
                ReStartAllOperations(!m_OnceConnected);
                return;
            }
            m_TransmittedClient += (long)Ret;
            LogUtility.LogFile(m_Id.toString() + " sent (proprietary segment) " + Long.toString(Ret), ModuleLogLevel);
            if (m_ErrorSent)
            {
                LogUtility.LogFile(m_Id.toString() + " ErrorSent flag is up, checking if queue is empty", ModuleLogLevel);
                if (IsClientTxQueueEmpty())
                {
                    LogUtility.LogFile(m_Id.toString() + "OnProprietarySegmentTransmitted: queue is empty and error is sent", LogUtility.LogLevels.LEVEL_LOG_HIGH3);
                    Dispose();
                    LeaveProprietarySegmentTxCriticalArea();
                    return;
                }
            }
            m_ProprietarySegmentTxInProgress = false;
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.toString(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentTransmitted", ModuleLogLevel);
        LeaveProprietarySegmentTxCriticalArea();
        ReStartAllOperations(!m_OnceConnected);
    	/*LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentTransmitted", ModuleLogLevel);
    	EnterProprietarySegmentTxCriticalArea(true);
    	try
    	{
    		LogUtility.LogFile("entered", ModuleLogLevel);
    		if (!m_ProprietarySegmentTxInProgress)
    		{
    			LogUtility.LogFile("OnProprietarySegmentTransmitted: tx is not in progress, return", ModuleLogLevel);
    			LeaveProprietarySegmentTxCriticalArea();
    			return;
    		}
    		
    		if (Ret <= 0)
    		{
    			LogUtility.LogFile(m_Id.toString() + " error on EndSend " + Integer.toString(Ret), ModuleLogLevel);
    			LeaveProprietarySegmentTxCriticalArea();
    	    	ReStartAllOperations(false);
    	    	return;
    		}
    		LogUtility.LogFile(m_Id.toString() + " Transmitted to client " + Integer.toString(Ret), ModuleLogLevel);
    		_OnProprietarySegmentTransmitted(Ret);
    		m_ProprietarySegmentTxInProgress = false;
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentTransmitted", ModuleLogLevel);
        LeaveProprietarySegmentTxCriticalArea();
    	ReStartAllOperations(false);*/
    }
    protected void OnNonProprietarySegmentTransmitted(int sent)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnNonProprietarySegmentTransmitted", ModuleLogLevel);
        EnterNonProprietarySegmentTxCriticalArea(true);
        try
        {
            LogUtility.LogFile("entered",ModuleLogLevel);
            if (!m_NonProprietarySegmentTxInProgress)
            {
                LogUtility.LogFile("OnNonProprietarySegmentTransmitted: tx is not in progress,return", ModuleLogLevel);
                LeaveNonProprietarySegmentTxCriticalArea();
                return;
            }
            if (sent <= 0)
            {
                LogUtility.LogFile("!!!NonProprietary: transferred < 0", LogUtility.LogLevels.LEVEL_LOG_HIGH3);
            }
            m_TransmittedServer += (long)sent;
            LogUtility.LogFile(m_Id.toString() + " " + Long.toString(sent) + " sent to destination " + Long.toString(sent) + " overall " + Long.toString(m_TransmittedServer), ModuleLogLevel);
            OnDestinationTransmitted(sent);
            m_NonProprietarySegmentTxInProgress = false;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnNonProprietarySegmentTransmitted", ModuleLogLevel);
        LeaveNonProprietarySegmentTxCriticalArea();
        ReStartAllOperations(!m_OnceConnected);
    }
    boolean _ProprietarySegmentTransmit(byte[] buff2transmit)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering _ProprietarySegmentTransmit", ModuleLogLevel);
        try
        {
            LogUtility.LogFile(m_Id.toString() + " send (proprietary segment) " + Long.toString(buff2transmit.length), ModuleLogLevel);
            
            m_clientSideSocket.SendAsync(buff2transmit, 0, buff2transmit.length);
            m_ProprietarySegmentTxInProgress = true;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            //Dispose2();
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving _ProprietarySegmentTransmit", ModuleLogLevel);
        return false;
    }

    protected void ProprietarySegmentSubmitStream4Tx(byte[] data)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentSubmit4Tx", ModuleLogLevel);
        if (m_ErrorSent)
        {
            LogUtility.LogFile("discard, ErrorSent flag is up, leaving ProprietarySegmentSubmit4Tx", ModuleLogLevel);
            return;
        }
        if (data != null)
        {
            LogUtility.LogFile(m_Id.toString() + " Subm2Tx stream client queue", ModuleLogLevel);
            SubmitStream4ClientTx(data);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentSubmit4Tx", ModuleLogLevel);
    }
    protected void ProprietarySegmentSubmitMsg4Tx(byte[] data,boolean submit2Head)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
        if (m_ErrorSent)
        {
            LogUtility.LogFile("discard, ErrorSent flag is up, leaving ProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
            return;
        }
        if (data != null)
        {
            LogUtility.LogFile(m_Id.toString() + " Subm2Tx msg to client queue", ModuleLogLevel);
            SubmitMsg4ClientTx(data,submit2Head);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
    }
    protected void ProprietarySegmentTransmit()
    {
    	boolean IsRestartRequired = false;
        byte[] buff2transmit;
        MyMemoryStream stream = new MyMemoryStream();
        LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentTransmit", ModuleLogLevel);
        if (!EnterProprietarySegmentTxCriticalArea(false))
        {
            return;
        }
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (m_ProprietarySegmentTxInProgress)
            {
                LogUtility.LogFile("ProprietarySegmentTransmit: tx is in progress,return", ModuleLogLevel);
                LeaveProprietarySegmentTxCriticalArea();
                return;
            }
            while (true)
            {
                if (IsClientTxQueueEmpty())
                {
                    LogUtility.LogFile(m_Id.toString() + " queue is empty, exiting the loop", ModuleLogLevel);
                    break;
                }
                if (!m_txStateMachine.IsBusy())
                {
                	ReferencedLong length = new ReferencedLong();
                	ReferencedBoolean isMsg = new ReferencedBoolean();
                    byte[] buf2tx = GetClient2Transmit(length, isMsg);
                    if (buf2tx != null)
                    {
                        LogUtility.LogFile(m_Id.toString() + " tx to client  msg len " + Long.toString(length.val) + " isMsg " + Boolean.toString(isMsg.val), ModuleLogLevel);
                        if (isMsg.val)
                        {
                            m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_DOWNSTREAM_MSG_KIND.ordinal());
                        }
                        else
                        {
                            m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_DOWNSTREAM_DATA_KIND.ordinal());
                        }
                        m_txStateMachine.SetLength(length.val);
                        m_txStateMachine.SetMsgBody(buf2tx);
                    }
                    else
                    {
                        LogUtility.LogFile(m_Id.toString() + " queue is empty, exiting the loop", ModuleLogLevel);
                        break;
                    }
                }
                buff2transmit = m_txStateMachine.GetBytes();
                if (buff2transmit != null)
                {
                    //_ProprietarySegmentTransmit(buff2transmit);
                    stream.AddBytes(buff2transmit);
                    if (m_txStateMachine.IsInBody())
                    {
                        if (m_txStateMachine.IsWholeMessage())
                        {
                            OnClientTransmitted(buff2transmit.length - m_txStateMachine.GetHeaderLength());
                        }
                        else
                        {
                            OnClientTransmitted(buff2transmit.length);
                        }
                    }
                    m_txStateMachine.OnTxComplete((long)buff2transmit.length);
                    if (m_txStateMachine.IsTransactionCompleted())
                    {
                        m_txStateMachine.ClearMsgBody();
                        m_TransmittedMsgsClient++;
                    }
                }
                else
                {
                    LogUtility.LogFile("cannot get more bytes to tx, exiting the loop", ModuleLogLevel);
                    break;
                }
            }
            
            if (stream.Length() > 0)
            {
                IsRestartRequired = _ProprietarySegmentTransmit(stream.GetBytes());
            } 
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentTransmit", ModuleLogLevel);
        LeaveProprietarySegmentTxCriticalArea();
        /*if (IsRestartRequired)*/
        {
            ReStartAllOperations(!m_OnceConnected);
        }
    	/*boolean IsRestartRequired = false;
    	LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentTransmit", ModuleLogLevel);
    	if (!EnterProprietarySegmentTxCriticalArea(false))
    	{
    		return;
    	}
    	try
    	{
    		LogUtility.LogFile("entered", ModuleLogLevel);
    		if (m_ProprietarySegmentTxInProgress)
    		{
    			LogUtility.LogFile(m_Id.toString() + " tx is in progress, return", ModuleLogLevel);
    			LeaveProprietarySegmentTxCriticalArea();
    			return;
    		}
    		if(!m_txStateMachine.IsBusy())
    		{
    			ReferencedLong length = new ReferencedLong();
    			ReferencedBoolean isMsg = new ReferencedBoolean();
    			byte []data = GetClient2Transmit(length,isMsg);
    			//byte []data = DebugGetClient2Transmit(length,isMsg);
    			if (data == null)
    			{
    				LogUtility.LogFile(m_Id.toString() + " queue is empty", ModuleLogLevel);
    				LeaveProprietarySegmentTxCriticalArea();
    				return;
    			}
    			LogUtility.LogFile(m_Id.toString() + " initiating new msg len " + Integer.toString(data.length) + " " + Boolean.toString(isMsg.val), ModuleLogLevel);
    			if (isMsg.val)
    			{
    				m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_DOWNSTREAM_MSG_KIND.ordinal());
    			}
    			else
    			{
    				m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_DOWNSTREAM_DATA_KIND.ordinal());
    			}
    			m_txStateMachine.SetLength(length.val);
    			m_txStateMachine.SetMsgBody(data);
    		}

    		byte[] buff2transmit = m_txStateMachine.GetBytes();
    		if (buff2transmit != null)
    		{
    			IsRestartRequired = _ProprietarySegmentTransmit(buff2transmit);
    		}
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentTransmit", ModuleLogLevel);
    	LeaveProprietarySegmentTxCriticalArea();
    	CheckConnectionAndShutDownIfGone();
    	if (IsRestartRequired)
    	{
    		ReStartAllOperations(false);
    	}*/
    }
    public boolean IsClientTxQueueEmpty()
    {
        return ((!m_txStateMachine.IsBusy()) && super.IsClientTxQueueEmpty());
    }
    void DebugCopyBytesFromOffset(byte[] src,byte[] dst,  int dst_offset,int Count)
    {
        for (int i = 0; i < Count; i++)
        {
            dst[dst_offset+i] = src[i];
        }
    }
    protected void OnNonProprietarySegmentReceived(int Received)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnNonProprietarySegmentReceived", ModuleLogLevel);
        EnterNonProprietarySegmentRxCriticalArea(true);
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (!m_NonProprietarySegmentRxInProgress)
            {
                LogUtility.LogFile("OnNonProprietarySegmentReceived: rx is not in progress,return", ModuleLogLevel);
                LeaveNonProprietarySegmentRxCriticalArea();
                return;
            }
            if (Received <= 0)
            {
                LogUtility.LogFile(m_Id.toString() + " Rx ERROR ", ModuleLogLevel);
                LeaveNonProprietarySegmentRxCriticalArea();
                ReStartAllOperations(!m_OnceConnected);
                return;
            }
            m_ReceivedServer += (long)Received;
            m_ReceivedMsgs++;
            LogUtility.LogFile(m_Id.toString() + " Received (non-proprietary segment) " + Long.toString(Received) + " overall " + Long.toString(m_ReceivedServer), ModuleLogLevel);

            byte[] buff = new byte[Received];
            CopyBytes(m_NonProprietarySegmentRxBuf, buff, Received);
            /*{
            	DebugWriteStream2File(true,buff,0,buff.length);
            }*/
            ProcessDownStreamData(buff,false);
            /*{
            	ReferencedInteger offset = new ReferencedInteger();
                byte []msg = PackMsg.AllocateMsgAndBuildHeader((long)buff.length, (byte)0, (byte)PackMsg.MsgKind_e.PACK_DATA_MSG_KIND.ordinal(), offset);
                DebugCopyBytesFromOffset(buff,msg,offset.val,(int)buff.length);
                ProprietarySegmentSubmitMsg4Tx(msg,false);
            }*/
            m_NonProprietarySegmentRxInProgress = false;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnNonProprietarySegmentReceived", ModuleLogLevel);
        LeaveNonProprietarySegmentRxCriticalArea();
        ReStartAllOperations(!m_OnceConnected);
    }
    protected void NonProprietarySegmentReceive()
    {
        boolean IsRestartRequired = false;
        LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentReceive", ModuleLogLevel);
        if (!EnterNonProprietarySegmentRxCriticalArea(false))
        {
            return;
        }
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if ((m_destinationSideSocket == null) || (!m_destinationSideSocket.isConnected()))
            {
                LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentReceive (socket is not connected)", ModuleLogLevel);
                LeaveNonProprietarySegmentRxCriticalArea();
                return;
            }
            
            if (m_NonProprietarySegmentRxInProgress)
            {
                LogUtility.LogFile("NonProprietarySegmentReceive: rx is in progess,return", ModuleLogLevel);
                LeaveNonProprietarySegmentRxCriticalArea();
                return;
            }
            LogUtility.LogFile(m_Id.toString() + " NonProprietary ReceivAsync ", ModuleLogLevel);
            if(m_destinationSideSocket.ReceiveAsync(m_NonProprietarySegmentRxBuf, 0, m_NonProprietarySegmentRxBuf.length) != 0)
            {
            	LogUtility.LogFile("ReceiveAsync is failed", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            }
            m_NonProprietarySegmentRxInProgress = true;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentReceive", ModuleLogLevel);
        LeaveNonProprietarySegmentRxCriticalArea();
        /*if (IsRestartRequired)*/
        {
            ReStartAllOperations(!m_OnceConnected);
        }
    }
    boolean _NonProprietarySegmentTransmit(byte []data)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering _NonProprietarySegmentTransmit", ModuleLogLevel);
        try
        {
            LogUtility.LogFile(m_Id.toString() + " Sending to destination " + Long.toString(data.length), ModuleLogLevel);
            m_destinationSideSocket.SendAsync(data, 0, data.length);
            m_NonProprietarySegmentTxInProgress = true;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving _NonProprietarySegmentTransmit", ModuleLogLevel);
        return false;
    }
    protected void NonProprietarySegmentSubmitStream4Tx(byte[] data)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentSubmit4Tx", ModuleLogLevel);
        if (data != null)
        {
            LogUtility.LogFile(m_Id.toString() + " Subm2Tx client queue", ModuleLogLevel);
            SubmitStream4DestinationTx(data);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentSubmit4Tx", ModuleLogLevel);
    }
    protected void NonProprietarySegmentSubmitMsg4Tx(byte[] data)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
        if (data != null)
        {
            LogUtility.LogFile(m_Id.toString() + " Subm2Tx client queue", ModuleLogLevel);
            SubmitMsg4DestinationTx(data);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
    }
    protected void NonProprietarySegmentTransmit()
    {
        boolean IsRestartRequired = false;
        LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentTransmit", ModuleLogLevel);
        if (!EnterNonProprietarySegmentTxCriticalArea(false))
        {
            return;
        }
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if ((m_destinationSideSocket == null) || (!m_destinationSideSocket.isConnected()))
            {
                LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentTransmit (socket is not connected)", ModuleLogLevel);
                LeaveNonProprietarySegmentTxCriticalArea();
                return;
            }
            if (!m_NonProprietarySegmentTxInProgress)
            {
                LogUtility.LogFile(m_Id.toString() + " is not in tx", ModuleLogLevel);
                ReferencedLong length = new ReferencedLong();
                ReferencedBoolean isMsg = new ReferencedBoolean();
                byte []data  = GetDestination2Transmit(length,isMsg);
                if (data != null)
                {
                    IsRestartRequired = _NonProprietarySegmentTransmit(data);
                }
            }
            else
            {
                LogUtility.LogFile(m_Id.toString() + " DestinationTx is busy and queueElement is null", ModuleLogLevel);
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentTransmit", ModuleLogLevel);
        LeaveNonProprietarySegmentTxCriticalArea();
        /*if (IsRestartRequired)*/
        {
            ReStartAllOperations(!m_OnceConnected);
        }
    }
    public abstract byte []GetFirstBuffToTransmitDestination();
    public abstract void ProcessUpStreamDataKind();
    public abstract void ProcessUpStreamMsgKind();

    public void OnDownStreamTransmissionOpportunity()
    {
    }

    public void OnProprietarySegmentMsgReceived()
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnProprietaryMsgReceived", ModuleLogLevel);
        try
        {
            LogUtility.LogFile("Received msg type " + Long.toString(m_rxStateMachine.GetKind()), ModuleLogLevel);
            if (m_rxStateMachine.GetMsgBody() == null)
            {
                LogUtility.LogFile("msg body is null!!!", ModuleLogLevel);
                return;
            }
            switch (PackEnvelopeKinds.values()[m_rxStateMachine.GetKind()])
            {
                case PACK_ENVELOPE_UPSTREAM_DATA_KIND:
                    ProcessUpStreamDataKind();
                    break;
                case PACK_ENVELOPE_UPSTREAM_MSG_KIND:
                    ProcessUpStreamMsgKind();
                    OnDownStreamTransmissionOpportunity();
                    break;
            }
            m_rxStateMachine.ClearMsgBody();
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietaryMsgReceived", ModuleLogLevel);
    }

    protected void OnProprietarySegmentReceived(int Received)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentReceived", ModuleLogLevel);
        EnterProprietarySegmentRxCriticalArea(true);
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (!m_ProprietarySegmentRxInProgress)
            {
                LogUtility.LogFile("OnProprietarySegmentReceived: rx is not in progress,return", ModuleLogLevel);
                LeaveProprietarySegmentRxCriticalArea();
                return;
            }
            if (Received <= 0)
            {
                LogUtility.LogFile(" Rx ERROR, ", ModuleLogLevel);
                LeaveProprietarySegmentRxCriticalArea();
                Dispose();
                return;
            }
            LogUtility.LogFile("Received (proprietary segment) " + Long.toString(Received), ModuleLogLevel);
            m_ReceivedClient += (long)Received;
            m_rxStateMachine.OnRxComplete(m_ProprietarySementRxBuf, Received);
            m_ProprietarySegmentRxInProgress = false;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + " EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return;
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentReceived", ModuleLogLevel);
        LeaveProprietarySegmentRxCriticalArea();
        ReStartAllOperations(!m_OnceConnected);
    }
    protected void ProprietarySegmentReceive()
    {
        boolean IsRestartRequired = false;
        LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentReceive", ModuleLogLevel);
        if (!EnterProprietarySegmentRxCriticalArea(false))
        {
            return;
        }
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (m_ProprietarySegmentRxInProgress)
            {
                LogUtility.LogFile("ProprietarySegmentReceive: rx is in progress,return", ModuleLogLevel);
                LeaveProprietarySegmentRxCriticalArea();
                return;
            }
            LogUtility.LogFile(m_Id.toString() + " Proprietary ReceivAsync ", ModuleLogLevel);
            if(m_clientSideSocket.ReceiveAsync(m_ProprietarySementRxBuf, 0, m_ProprietarySementRxBuf.length) != 0)
            {
            	LogUtility.LogFile("ReceiveAsync is failed", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            }
            m_ProprietarySegmentRxInProgress = true;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + "EXCEPTION ", exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentReceive", ModuleLogLevel);
        LeaveProprietarySegmentRxCriticalArea();
        /*if (IsRestartRequired)*/
        {
            ReStartAllOperations(!m_OnceConnected);
        }
    }
    protected boolean ClientTxInProgress()
    {
        return m_ProprietarySegmentTxInProgress;
    }
    public void Start()
    {
        try
        {
            LogUtility.LogFile(m_Id.toString() + " Starting new server", LogUtility.LogLevels.LEVEL_LOG_HIGH);
            m_rxStateMachine.SetCallback(this);
            ProprietarySegmentReceive();
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString() + "EXCEPTION ", exc,LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
}
