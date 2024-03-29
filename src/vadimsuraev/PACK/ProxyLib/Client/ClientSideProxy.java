﻿package vadimsuraev.PACK.ProxyLib.Client;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.ProxyLib.IOnGotResults;
import vadimsuraev.PACK.ProxyLib.Proxy;
import vadimsuraev.PACK.ProxyLib.ProxySocket;
import vadimsuraev.PACK.ProxyLib.RxTxStateMachine.OnMessageCallback;

import  vadimsuraev.PACK.ProxyLib.ProxyLibTypes.PackEnvelopeKinds;
import vadimsuraev.ReferencedTypes.ReferencedBoolean;
import vadimsuraev.ReferencedTypes.ReferencedLong;

public class ClientSideProxy extends Proxy implements OnMessageCallback
{
	InetSocketAddress m_destinationSideEndPoint;
        
    public ClientSideProxy(SocketChannel clientSocket, InetSocketAddress remoteEndPoint,IOnGotResults onGotResults)
    {
    	super(onGotResults);
        try
        {
            m_clientSideSocket = new ProxySocket("ClientNonProp",clientSocket,false,this);
            m_destinationSideSocket = new ProxySocket("ClientProp",true,this);
            m_destinationSideEndPoint = remoteEndPoint;
            m_destinationSideSocket.Connect(m_destinationSideEndPoint);
            m_Id = /*m_destinationSideSocket.GetlocalEndPoint()*/remoteEndPoint;
            m_rxStateMachine.SetCallback(this);
            m_rxStateMachine.SetEndPoint(clientSocket.socket().getLocalSocketAddress());
            m_txStateMachine.SetEndPoint(m_destinationSideEndPoint);
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }      
        
    protected void NonProprietarySegmentSubmitStream4Tx(byte []data)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentSubmitStream4Tx", ModuleLogLevel);

        if (data != null)
        {
            LogUtility.LogFile("Submit stream to client queue " + Integer.toString(data.length), ModuleLogLevel);
            SubmitStream4ClientTx(data);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentSubmitStream4Tx", ModuleLogLevel);
    }
    protected void NonProprietarySegmentTransmit()
    {
    	if((m_clientSideSocket == null)||(!m_clientSideSocket.isConnected()))
    	{
    		LogUtility.LogFile(m_Id.toString() + " NonProprietarySegmentTransmit: socket is disconnected. return", ModuleLogLevel);
    		return;
    	}
        LogUtility.LogFile(m_Id.toString() + " Entering NonProprietarySegmentTransmit tx client " + Long.toString(m_TransmittedClient) + " rx server " + Long.toString(m_ReceivedServer), ModuleLogLevel);
        if (!EnterNonProprietarySegmentTxCriticalArea(false))
        {
            return;
        }

        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (m_NonProprietarySegmentTxInProgress)
            {
                LogUtility.LogFile("NonProprietarySegmentTransmit: tx is in progress,return", ModuleLogLevel);
                LeaveNonProprietarySegmentTxCriticalArea();
                return;
            }

            if (IsClientTxQueueEmpty())
            {
                LogUtility.LogFile(m_Id.toString() + " cannot get from the queue", ModuleLogLevel);
                LeaveNonProprietarySegmentTxCriticalArea();
                return;
            }
            LogUtility.LogFile(m_Id.toString() + " Sending (non-proprietary segment) " + Long.toString(m_clientStream.Length()), ModuleLogLevel);
                
            ReferencedLong length = new ReferencedLong();
            ReferencedBoolean isMsg = new ReferencedBoolean();
            byte[] buff = (byte [])GetClient2Transmit(length,isMsg);
            if (buff != null)
            {
                m_clientSideSocket.SendAsync(buff, 0, buff.length);
                m_NonProprietarySegmentTxInProgress = true;
            }
        }
        catch (Exception exc)
        {
             LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
             m_NonProprietarySegmentTxInProgress = false;
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving NonProprietarySegmentTransmit", ModuleLogLevel);
        LeaveNonProprietarySegmentTxCriticalArea();
    }
    public void OnMsgReceived()
    {
    	OnProprietarySegmentMsgReceived();
    }
    protected void OnProprietarySegmentMsgReceived()
    {
         LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentMsgReceived", ModuleLogLevel);
         try
         {
             LogUtility.LogFile(m_Id.toString() + " Received message type " + Integer.toString(m_rxStateMachine.GetKind()), ModuleLogLevel);
            if (m_rxStateMachine.GetMsgBody() == null)
            {
                LogUtility.LogFile(m_Id.toString() + " msg body is null", ModuleLogLevel);
                return;
            }
            m_ReceivedMsgs++;
            switch (PackEnvelopeKinds.values()[m_rxStateMachine.GetKind()])
            {
                case PACK_ENVELOPE_DOWNSTREAM_DATA_KIND:
                    NonProprietarySegmentSubmitStream4Tx(m_rxStateMachine.GetMsgBody());
                    //NonProprietarySegmentTransmit();
                    m_rxStateMachine.ClearMsgBody();
                    break;
            }
        }
        catch (Exception exc)
        {
             LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentMsgReceived", ModuleLogLevel);
    }
    protected  void OnProprietarySegmentReceived(int Received)
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentReceived", ModuleLogLevel);
        EnterProprietarySegmentRxCriticalArea(true);
        try
        {
            LogUtility.LogFile("entered", ModuleLogLevel);
            if (!m_ProprietarySegmentRxInProgress)
            {
                LogUtility.LogFile("OnProprietarySegmentReceived: rx is not in progress", ModuleLogLevel);
                LeaveProprietarySegmentRxCriticalArea();
                return;
            }
            if (Received <= 0)
            {
                LogUtility.LogFile(m_Id.toString() + " Received (proprietary segment) ERROR ", ModuleLogLevel);
                m_ProprietarySegmentRxInProgress = false;
                LeaveProprietarySegmentRxCriticalArea();
                ReStartAllOperations(!m_OnceConnected);
                return;
            }
            LogUtility.LogFile(m_Id.toString() + " Received (proprietary segment) " + Integer.toString(Received), ModuleLogLevel);
            m_ReceivedServer += (long)Received;
            m_rxStateMachine.OnRxComplete(m_ProprietarySementRxBuf, Received);
            m_ProprietarySegmentRxInProgress = false;
        }
        catch (Exception exc)
        {
            LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return;
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentReceived", ModuleLogLevel);
        LeaveProprietarySegmentRxCriticalArea();
        ReStartAllOperations(false);
    }

    protected  void ProprietarySegmentReceive()
    {
    	if((m_destinationSideSocket == null)||(!m_destinationSideSocket.isConnected()))
    	{
    		LogUtility.LogFile(m_Id.toString() + " ProprietarySegmentReceive: socket is disconnected. return", ModuleLogLevel);
    		return;
    	}
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
            m_destinationSideSocket.ReceiveAsync(m_ProprietarySementRxBuf, 0, m_ProprietarySementRxBuf.length);
            m_ProprietarySegmentRxInProgress = true;
        }
        catch (Exception exc)
        {
        	LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentReceive", ModuleLogLevel);
        LeaveProprietarySegmentRxCriticalArea();
/*        if (IsRestartRequired)*/
        {
        	ReStartAllOperations(!m_OnceConnected);
        }
    }
    protected void _OnProprietarySegmentTransmitted(int transmitted)
    {
    	if (m_txStateMachine.IsInBody())
    	{
    		if (m_txStateMachine.IsWholeMessage())
    		{
    			OnDestinationTransmitted(transmitted - m_txStateMachine.GetHeaderLength());
    		}
    		else
    		{
    			OnDestinationTransmitted(transmitted);
    		}
    	}
    	m_TransmittedServer += (long)transmitted;
    	LogUtility.LogFile(m_Id.toString() + " sent (proprietary segment) " + Integer.toString(transmitted), ModuleLogLevel);
    	m_txStateMachine.OnTxComplete((long)transmitted);
    	if (m_txStateMachine.IsTransactionCompleted())
    	{
    		m_txStateMachine.ClearMsgBody();
    	}
    }
    protected  void OnProprietarySegmentTransmitted(int Ret)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentTransmitted", ModuleLogLevel);
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
    	    	ReStartAllOperations(!m_OnceConnected);
    	    	return;
    		}
    		LogUtility.LogFile(m_Id.toString() + " Transmitted to server " + Integer.toString(Ret), ModuleLogLevel);
    		_OnProprietarySegmentTransmitted(Ret);
    		m_ProprietarySegmentTxInProgress = false;
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentTransmitted", ModuleLogLevel);
        LeaveProprietarySegmentTxCriticalArea();
    	ReStartAllOperations(false);
    }
    protected boolean _ProprietarySegmentTransmit(byte []buff2transmit)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering _ProprietarySegmentTransmit", ModuleLogLevel);
    	try
    	{
    		LogUtility.LogFile(m_Id.toString() + " Trying to send to destination " + Integer.toString(buff2transmit.length), ModuleLogLevel);
    		m_destinationSideSocket.SendAsync(buff2transmit, 0, buff2transmit.length);
    		m_ProprietarySegmentTxInProgress = true;
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving _ProprietarySegmentTransmit", ModuleLogLevel);
    	return false;
    }

