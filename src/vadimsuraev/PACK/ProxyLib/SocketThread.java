package vadimsuraev.PACK.ProxyLib;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

class SocketThread extends Thread
{
	/**
	 * 
	 */
	SelectorProvider m_selectorProvider;
	Selector m_selector = null;
	ReentrantLock m_ActionQueueMutex;
	LinkedList<ActionRequestQueueEntry> m_ActionQueue;
	
	public SocketThread()
	{
		super();
		m_selectorProvider = SelectorProvider.provider();
		m_selector = null;
		m_ActionQueueMutex = new ReentrantLock();
		m_ActionQueue = new LinkedList<ActionRequestQueueEntry>();
		try {
			m_selector = Selector.open();
			//				m_selector = m_selectorProvider.openSelector();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void CancellAll(AbstractSelectableChannel channel)
	{
		m_ActionQueueMutex.lock();
		SelectionKey sk = ((SocketChannel)channel).keyFor(m_selector);
		if(sk != null)
		{
			sk.cancel();
		}
		Iterator<ActionRequestQueueEntry> iterator = m_ActionQueue.iterator();
		ActionRequestQueueEntry entry = null;
		LinkedList<ActionRequestQueueEntry> temp = new LinkedList<ActionRequestQueueEntry>();
		if(iterator.hasNext())
		{
			entry = iterator.next();
		}
		while(entry != null)
		{
			if(entry.GetChannel() == channel)
			{
				temp.add(entry);
			}
			if(!iterator.hasNext())
			{
				break;
			}
			entry = iterator.next();
		}
		if(temp.size() > 0)
		{
			m_ActionQueue.removeAll(temp);
		}
		m_ActionQueueMutex.unlock();
	}
	
	public void SubmitAction(int op,AbstractSelectableChannel channel,Object data,ProxySocket proxySocket)
	{
		SubmitAction(op,channel,data,proxySocket,"dummy");
	}
	
	public void SubmitAction(int op,AbstractSelectableChannel channel,Object data,ProxySocket proxySocket,String debugInfo)
	{
		ActionRequestQueueEntry entry = new ActionRequestQueueEntry(op,channel,data,proxySocket,debugInfo);
		m_ActionQueueMutex.lock();
		m_ActionQueue.add(entry);
		m_ActionQueueMutex.unlock();
		m_selector.wakeup();
	}
	
	ActionRequestQueueEntry DequeueAction()
	{
		ActionRequestQueueEntry entry;
		m_ActionQueueMutex.lock();
		entry = (m_ActionQueue.size() > 0) ? m_ActionQueue.removeFirst() : null;
		m_ActionQueueMutex.unlock();
		return entry;
	}
	
	void Connect(ActionRequestQueueEntry entry)
	{
		SocketAddress sa;
		SocketChannel sc;
		
		sa = (SocketAddress) entry.GetData();
		sc = (SocketChannel) entry.GetChannel();
		try {
			((SocketChannel)sc).connect(sa);
			System.out.println("connected " + sa);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			sc.configureBlocking(false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void Read(ActionRequestQueueEntry entry)
	{
		SocketChannel sc;
		SelectionKey sk = null;
		int ops = 0;
		sc = (SocketChannel) entry.GetChannel();
		try {
			sk = sc.keyFor(m_selector);
			if(sk != null)
			{
				ops = sk.interestOps();
			}
			sk = sc.register(m_selector, ops | SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(sk != null)
		{
			sk.attach(entry.GetProxySocketInstance());
		}
		else
		{
			System.out.println("Attachment is null ");
		}
//		System.out.println("Registered for read ");
	}
	void Write(ActionRequestQueueEntry entry)
	{
		SocketChannel sc;
		SelectionKey sk = null;
		int ops = 0;
		sc = (SocketChannel) entry.GetChannel();
		try {
			sk = sc.keyFor(m_selector);
			if(sk != null)
			{
				ops = sk.interestOps();
			}
			sk = sc.register(m_selector, ops | SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(sk != null)
		{
			sk.attach(entry.GetProxySocketInstance());
		}
		else
		{
			System.out.println("Attachment is null ");
		}
//		System.out.println("Registered for write ");
	}
	
	void Accept(ActionRequestQueueEntry entry)
	{
		ServerSocketChannel sc;
		SelectionKey sk = null;
		sc = (ServerSocketChannel) entry.GetChannel();
		try {
			sc.configureBlocking(false);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			sk = sc.register(m_selector, SelectionKey.OP_ACCEPT);
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(sk != null)
		{
			sk.attach(entry.GetProxySocketInstance());
		}
		System.out.println("Registered for accept ");
	}
	
	public void run()
	{
		int readyChannels = 0;
		int ops = 0;
		ActionRequestQueueEntry entry;
		try {
			//m_selector = Selector.open();
							m_selector = m_selectorProvider.openSelector();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(true)
		{
			try 
			{
				readyChannels = m_selector.select(500);
				//System.out.println(" Proxy socket, got " + readyChannels + " channels");
				m_ActionQueueMutex.lock();
				while(m_ActionQueue.size() > 0)
				{
					entry = m_ActionQueue.removeFirst();
					switch(entry.GetOp())
					{
					case SelectionKey.OP_ACCEPT:
						System.out.println("Got accept op");
						Accept(entry);
						break;
					case SelectionKey.OP_CONNECT:
						System.out.println("Got connect op");
						Connect(entry);
						break;
					case SelectionKey.OP_READ:
						//System.out.println("Got read op");
						Read(entry);
						break;
					case SelectionKey.OP_WRITE:
						//System.out.println("Got write op");
						Write(entry);
						break;
					default:
						;
					}
				}
				m_ActionQueueMutex.unlock();
				if(readyChannels == 0)
				{
					continue;
				}
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Set<SelectionKey> selectedKeys = m_selector.selectedKeys();

			Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
			ProxySocket proxySocket = null;
			while(keyIterator.hasNext()) 
			{
				SelectionKey key = keyIterator.next();
				try
				{
					if (key.isReadable()) 
					{
						//System.out.println(" Readable");
						proxySocket = (ProxySocket) key.attachment();
						ops = key.interestOps() & ~(SelectionKey.OP_READ);
						try {
							SelectionKey key2 = key.channel().register(m_selector, ops);
							key2.attach(proxySocket);
						} catch (ClosedChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(proxySocket == null)
						{
							System.out.println(" read attachment is null!!!!");
						}
						else
						{
							//System.out.println(" read attachment is NOT null!!!!");
							proxySocket.OnRead();
						}
					}
					if ((key.isWritable())/*&&((key.interestOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE)*/) 
					{
						//System.out.println(" Writeable");
						proxySocket = (ProxySocket) key.attachment();
					    ops = key.interestOps() & ~(SelectionKey.OP_WRITE);
						try {
							SelectionKey key2 = key.channel().register(m_selector, ops);
							key2.attach(proxySocket);
						} catch (ClosedChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(proxySocket == null)
						{
							System.out.println(" write attachment is null!!!!");
						}
						else
						{
							proxySocket.OnWritable();
						}
					}
					if(key.isAcceptable())
					{
						System.out.println(" Acceptable");
					    proxySocket = (ProxySocket)key.attachment();
					    if(proxySocket == null)
					    {
					    	System.out.println(" accept attachment is null!!!!");
					    }
					    else
					    {
					    	proxySocket.OnAccept();
					    }
					}
				}
				catch(CancelledKeyException cce)
				{
					proxySocket = (ProxySocket)key.attachment();
				    if(proxySocket == null)
				    {
				    	System.out.println(" exception(closed) attachment is null!!!!");
				    }
				    else
				    {
				    	proxySocket.OnClosed();
				    }
				}
				keyIterator.remove();
			}
		}
	}
}