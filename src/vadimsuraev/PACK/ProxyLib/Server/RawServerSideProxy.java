package vadimsuraev.PACK.ProxyLib.Server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.ProxyLib.ProxySocket;

public class RawServerSideProxy extends ServerSideProxy
{
	SocketAddress remoteEndPoint;
    public RawServerSideProxy(SocketChannel sock)
    {
    	super(sock);
    	InetAddress ia = null;
		try {
			ia = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        remoteEndPoint = new InetSocketAddress(ia,8888);
    }
    public void SetRemoteEndpoint(SocketAddress ipEndpoint)
    {
        remoteEndPoint = ipEndpoint;
    }
    public byte []GetFirstBuffToTransmitDestination()
    {
        try
        {
        	SocketAddress DestinationEndPoint = remoteEndPoint;
            m_destinationSideSocket = new ProxySocket("ServerSide",false, this);
            LogUtility.LogFile("Trying to connect destination " + DestinationEndPoint.toString(), ModuleLogLevel);
            m_destinationSideSocket.Connect(DestinationEndPoint);
            LogUtility.LogFile("destination connected ", ModuleLogLevel);
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return null;
        }
        return m_rxStateMachine.GetMsgBody();
    }
    public void ProcessUpStreamDataKind()
    {
        LogUtility.LogFile(m_Id.toString() + " Entering ProcessUpStreamDataKind", ModuleLogLevel);
        try
        {
            if ((m_destinationSideSocket != null) && (m_destinationSideSocket.isConnected()))
            {
                LogUtility.LogFile("destination connected, submit " + Long.toString(m_rxStateMachine.GetMsgBody().length), ModuleLogLevel);
                NonProprietarySegmentSubmitStream4Tx(m_rxStateMachine.GetMsgBody());
                //NonProprietarySegmentTransmit();
            }
            else if (m_destinationSideSocket == null)
            {
                LogUtility.LogFile("destination is not connected and no attempt " + Long.toString(m_rxStateMachine.GetMsgBody().length), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                //ShutDownFlag = false;
                byte []data = GetFirstBuffToTransmitDestination();
                try
                {
                    if (data != null)
                    {
                        NonProprietarySegmentSubmitStream4Tx(data);
                        //NonProprietarySegmentTransmit();
                    }
                }
                catch (Exception exc)
                {
                    LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                }
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        LogUtility.LogFile(m_Id.toString() + " Leaving ProcessUpStreamDataKind", ModuleLogLevel);
    }
    public void ProcessUpStreamMsgKind()
    {
    }
    public boolean ProcessDownStreamData(byte[] data, boolean isInvokedOnTransmit)
    {
        if (!isInvokedOnTransmit)
        {
            ProprietarySegmentSubmitStream4Tx(data);
            return true;
        }
        return false;
    }
	@Override
	public void OnMsgReceived() {
		OnProprietarySegmentMsgReceived();
	}
	@Override
	public void OnRead(Object data, int count) {
		Boolean isProp = (Boolean)data;
		if(isProp)
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
		Boolean isProp = (Boolean)data;
		if(isProp)
		{
			OnProprietarySegmentTransmitted(count);
		}
		else
		{
			OnNonProprietarySegmentTransmitted(count);
		}
	}
}
