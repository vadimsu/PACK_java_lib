package vadimsuraev.PACK.ProxyLib.Client;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.ProxyLib.IOnGotResults;
import vadimsuraev.PACK.ProxyLib.ProxyLibTypes.PackEnvelopeKinds;
import vadimsuraev.PACK.ReceiverPackLib.*;


public class PackClientSide extends ClientSideProxy implements ClientSideCallbacks
{
	static boolean m_GlobalsInitiated = false;
    ReceiverPackLib m_receiverPackLib;

    public static void InitGlobalObjects()
    {
    	if(m_GlobalsInitiated)
    	{
    		return;
    	}
    	m_GlobalsInitiated = true;
        LogUtility.LogFile("PackClientSide:InitGlobalObjects", ModuleLogLevel);
        ReceiverPackLib.InitGlobalObjects();
    }

    public PackClientSide(SocketChannel clientSocket, InetSocketAddress remoteEndPoint,IOnGotResults onGotResults)
    {
        super(clientSocket,remoteEndPoint,onGotResults);
        InitGlobalObjects();
        m_receiverPackLib = new ReceiverPackLib(m_Id,this);
    }
    
    public void OnMsgRead4Tx(byte[] msg,boolean dummy)
    {
        LogUtility.LogFile("Entering OnMsgReady4Tx", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        ProprietarySegmentSubmitMsg4Tx(msg);
        //ProprietarySegmentTransmit();
        LogUtility.LogFile("Leaving OnMsgReady4Tx", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    }
    public void OnDataReceived(byte[] data,int offset,int length)
    {
        LogUtility.LogFile("Entering OnDataReceived", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        try
        {
            //LogUtility.LogUtility.LogFile(ASCIIEncoding.ASCII.GetString(data),LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        catch (Exception exc)
        {
            //LogUtility.LogUtility.LogFile(Integer.toString(Id) + " EXCEPTION " + exc.Message + " " + exc.StackTrace, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        byte[] buff = new byte[length];
        for (int i = 0; i < length; i++)
        {
            buff[i] = data[offset + i];
        }
        //LogUtility.LogUtility.LogBinary("_total", buff);
        NonProprietarySegmentSubmitStream4Tx(/*data*/buff);
        NonProprietarySegmentTransmit();
        LogUtility.LogFile("Leaving OnDataReceived", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    }
    protected long GetSaved()
    {
        if (m_receiverPackLib != null)
        {
            return m_receiverPackLib.GetTotalDataSaved();
        }
        return super.GetSaved();
    }
    protected void OnProprietarySegmentMsgReceived()
    {
        LogUtility.LogFile(m_Id.toString() + " Entering OnProprietarySegmentMsgReceived", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        try
        {
            LogUtility.LogFile(m_Id.toString() + " Received message type " + Byte.toString(m_rxStateMachine.GetKind()), ModuleLogLevel);
            if (m_rxStateMachine.GetMsgBody() == null)
            {
                 LogUtility.LogFile(m_Id.toString() + " msg body is null", ModuleLogLevel);
                 return;
            }
            m_ReceivedMsgs++;
            switch (PackEnvelopeKinds.values()[m_rxStateMachine.GetKind()])
            {
                case PACK_ENVELOPE_DOWNSTREAM_DATA_KIND:
                    //receiverPackLib.OnDataByteMode(rxStateMachine.GetMsgBody(), 0);
                    //rxStateMachine.ClearMsgBody();
                    LogUtility.LogFile("SHOULD NOT OCCUR!!!!!!!!!!!!", LogUtility.LogLevels.LEVEL_LOG_HIGH);
                    break;
                case PACK_ENVELOPE_DOWNSTREAM_MSG_KIND:
                    EnterProprietaryLibCriticalArea();
                    try
                    {
                        m_receiverPackLib.OnDataByteMode(m_rxStateMachine.GetMsgBody(), 0);
                        LeaveProprietaryLibCriticalArea();
                    }
                    catch (Exception exc)
                    {
                        LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
                        LeaveProprietaryLibCriticalArea();
                    }
                    m_rxStateMachine.ClearMsgBody();
                    break;
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            try {
				throw new Exception("Exception in OnProprietarySegmentMsgReceived", exc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving OnProprietarySegmentMsgReceived", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    }
    String GenerateDebugInfo()
    {
        boolean NonProrietaryTxMutexAvailable = NonProprietaryTxMutexAvailable();
        boolean ProrietaryTxMutexAvailable = ProprietaryTxMutexAvailable();
        boolean NonProrietaryRxMutexAvailable = NonProprietaryRxMutexAvailable();
        boolean ProrietaryRxMutexAvailable = NonProprietaryRxMutexAvailable();
        boolean clientMutexAvailable = ClientMutexAvailable();
        boolean destinationMutexAvailable = DestinationMutexAvailable();
        String debufInfo = m_Id.toString() + " Received client " + Long.toString(m_ReceivedClient) + " Transmitted client " + Long.toString(m_TransmittedClient) + " m_NonProprietarySegmentRxInProgress " + Boolean.toString(m_NonProprietarySegmentRxInProgress) +
            " m_NonProprietarySegmentTxInProgress " + Boolean.toString(m_NonProprietarySegmentTxInProgress) +
            " m_ProprietarySegmentRxInProgress " + Boolean.toString(m_ProprietarySegmentRxInProgress) +
            " m_ProprietarySegmentTxInProgress " + Boolean.toString(m_ProprietarySegmentTxInProgress) +
            " NonProrietaryTxMutexAvailable " + Boolean.toString(NonProrietaryTxMutexAvailable) +
            " ProrietaryTxMutexAvailable " + Boolean.toString(ProrietaryTxMutexAvailable) +
            " NonProrietaryRxMutexAvailable " + Boolean.toString(NonProrietaryRxMutexAvailable) +
            " ProrietaryRxMutexAvailable " + Boolean.toString(ProrietaryRxMutexAvailable) +
            " clientMutexAvailable " + Boolean.toString(clientMutexAvailable) +
            " destinationMutexAvailable " + Boolean.toString(destinationMutexAvailable) +
            m_rxStateMachine.GetDebugInfo() + " " + m_txStateMachine.GetDebugInfo() + m_receiverPackLib.GetDebugInfo();
        return debufInfo;
    }
    public Object GetResults()
    {
    	Object[] res = new Object[3];
        res[0] = m_receiverPackLib.GetTotalData();
        res[1] = m_receiverPackLib.GetTotalDataSaved();          
        res[2] = GenerateDebugInfo();
        return res;
    }
    protected void Disposing()
    {
    	try
    	{
    	    Object[] res = new Object[3];
            res[0] = m_receiverPackLib.GetTotalData();
            res[1] = m_receiverPackLib.GetTotalDataSaved();
            res[2] = GenerateDebugInfo();
            super.OnGotResults(res);
            m_receiverPackLib.OnDispose();
            super.Disposing();
    	}
    	catch(Exception e)
    	{
    		;
    	}
    }
    public static void Flush()
    {
        ReceiverPackLib.Flush();
    }

	@Override
	public void OnEnd() {
		// TODO Auto-generated method stub
		
	}
}
