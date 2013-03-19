package vadimsuraev.PACK.ProxyLib.Server;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import vadimsuraev.LogUtility.*;
import vadimsuraev.PACK.ProxyLib.ProxySocket;


public class HttpServerSideProxy extends ServerSideProxy
{
	// private variables
	/// <summary>Holds the value of the HttpQuery property.</summary>
	private String m_HttpQuery;
	/// <summary>Holds the value of the RequestedPath property.</summary>
	private String m_RequestedPath;
	/// <summary>Holds the value of the HeaderFields property.</summary>
	private HashMap<String,String> m_HeaderFields;
	/// <summary>Holds the value of the HttpVersion property.</summary>
	private String m_HttpVersion;
	/// <summary>Holds the value of the HttpRequestType property.</summary>
	private String m_HttpRequestType;
	/// <summary>Holds the POST data</summary>
	private String m_HttpPost;
	public HttpServerSideProxy(SocketChannel sock)
	{
		super(sock);
		m_Id = sock.socket().getRemoteSocketAddress();
		m_RequestedPath = null;
		HttpReqCleanUp();
	}
	void HttpReqCleanUp()
	{
		m_HttpQuery = "";
		m_HeaderFields = null;
		m_HttpVersion = "";
		m_HttpRequestType = "";
		m_HttpPost = null;
	}
	public String GetRequestPath()
	{
		if (m_RequestedPath == null)
		{
			return "PATH IS EMPTY, QUERY: " + m_HttpQuery;
		}
		return m_RequestedPath;
	}
	public byte []GetFirstBuffToTransmitDestination()
	{
		byte[] buff = m_rxStateMachine.GetMsgBody();
		char []chs = new char[buff.length];
		for(int i = 0;i < buff.length;i++)
		{
			chs[i] = (char)buff[i];
		}
		m_HttpQuery += /*Encoding.ASCII.GetString(buff, 0, buff.length)*/String.valueOf(chs);
		if (IsValidQuery())
		{
			LogUtility.LogFile(m_Id.toString() + " Valid query:  " + m_HttpQuery, LogUtility.LogLevels.LEVEL_LOG_HIGH3);
			ProcessQuery();
		}
		return null;
	}
	public void ProcessUpStreamDataKind()
	{
		LogUtility.LogFile(m_Id.toString() + " Entering ProcessUpStreamDataKind", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
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
				LogUtility.LogFile("destination is not connected and no attempt " + Long.toString(m_rxStateMachine.GetMsgBody().length), ModuleLogLevel);
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
		LogUtility.LogFile(m_Id.toString() + " Leaving ProcessUpStreamDataKind", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
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
	void SendBadRequest()
	{
		try
		{
			ProprietarySegmentSubmitStream4Tx("HTTP/1.1 400 Bad Request\r\nConnection: close\r\nContent-Type: text/html\r\n\r\n<html><head><title>400 Bad Request</title></head><body><div align=\"center\"><table border=\"0\" cellspacing=\"3\" cellpadding=\"3\" bgcolor=\"#C0C0C0\"><tr><td><table border=\"0\" width=\"500\" cellspacing=\"3\" cellpadding=\"3\"><tr><td bgcolor=\"#B2B2B2\"><p align=\"center\"><strong><font size=\"2\" face=\"Verdana\">400 Bad Request</font></strong></p></td></tr><tr><td bgcolor=\"#D1D1D1\"><font size=\"2\" face=\"Verdana\"> The proxy server could not understand the HTTP request!<br><br> Please contact your network administrator about this problem.</font></td></tr></table></center></td></tr></table></div></body></html>".getBytes());
			//ProprietarySegmentTransmit();
			m_ErrorSent = true;
		}
		catch(Exception exc)
		{
			LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
		}
	}
	boolean IsValidQuery()
	{
		int index = m_HttpQuery.indexOf("\r\n\r\n");
		if (index == -1)
			return false;
		m_HeaderFields = ParseQuery();
		if (m_HttpRequestType.toUpperCase().equals("POST"))
		{
			try
			{
				int length = Integer.parseInt((String)m_HeaderFields.get("Content-Length"));
				return m_HttpQuery.length() >= index + 6 + length;
			}
			catch(Exception exc)
			{
				LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
				SendBadRequest();
				return true;
			}
		}
		else
		{
			return true;
		}
	}
	String RebuildQuery()
	{
		String ret = m_HttpRequestType + " " + m_RequestedPath + " " + m_HttpVersion + "\r\n";
		if (m_HeaderFields != null)
		{
			Iterator<String> itr = m_HeaderFields.values().iterator();
			while(itr.hasNext())
			{
				String sc = itr.next();
				if (sc.length() < 6 || !sc.substring(0, 6).equals("proxy-"))
					ret += sc.toUpperCase(Locale.getDefault()) + ": " + (String)m_HeaderFields.get(sc) + "\r\n";
			}
			ret += "\r\n";
			if (m_HttpPost != null)
				ret += m_HttpPost;
		}
		return ret;
	}
	void ProcessQuery()
	{
		m_HeaderFields = ParseQuery();
		if (m_HeaderFields == null || !m_HeaderFields.containsKey("Host"))
		{
			SendBadRequest();
			return;
		}
		int Port;
		String Host;
		int Ret;
		if (m_HttpRequestType.toUpperCase().equals("CONNECT"))
		{ //HTTPS
			Ret = m_RequestedPath.indexOf(":");
			if (Ret >= 0)
			{
				Host = m_RequestedPath.substring(0, Ret);
				if (m_RequestedPath.length() > Ret + 1)
					Port = Integer.parseInt(m_RequestedPath.substring(Ret + 1));
				else
					Port = 443;
			}
			else
			{
				Host = m_RequestedPath;
				Port = 443;
			}
		}
		else
		{ //Normal HTTP
			Ret = ((String)m_HeaderFields.get("Host")).indexOf(":");
			if (Ret > 0)
			{
				Host = ((String)m_HeaderFields.get("Host")).substring(0, Ret);
				Port = Integer.parseInt(((String)m_HeaderFields.get("Host")).substring(Ret + 1));
			}
			else
			{
				Host = (String)m_HeaderFields.get("Host");
				Port = 80;
			}
			if (m_HttpRequestType.toUpperCase().equals("POST"))
			{
				int index = m_HttpQuery.indexOf("\r\n\r\n");
				m_HttpPost = m_HttpQuery.substring(index + 4);
			}
		}
		try
		{
			InetAddress destIp = null;
			InetAddress []addr = null;
			InetSocketAddress DestinationEndPoint;
			try
			{
				addr = InetAddress.getAllByName(Host);
			}
			catch(UnknownHostException exc)
			{
				;
			}
			if(addr != null)
			{
				destIp = addr[0];
			}
			else
			{
				LogUtility.LogFile(m_Id.toString() + " Cannotget IP address " + Host, LogUtility.LogLevels.LEVEL_LOG_HIGH);
				SendBadRequest();
				return;
			}
			LogUtility.LogFile(m_Id.toString() + " host name is the IP address " + destIp.toString(), ModuleLogLevel);
			DestinationEndPoint = new InetSocketAddress(destIp, Port);
			m_destinationSideSocket = new ProxySocket("Server",false,this);
			//destinationSideSocket.LingerState.Enabled = true;
			//destinationSideSocket.LingerState.LingerTime = 5000;
			if (m_HeaderFields.containsKey("Proxy-Connection") && m_HeaderFields.get("Proxy-Connection").toLowerCase().equals("keep-alive"))
			{
				m_destinationSideSocket.SetKeepAlive(true);
			}
			LogUtility.LogFile(m_Id.toString() + " Trying to connect destination " + DestinationEndPoint.toString(), ModuleLogLevel);
			m_destinationSideSocket.Connect(DestinationEndPoint);
			LogUtility.LogFile(m_Id.toString() + " destination connected ", ModuleLogLevel);
			m_OnceConnected = true;
			//QueueElement queueElement = new QueueElement();
			if (m_HttpRequestType.toUpperCase().equals("CONNECT"))
			{ //HTTPS
				ProprietarySegmentSubmitStream4Tx((m_HttpVersion + " 200 Connection established\r\nProxy-Agent: Vadim Suraev Proxy Server\r\n\r\n").getBytes());
				//ProprietarySegmentTransmit();
			}
			else
			{
				LogUtility.LogFile(m_Id.toString() + " Submit to tx to destination ", ModuleLogLevel);
				NonProprietarySegmentSubmitStream4Tx(RebuildQuery().getBytes());
				//NonProprietarySegmentTransmit();
			}
			m_RequestedPath = Host + " " + m_RequestedPath;
			HttpReqCleanUp();
		}
		catch(Exception exc)
		{
			LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
			SendBadRequest();
			return;
		}
	}
	HashMap<String,String> ParseQuery()
	{
		HashMap<String,String> retdict = new HashMap<String,String>();
		String[] Lines = m_HttpQuery.replace("\r\n", "\n").split("\n");
		int Cnt, Ret;
		//Extract requested URL
		if (Lines.length > 0)
		{
			//Parse the Http Request Type
			Ret = Lines[0].indexOf(' ');
			if (Ret > 0)
			{
				m_HttpRequestType = Lines[0].substring(0, Ret);
				Lines[0] = Lines[0].substring(Ret).trim();
			}
			//Parse the Http Version and the Requested Path
			Ret = Lines[0].lastIndexOf(' ');
			if (Ret > 0)
			{
				m_HttpVersion = Lines[0].substring(Ret).trim();
				m_RequestedPath = Lines[0].substring(0, Ret);
			}
			else
			{
				m_RequestedPath = Lines[0];
			}
			//Remove http:// if present
			if (m_RequestedPath.length() >= 7 && m_RequestedPath.substring(0, 7).toLowerCase().equals("http://"))
			{
				Ret = m_RequestedPath.indexOf('/', 7);
				if (Ret == -1)
					m_RequestedPath = "/";
				else
					m_RequestedPath = m_RequestedPath.substring(Ret);
			}
		}
		for (Cnt = 1; Cnt < Lines.length; Cnt++)
		{
			Ret = Lines[Cnt].indexOf(":");
			if (Ret > 0 && Ret < Lines[Cnt].length() - 1)
			{
				try
				{
					retdict.put(Lines[Cnt].substring(0, Ret), Lines[Cnt].substring(Ret + 1).trim());
				}
				catch(Exception exc)
				{
					LogUtility.LogFile(m_Id.toString() + " EXCEPTION " + exc.getMessage(), LogUtility.LogLevels.LEVEL_LOG_HIGH);
				}
			}
		}
		return retdict;
	}
	@Override
	public void OnMsgReceived() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void OnRead(Object data, int count) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void OnWritten(Object data, int count) {
		// TODO Auto-generated method stub
		
	}
}
