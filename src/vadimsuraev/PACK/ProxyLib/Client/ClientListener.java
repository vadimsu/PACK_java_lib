package vadimsuraev.PACK.ProxyLib.Client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import vadimsuraev.PACK.ProxyLib.IOnGotResults;
import vadimsuraev.PACK.ProxyLib.Listener;

public class ClientListener extends Listener
{
	SocketAddress m_serverSideEndPoint;
    byte m_ProxyType;
    public ClientListener(SocketAddress myEndPoint, SocketAddress remoteEndPoint,byte proxyType,IOnGotResults onGotResults)
    {
    	super(myEndPoint,(byte)0,onGotResults);
        m_serverSideEndPoint = remoteEndPoint;
        m_ProxyType = proxyType;
    }
    @Override
	public void OnAccepted(SocketChannel socketChannel) {
    	ClientSideProxy clientSideProxy = null;
        switch (ClientSideProxyTypes.values()[m_ProxyType])
        {
            case CLIENT_SIDE_PROXY_PACK:
                clientSideProxy = new PackClientSide(socketChannel, (InetSocketAddress)m_serverSideEndPoint,m_IOnGotResults);
                break;
            case CLIENT_SIDE_PROXY_GENERAL:
                clientSideProxy = new ClientSideProxy(socketChannel, (InetSocketAddress)m_serverSideEndPoint,m_IOnGotResults);
                break;
        }
        if(clientSideProxy != null)
        {
            clientSideProxy.Start();
        }
	}
}
