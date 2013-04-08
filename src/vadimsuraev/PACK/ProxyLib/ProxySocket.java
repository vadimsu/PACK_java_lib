package vadimsuraev.PACK.ProxyLib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import vadimsuraev.LogUtility.LogUtility;

public class ProxySocket 
{
	class TxQueueEntry
	{
		byte []m_buffer;
		int m_offset;
		int m_count;
		public TxQueueEntry(byte []buffer,int offset,int count)
		{
			m_buffer = buffer;
			m_offset = offset;
			m_count = count;
		}
		public byte []GetBuffer()
		{
			return m_buffer;
		}
		public int GetOffset()
		{
			return m_offset;
		}
		public int GetCount()
		{
			return m_count;
		}
	}
	AbstractSelectableChannel m_SocketChannel;
	SelectionKey m_SelectionKey;
	SocketCallbacks m_SocketCallbacks;
	SocketAcceptCallback m_SocketAcceptCallback;
	Object m_data;
	byte []m_CurrentReceiveBuffer;
	int  m_CurrentReceiveOffset;
	int m_CurrentReceiveCount;
	ReentrantLock m_ReadMutex;
	ReentrantLock m_WriteMutex;
	LinkedList<TxQueueEntry> m_txQueue;
	String m_name;
	static SocketThread m_SocketThread;
	void GenericInit(String name,Object data,SocketCallbacks socketCallbacks)
	{
		if(m_SocketThread == null)
		{
			m_SocketThread = new SocketThread();
			m_SocketThread.start();
		}
		m_name = name;
		m_data = data;
		m_SocketCallbacks = socketCallbacks;
		m_SelectionKey = null;
		m_CurrentReceiveBuffer = null;
    	m_CurrentReceiveOffset = 0;
    	m_CurrentReceiveCount = 0;
    	m_ReadMutex = new ReentrantLock();
    	m_WriteMutex = new ReentrantLock();
    	m_txQueue = new LinkedList<TxQueueEntry>();
	}
    public ProxySocket(String name,SocketChannel socketChannel, Object data,SocketCallbacks socketCallbacks)
    {
    	m_SocketChannel = socketChannel;
    	GenericInit(name,data,socketCallbacks);
		System.out.println(m_name + " Proxy socket (already connected) ");
		try {
			m_SocketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    public ProxySocket(String name,Object data,SocketCallbacks socketCallbacks)
    {
    	GenericInit(name,data,socketCallbacks);
    	try 
    	{
			m_SocketChannel = SocketChannel.open();
		}
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    	System.out.println(m_name + " Proxy socket (not connected) ");
    }
    public ProxySocket(String name,Object data,SocketAcceptCallback socketAcceptCallback)
    {
    	m_SocketAcceptCallback = socketAcceptCallback;
    	if(m_SocketThread == null)
		{
			m_SocketThread = new SocketThread();
			m_SocketThread.start();
		}
		m_name = name;
		m_data = data;
		m_SelectionKey = null;
		m_CurrentReceiveBuffer = null;
    	m_CurrentReceiveOffset = 0;
    	m_CurrentReceiveCount = 0;
    	m_ReadMutex = null;
    	m_WriteMutex = null;
    	m_txQueue = null;
    	try {
			m_SocketChannel = ServerSocketChannel.open();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	System.out.println(m_name + " Proxy socket (server only) ");
    }
    public void Connect(SocketAddress sa)
    {
    	m_SocketThread.SubmitAction(SelectionKey.OP_CONNECT, m_SocketChannel, sa, this);
    }
    
    public boolean Bind(SocketAddress sa)
    {
    	ServerSocket serverSocket = ((ServerSocketChannel)m_SocketChannel).socket();
		try {
			serverSocket.bind(sa);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		System.out.println("Bound to " + sa);
    	return true;
    }
    public void AcceptAsync()
    {
    	m_SocketThread.SubmitAction(SelectionKey.OP_ACCEPT, m_SocketChannel, false, this);
    }
    public void OnConnected()
    {
    	m_SocketCallbacks.OnConnected();
    }
    public void OnAccept()
    {
    	SocketChannel socketChannel = null;
    	try {
    		socketChannel = ((ServerSocketChannel)m_SocketChannel).accept();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	m_SocketAcceptCallback.OnAccepted(socketChannel);
    }
    public void OnRead()
    {
    	LogUtility.LogFile("OnRead " + m_name, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    	byte []buf = null;
    	int offset;
    	int count;
    	m_ReadMutex.lock();
    	buf = m_CurrentReceiveBuffer;
    	offset = m_CurrentReceiveOffset;
    	count = m_CurrentReceiveCount;
    	m_CurrentReceiveBuffer = null;
    	m_CurrentReceiveOffset = 0;
    	m_CurrentReceiveCount = 0;
    	m_ReadMutex.unlock();
    	ByteBuffer dst = ByteBuffer.wrap(buf);
    	dst.position(offset);
    	dst.limit(offset+count);
    	int bytesRead = 0;
    	try {
    		bytesRead = ((SocketChannel)m_SocketChannel).read(dst);
    		if(bytesRead != count)
    		{
    			LogUtility.LogFile("Actually read " + Long.toString(bytesRead) + " needed " + Long.toString(count), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    		}
		} catch (IOException e) {
			m_SocketCallbacks.OnConnectionBroken(m_data);
			return;
		}
    	m_SocketCallbacks.OnRead(m_data,bytesRead);
    }
    public void OnWritable()
    {
    	LogUtility.LogFile("OnWritable " + m_name, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    	int written = 0;
    	m_WriteMutex.lock();
    	while(!m_txQueue.isEmpty())
    	{
    		TxQueueEntry txQueueEntry = m_txQueue.element();
    		ByteBuffer src = ByteBuffer.wrap(txQueueEntry.GetBuffer());
    		src.position(txQueueEntry.GetOffset());
    		src.limit(txQueueEntry.GetOffset() + txQueueEntry.GetCount());
    		try 
    		{
    			if(((SocketChannel)m_SocketChannel).socket().getSendBufferSize() < txQueueEntry.GetCount())
    			{
    				LogUtility.LogFile("Send buffer is smaller!!! " + Long.toString(((SocketChannel)m_SocketChannel).socket().getSendBufferSize()) + " required " + Long.toString(src.remaining()) + " " + Long.toString(txQueueEntry.GetCount()), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    				((SocketChannel)m_SocketChannel).socket().setSendBufferSize(src.remaining());
    			}
    			else
    			{
    				LogUtility.LogFile("Send buffer is ok!!! " + Long.toString(((SocketChannel)m_SocketChannel).socket().getSendBufferSize()) + " required " + Long.toString(src.remaining()) + " " + Long.toString(txQueueEntry.GetCount()), LogUtility.LogLevels.LEVEL_LOG_HIGH);
    			}
				int writtenThisTime = ((SocketChannel)m_SocketChannel).write(src);
				if(writtenThisTime <= 0)
				{
					LogUtility.LogFile("write failed ", LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
					break;
				}
				if(writtenThisTime != txQueueEntry.GetCount())
	    		{
	    			LogUtility.LogFile("Actually written " + Long.toString(writtenThisTime) + " needed " + Long.toString(txQueueEntry.GetCount()), LogUtility.LogLevels.LEVEL_LOG_HIGH);
	    		}
				written += writtenThisTime;
				m_txQueue.removeFirst();
			} 
    		catch (IOException e) 
    		{
    			m_WriteMutex.unlock();
    			e.printStackTrace();
    			m_SocketCallbacks.OnConnectionBroken(m_data);
    			return;
			}
    		txQueueEntry = null;
    	}
    	m_WriteMutex.unlock();
    	m_SocketCallbacks.OnWritten(m_data,written);
    }
    public void OnClosed()
    {
    	m_SocketCallbacks.OnConnectionBroken(m_data);
    }
    int InitiateReceive(byte []buffer,int offset,int count)
    {
//    	System.out.println(m_name + " ReceiveAsync ");
    	m_ReadMutex.lock();
    	if(m_CurrentReceiveBuffer != null)
    	{
    		m_ReadMutex.unlock();
    		return -1;
    	}
    	m_CurrentReceiveBuffer = buffer;
    	m_CurrentReceiveOffset = offset;
    	m_CurrentReceiveCount = count;
    	m_ReadMutex.unlock();
    	return 0;
    }
    public int ReceiveAsync(byte []buffer,int offset,int count)
    {
    	if(InitiateReceive(buffer,offset,count) != 0)
        {
        	return -1;
        }
    	m_SocketThread.SubmitAction(SelectionKey.OP_READ, m_SocketChannel, false, this);
    	LogUtility.LogFile("ReceiveAsync succeeded " + m_name, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    	return 0;
    }
    public int ReceiveAsync(byte []buffer,int offset,int count,String debugInfo)
    {
        if(InitiateReceive(buffer,offset,count) != 0)
        {
        	return -1;
        }
    	m_SocketThread.SubmitAction(SelectionKey.OP_READ, m_SocketChannel, false, this,debugInfo);
    	LogUtility.LogFile("ReceiveAsync succeeded " + m_name, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    	return 0;
    }
    void InitiateSend(byte []buffer,int offset,int count)
    {
    	TxQueueEntry txQueueEntry = new TxQueueEntry(buffer,offset,count);
    	m_WriteMutex.lock();
    	m_txQueue.add(txQueueEntry);
    	m_WriteMutex.unlock();
    	LogUtility.LogFile("Send initiated " + m_name, LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
    }
    public int SendAsync(byte []buffer,int offset,int count)
    {
    	InitiateSend(buffer,offset,count);
    	m_SocketThread.SubmitAction(SelectionKey.OP_WRITE, m_SocketChannel, false, this);
    	return 0;
    }
    public int SendAsync(byte []buffer,int offset,int count,String debugInfo)
    {
    	InitiateSend(buffer,offset,count);
    	m_SocketThread.SubmitAction(SelectionKey.OP_WRITE, m_SocketChannel, false, this,debugInfo);
    	return 0;
    }
    public void Disconnect()
    {
    	m_SocketThread.SubmitAction(SocketThread.OP_DISCONNECT, m_SocketChannel, false, this);
    	/*m_SocketThread.CancellAll(m_SocketChannel);
    	try {
			((SocketChannel)m_SocketChannel).socket().shutdownInput();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	try {
			((SocketChannel)m_SocketChannel).socket().shutdownOutput();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	try {
			((SocketChannel)m_SocketChannel).socket().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	try {
			((SocketChannel)m_SocketChannel).close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	m_SocketCallbacks.OnConnectionBroken(m_data);*/
    }
    public boolean isConnected()
    {
    	return ((SocketChannel)m_SocketChannel).socket().isConnected();
    }
    public void ServerClose()
    {
    	m_SocketThread.CancellAll(m_SocketChannel);
    	try {
			((ServerSocketChannel)m_SocketChannel).socket().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public void SetKeepAlive(boolean val)
    {
    	if(m_SocketChannel != null)
    	{
    		try {
				((SocketChannel)m_SocketChannel).socket().setKeepAlive(val);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    public SocketAddress GetlocalEndPoint()
    {
    	SocketAddress sa = ((SocketChannel)m_SocketChannel).socket().getLocalSocketAddress();
    	if(sa != null)
    	{
    		return sa;
    	}
    	sa = new InetSocketAddress(((SocketChannel)m_SocketChannel).socket().getLocalAddress(),
    			((SocketChannel)m_SocketChannel).socket().getLocalPort());
    	return sa;
    }
    
    public int GeRemotePort()
    {
    	return ((SocketChannel)m_SocketChannel).socket().getPort();
    }
    
    public String GetName()
    {
    	return m_name;
    }
}
