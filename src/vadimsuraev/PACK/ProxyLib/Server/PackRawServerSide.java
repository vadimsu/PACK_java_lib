package vadimsuraev.PACK.ProxyLib.Server;

import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.ReceiverPackLib.ClientSideCallbacks;
import vadimsuraev.PACK.senderPackLib.OnMsgReady4Tx;
import vadimsuraev.PACK.senderPackLib.SenderPackLib;

public class PackRawServerSide extends RawServerSideProxy implements OnMsgReady4Tx
{
    SenderPackLib senderPackLib;

    public PackRawServerSide(SocketChannel sock)
    {
    	super(sock);
        m_Id = sock.socket().getRemoteSocketAddress();
        senderPackLib = new SenderPackLib(m_Id, this);
    }
    
    public void ProcessUpStreamMsgKind()
    {
        LogUtility.LogFile("Entering ProcessUpStreamMsgKind", ModuleLogLevel);
        EnterProprietaryLibCriticalArea();
        try
        {
            senderPackLib.OnDataByteMode(m_rxStateMachine.GetMsgBody(), 0);
            LeaveProprietaryLibCriticalArea();
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            LeaveProprietaryLibCriticalArea();
        }
        //ProprietarySegmentSubmitStream4Tx(data);
        //ProprietarySegmentTransmit();
        LogUtility.LogFile("Leaving ProcessUpStreamMsgKind", ModuleLogLevel);
    }
    public void OnDownStreamTransmissionOpportunity()
    {
    }
    
    protected void OnBeginShutdown()
    {
        LogUtility.LogFile("Entering OnBeginShutdown", ModuleLogLevel);
        Flush();
        LogUtility.LogFile("Leaving OnBeginShutdown", ModuleLogLevel);
    }
    public boolean ProcessDownStreamData(byte[] data, boolean isInvokedOnTransmit)
    {
        byte[] buff = null;
        boolean ret = false;
        LogUtility.LogFile("Entering ProcessDownStreamData", ModuleLogLevel);
        EnterProprietaryLibCriticalArea();
        try
        {
            ret = senderPackLib.AddData(data,isInvokedOnTransmit);
            
            LeaveProprietaryLibCriticalArea();
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            LeaveProprietaryLibCriticalArea();
        }
        LogUtility.LogFile("Leaving ProcessDownStreamData", ModuleLogLevel);
        return ret;
    }

	@Override
	public void OnMessageReadyToTx(Object param, byte[] msg, boolean submit2Head) {
		LogUtility.LogFile("Entering OnMessageReadyToTx", ModuleLogLevel);
        ProprietarySegmentSubmitMsg4Tx(msg,submit2Head);
        //ProprietarySegmentTransmit();
        LogUtility.LogFile("Leaving OnMessageReadyToTx", ModuleLogLevel);
	}
}
