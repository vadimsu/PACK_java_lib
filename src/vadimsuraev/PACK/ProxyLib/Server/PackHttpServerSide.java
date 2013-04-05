package vadimsuraev.PACK.ProxyLib.Server;

import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.senderPackLib.OnMsgReady4Tx;
import vadimsuraev.PACK.senderPackLib.SenderPackLib;

public class PackHttpServerSide extends HttpServerSideProxy implements OnMsgReady4Tx
{
    SenderPackLib m_senderPackLib;
    public PackHttpServerSide(SocketChannel sock)
    {
    	super(sock);
        m_senderPackLib = new SenderPackLib(m_Id,this);
    }
    protected long GetSaved()
    {
        if (m_senderPackLib != null)
        {
            return m_senderPackLib.GetTotalSavedData();
        }
        return super.GetSaved();
    }
    public void OnDownStreamTransmissionOpportunity()
    {
    }
    
    public void ProcessUpStreamMsgKind()
    {
        LogUtility.LogFile("Entering ProcessUpStreamMsgKind", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        try
        {
            EnterProprietaryLibCriticalArea();
            try
            {
                m_senderPackLib.OnDataByteMode(m_rxStateMachine.GetMsgBody(), 0);
                LeaveProprietaryLibCriticalArea();
            }
            catch (Exception exc)
            {
                LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                LeaveProprietaryLibCriticalArea();
            }
            //ProprietarySegmentSubmitStream4Tx(data);
            //ProprietarySegmentTransmit();
            LogUtility.LogFile("Leaving ProcessUpStreamMsgKind", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
    protected void OnBeginShutdown()
    {
        LogUtility.LogFile("Entering OnBeginShutdown", ModuleLogLevel);
        Flush();
        LogUtility.LogFile("Leaving OnBeginShutdown", ModuleLogLevel);
    }
    public boolean ProcessDownStreamData(byte[] data, boolean isInvokedOnTransmit)
    {
        boolean ret = false;
        LogUtility.LogFile("Entering ProcessDownStreamData", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        try
        {
            EnterProprietaryLibCriticalArea();
            ret = m_senderPackLib.AddData(data, isInvokedOnTransmit);
        }
        catch (Exception exc)
        {
            LogUtility.LogFile("EXCEPTION: " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile("Leaving ProcessDownStreamData", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        LeaveProprietaryLibCriticalArea();
        return ret;
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
            m_rxStateMachine.GetDebugInfo() + " " + m_txStateMachine.GetDebugInfo() + m_senderPackLib.GetDebugInfo();
        if (m_destinationSideSocket == null)
        {
            debufInfo += " destination socket is null ";
        }
        else
        {
            debufInfo += " destination socket Connected " + Boolean.toString(m_destinationSideSocket.isConnected());
        }
        return debufInfo;
    }
    public Object GetResults()
    {
    	Object[] res = new Object[5];
        res[0] = GetRequestPath();
        res[1] = m_senderPackLib.GetTotalAdded();
        res[2] = m_senderPackLib.GetTotalSent();
        res[3] = m_senderPackLib.GetTotalSavedData();    
        res[4] = GenerateDebugInfo();
        return res;
    }
    protected void Disposing()
    {
        //LogUtility.LogFile(GetRequestPath() +  " Total added  " + Long.toString(senderPackLib.GetTotalAdded()) +
        //"Total sent " + Long.toString(senderPackLib.GetTotalSent()) + 
        //"Total saved " + Long.toString(senderPackLib.GetTotalSavedData()),LogUtility.LogLevels.LEVEL_LOG_HIGH3);
    }
	@Override
	public void OnMessageReadyToTx(Object param, byte[] msg, boolean submit2Head) {
		LogUtility.LogFile("Entering OnMsgReady4Tx", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
        ProprietarySegmentSubmitMsg4Tx(msg,submit2Head);
        //ProprietarySegmentTransmit();
        LogUtility.LogFile("Leaving OnMsgReady4Tx", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
	}
}
