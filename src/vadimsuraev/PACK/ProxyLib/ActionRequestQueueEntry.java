package vadimsuraev.PACK.ProxyLib;

import java.nio.channels.spi.AbstractSelectableChannel;

class ActionRequestQueueEntry
{
	/**
	 * 
	 */
	int m_op;
	AbstractSelectableChannel m_channel;
	Object m_data;
	ProxySocket m_ProxySocket;
	String m_DebugInfo;
	
	void Init(int op,AbstractSelectableChannel channel,Object data, ProxySocket proxySocket)
	{
		m_op = op;
		m_channel = channel;
		m_data = data;
		m_ProxySocket = proxySocket;
	}
	
	public ActionRequestQueueEntry(int op,AbstractSelectableChannel channel,Object data, ProxySocket proxySocket)
	{
		m_DebugInfo = "dummy";
		Init(op,channel,data, proxySocket);
	}
	public ActionRequestQueueEntry(int op,AbstractSelectableChannel channel,Object data, ProxySocket proxySocket,String debugInfo)
	{
		m_DebugInfo = debugInfo;
		Init(op,channel,data, proxySocket);
	}
	public int GetOp()
	{
		return m_op;
	}
	public AbstractSelectableChannel GetChannel()
	{
		return m_channel;
	}
	public Object GetData()
	{
		return m_data;
	}
	public ProxySocket GetProxySocketInstance()
	{
		return m_ProxySocket;
	}
	public String GetDebugInfo()
	{
		return m_DebugInfo;
	}
}