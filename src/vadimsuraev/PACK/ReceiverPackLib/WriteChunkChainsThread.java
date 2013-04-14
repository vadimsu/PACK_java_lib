package vadimsuraev.PACK.ReceiverPackLib;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import vadimsuraev.PACK.ChunkAndChainManager.ChunkAndChainFileManager;
import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.PackChunking.PackChunking;
import vadimsuraev.PACK.ReceiverPackLib.Chains2Save;

class WriteChunkChainsThread extends Thread
{
	static final long mc_MinChainLengthInBytes = 1024 * 10;
    static final long mc_MinChainLengthInChunks = 10;
    static ReentrantLock m_Chains2SaveMutex = new ReentrantLock();
    static HashMap<SocketAddress, List<Chains2Save>> m_ChainsPerIpEndpoint = new HashMap<SocketAddress, List<Chains2Save>>();
    static BlockingQueue<SocketAddress> m_SaveQueue = new LinkedBlockingQueue<SocketAddress>();
    static int m_FlushSwTimer;
	public WriteChunkChainsThread()
	{
		super();
		m_FlushSwTimer = -1;
	}
	static public void OnComplete(SocketAddress ipEndPoint,WriteChunkChainsThread thread2WakeUp)
    {
        try
        {
            m_SaveQueue.add(ipEndPoint);
        }
        catch (Exception exc)
        {
            LogUtility.LogException("",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
    }
	static public void AddChain2Save(Chains2Save chain2Save,SocketAddress ipEndPoint)
    {
        m_Chains2SaveMutex.lock();
        try
        {
            List<Chains2Save> list = m_ChainsPerIpEndpoint.get(ipEndPoint);
            if (list != null)
            {
                list.add(chain2Save);
            }
            else
            {
                list = new LinkedList<Chains2Save>();
                list.add(chain2Save);
                m_ChainsPerIpEndpoint.put(ipEndPoint, list);
            }
        }
        catch (Exception exc)
        {
            LogUtility.LogException("",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
        }
        m_Chains2SaveMutex.unlock();
    }
	static public long GetChainLength(List<Long> chunkList)
    {
        long length = 0;
        Iterator<Long> itr = chunkList.iterator();
        while(itr.hasNext())
        {
        	Long chunk = (Long) itr.next();
            length += (long)PackChunking.chunkToLen(chunk);
        }
        return length;
    }
	static public boolean PassesSaveCriteria(Chains2Save chain2Save)
    {
        return ((GetChainLength(chain2Save.GetChunkList()) >= mc_MinChainLengthInBytes)&&((chain2Save.GetLastNonMatchingChunk() - chain2Save.GetFirstNonMatchingChunk() >= mc_MinChainLengthInChunks)));
    }
	static void FlushIfRequired()
	{
		m_FlushSwTimer++;
		if (m_FlushSwTimer == 200)
		{
			m_FlushSwTimer = -1;
			try
			{
				LogUtility.LogFile("FLUSHING ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
				ReceiverPackLib.Flush();
			}
			catch (Exception exc)
			{
				LogUtility.LogException("",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
			}
		}
	}
	public void run()
	{
		
        int time2wait;
        while (true)
        {
            try
            {
            	SocketAddress ipEndPoint = m_SaveQueue.poll(30000,TimeUnit.MILLISECONDS);
                if (ipEndPoint == null)
                {
                    LogUtility.LogFile("Queue is empty", LogUtility.LogLevels.LEVEL_LOG_HIGH);
                    FlushIfRequired();
                    continue;
                }
                LogUtility.LogFile("Queue is not empty", LogUtility.LogLevels.LEVEL_LOG_HIGH);
                List<Chains2Save> list = null;
                m_Chains2SaveMutex.lock();
                if (m_ChainsPerIpEndpoint.size() > 0)
                {
                	list = m_ChainsPerIpEndpoint.remove(ipEndPoint);
                }
                m_Chains2SaveMutex.unlock();
                if (list != null)
                {
                	Iterator<Chains2Save> itr = list.iterator();
                    while (itr.hasNext())
                    {
                    	Chains2Save ch2Sv = itr.next();
                        if (PassesSaveCriteria(ch2Sv))
                        {
                            ChunkAndChainFileManager.SaveChain(ch2Sv.GetChunkList(), ch2Sv.GetFirstNonMatchingChunk(), ch2Sv.GetLastNonMatchingChunk(), ch2Sv.GetPacket(), ch2Sv.GetFirstNonMatchingChunkOffset(), ch2Sv.GetChunkAndChainFileManager());
                            LogUtility.LogFile("Saved chain " + Integer.toString(ch2Sv.GetChunkList().size()) + " " + Integer.toString(ch2Sv.GetFirstNonMatchingChunk()) + " " + Integer.toString(ch2Sv.GetLastNonMatchingChunk()) + " " + Integer.toString(ch2Sv.GetFirstNonMatchingChunkOffset()), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                        }
                    }
                    list.clear();
                }
                FlushIfRequired();
            }
            catch(Exception exc)
            {
                LogUtility.LogException("",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
            }
        }
	}
}