    protected  void OnNonProprietarySegmentTransmitted(int sent)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering OnNonProprietarySegmentTransmitted", ModuleLogLevel);
    	EnterNonProprietarySegmentTxCriticalArea(true);
    	try
    	{
    		LogUtility.LogFile("entered", ModuleLogLevel);
    		if (!m_NonProprietarySegmentTxInProgress)
    		{
    			LogUtility.LogFile("OnNonProprietarySegmentTransmitted: tx is not in progress, return", ModuleLogLevel);
    			LeaveNonProprietarySegmentTxCriticalArea();
    			return;
    		}
    		
    		if (sent < 0)
    		{
    			LogUtility.LogFile(m_Id.toString() + " OnNonProprietarySegmentTransmitted ", LogUtility.LogLevels.LEVEL_LOG_HIGH3);
    			return;
    		}
    		m_TransmittedClient += (long)sent;
    		LogUtility.LogFile(m_Id.toString() + " " + Integer.toString(sent) + " sent to client overall " + Long.toString(m_TransmittedClient), ModuleLogLevel);
    		m_NonProprietarySegmentTxInProgress = false;
    		OnClientTransmitted(sent);
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    		LeaveNonProprietarySegmentTxCriticalArea();
    		return;
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving OnNonProprietarySegmentTransmitted", ModuleLogLevel);
    	LeaveNonProprietarySegmentTxCriticalArea();
    	ReStartAllOperations(!m_OnceConnected);
    }
    protected void ProprietarySegmentSubmitMsg4Tx(byte[] data)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);

    	if (data != null)
    	{
    		LogUtility.LogFile("submit msg to dest " + Integer.toString(data.length), ModuleLogLevel);
    		SubmitMsg4DestinationTx(data);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentSubmitMsg4Tx", ModuleLogLevel);
    }
    protected void ProprietarySegmentSubmitStream4Tx(byte[] data)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering ProprietarySegmentSubmitStream4Tx", ModuleLogLevel);
    	if (data != null)
    	{
    		LogUtility.LogFile("submit stream for tx to dest " + Integer.toString(data.length), ModuleLogLevel);
    		SubmitStream4DestinationTx(data);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving ProprietarySegmentSubmitStream4Tx", ModuleLogLevel);
    }
    protected  void ProprietarySegmentTransmit()
    {
    	if((m_destinationSideSocket == null)||(!m_destinationSideSocket.isConnected()))
    	{
    		LogUtility.LogFile(m_Id.toString() + " ProprietarySegmentTransmit: socket is disconnected. return", ModuleLogLevel);
    		return;
    	}
    	boolean IsRestartRequired = false;
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
    			byte []data = GetDestination2Transmit(length,isMsg);
    			if (data == null)
    			{
    				LogUtility.LogFile(m_Id.toString() + " queue is empty", ModuleLogLevel);
    				LeaveProprietarySegmentTxCriticalArea();
    				return;
    			}
    			LogUtility.LogFile(m_Id.toString() + " initiating new msg len " + Integer.toString(data.length) + " " + Boolean.toString(isMsg.val), ModuleLogLevel);
    			if (isMsg.val)
    			{
    				m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_UPSTREAM_MSG_KIND.ordinal());
    			}
    			else
    			{
    				m_txStateMachine.SetKind((byte)PackEnvelopeKinds.PACK_ENVELOPE_UPSTREAM_DATA_KIND.ordinal());
    			}
    			m_txStateMachine.SetLength(length.val);
    			m_txStateMachine.SetMsgBody(data);
    		}

    		byte[] buff2transmit = m_txStateMachine.GetBytes(/*8192*/);
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
    	/*if (IsRestartRequired)*/
    	{
    		ReStartAllOperations(!m_OnceConnected);
    	}
    }
    protected boolean ClientTxInProgress()
    {
    	return m_NonProprietarySegmentTxInProgress;
    }
    protected  void OnNonProprietarySegmentReceived(int Received)
    {
    	LogUtility.LogFile(m_Id.toString() + " Entering OnNonProprietarySegmentReceived", ModuleLogLevel);
    	EnterNonProprietarySegmentRxCriticalArea(true);
    	try
    	{
    		LogUtility.LogFile("entered", ModuleLogLevel);
    		if (!m_NonProprietarySegmentRxInProgress)
    		{
    			LogUtility.LogFile("OnNonProprietarySegmentReceived: exiting, rx is not in progress", ModuleLogLevel);
    			LeaveNonProprietarySegmentRxCriticalArea();
    			return;
    		}
    		
    		if (Received <= 0)
    		{
    			LogUtility.LogFile(m_Id.toString() + " OnClientReceive error ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    			m_NonProprietarySegmentRxInProgress = false;
    			LeaveNonProprietarySegmentRxCriticalArea();
    			//Dispose();
    			return;
    		}
    		m_ReceivedClient += (long)Received;
    		LogUtility.LogFile(m_Id.toString() + " Received on non-proprietary segment " + Integer.toString(Received) + " overall " + Long.toString(m_ReceivedClient), ModuleLogLevel);
    		byte[] data = new byte[Received];
    		CopyBytes(m_NonProprietarySegmentRxBuf, data, Received);
    		ProprietarySegmentSubmitStream4Tx(data);
    		//ProprietarySegmentTransmit();
    		m_NonProprietarySegmentRxInProgress = false;
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving OnNonProprietarySegmentReceived", ModuleLogLevel);
    	LeaveNonProprietarySegmentRxCriticalArea();
    	ReStartAllOperations(!m_OnceConnected);
    }
    protected void NonProprietarySegmentReceive()
    {
    	if((m_clientSideSocket == null)||(!m_clientSideSocket.isConnected()))
    	{
    		LogUtility.LogFile(m_Id.toString() + " NonProprietarySegmentReceive: socket is disconnected. return", ModuleLogLevel);
    		return;
    	}
    	boolean IsRestartRequired = false;
    	LogUtility.LogFile(m_Id.toString() + " Entering ReceiveNonProprietarySegment", ModuleLogLevel);
    	if (!EnterNonProprietarySegmentRxCriticalArea(false))
    	{
    		return;
    	}
    	try
    	{
    		LogUtility.LogFile("entered", ModuleLogLevel);
    		if (m_NonProprietarySegmentRxInProgress)
    		{
    			LogUtility.LogFile("ReceiveNonProprietarySegment: exiting, rx is in progress", ModuleLogLevel);
    			LeaveNonProprietarySegmentRxCriticalArea();
    			return;
    		}
    		LogUtility.LogFile(m_Id.toString() + " NonProprietary ReceivAsync ", ModuleLogLevel);
    		m_clientSideSocket.ReceiveAsync(m_NonProprietarySegmentRxBuf, 0, m_NonProprietarySegmentRxBuf.length);
    		m_NonProprietarySegmentRxInProgress = true;
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    	LogUtility.LogFile(m_Id.toString() + " Leaving ReceiveNonProprietarySegment", ModuleLogLevel);
    	LeaveNonProprietarySegmentRxCriticalArea();
    	/*if (IsRestartRequired)*/
    	{
    		ReStartAllOperations(!m_OnceConnected);
    	}
    }
    protected  void Disposing()
    {
    }
    public  void Start()
    {
    	try
    	{
    		LogUtility.LogFile(m_Id.toString() + " Starting new client", LogUtility.LogLevels.LEVEL_LOG_HIGH);
    		NonProprietarySegmentReceive();
    	}
    	catch (Exception exc)
    	{
    		LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
    	}
    }

	@Override
	public void OnRead(Object data, int count) {
		Boolean isPropSeg = (Boolean)data;
		if(isPropSeg)
		{
			OnProprietarySegmentReceived(count);
		}
		else
		{
			OnNonProprietarySegmentReceived(count);
		}
	}

	@Override
	public void OnWritten(Object data, int count) {
		Boolean isPropSeg = (Boolean)data;
		if(isPropSeg)
		{
			OnProprietarySegmentTransmitted(count);
		}
		else
		{
			OnNonProprietarySegmentTransmitted(count);
		}
	}

	@Override
	public void OnConnectionBroken(Object data) {
		Boolean isProp = (Boolean)data;
		if(isProp)
		{
			OnDestinationDisconnected();
		}
		else
		{
			OnClientDisconnected();
		}
	}

	@Override
	public void OnConnected() {
		m_OnceConnected = true;
	}
}
