package vadimsuraev.PACK.ProxyLib;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import vadimsuraev.PACK.ProxyLib.Server.*;

public class Listener implements SocketAcceptCallback, IOnGotResults
{
    protected ProxySocket m_socket;
    byte m_ProxyType;
    SocketAddress m_remoteEndpoint;
    protected IOnGotResults m_IOnGotResults;

    public static void InitGlobalObjects()
    {
        Proxy.InitGlobalObjects();
    }

    public Listener(SocketAddress ipEndPoint,byte proxyType,IOnGotResults onGotResults)
    {
        m_ProxyType = proxyType;
        m_socket = new ProxySocket("Listener",false,this);
        m_socket.Bind(ipEndPoint);
        InetAddress ia = null;
        m_IOnGotResults = onGotResults;
		try {
			ia = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_remoteEndpoint = new InetSocketAddress(ia,7777);
    }

    public void SetRemoteEndpoint(SocketAddress ipEndPoint)
    {
        m_remoteEndpoint = ipEndPoint;
    }
    public void Start()
    {
        m_socket.AcceptAsync();
    }

    public void Stop()
    {
    	m_socket.ServerClose();
    }

	@Override
	public void OnAccepted(SocketChannel socketChannel) {
		Proxy serverSideProxy = null;
		switch (ServerSideProxyTypes.values()[m_ProxyType])
        {
            case SERVER_SIDE_PROXY_RAW:
                serverSideProxy = new RawServerSideProxy(socketChannel);
                break;
            case SERVER_SIDE_PROXY_HTTP:
                serverSideProxy = new HttpServerSideProxy(socketChannel);
                break;
            case SERVER_SIDE_PROXY_PACK_HTTP:
                serverSideProxy = new PackHttpServerSide(socketChannel);
                break;
            case SERVER_SIDE_PROXY_PACK_RAW:
                serverSideProxy = new PackRawServerSide(socketChannel);
                break;
        }
		if (serverSideProxy != null)
        {
            serverSideProxy.SetRemoteEndpoint((InetSocketAddress) m_remoteEndpoint);
            serverSideProxy.Start();
        }
	}

	@Override
	public void OnGotResults(Object results) {
		if(m_IOnGotResults != null)
		{
			m_IOnGotResults.OnGotResults(results);
		}
	}
}
