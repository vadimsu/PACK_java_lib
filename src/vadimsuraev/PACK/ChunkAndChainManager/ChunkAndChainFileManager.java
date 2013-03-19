package vadimsuraev.PACK.ChunkAndChainManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import vadimsuraev.ByteArrayScalarTypeConversionLib.*;
import vadimsuraev.ByteArrayUtils.ByteArrayUtils;
import vadimsuraev.PACK.ChunkChainDataTypes.Chain;
import vadimsuraev.PACK.ChunkChainDataTypes.ChunkCB;
import vadimsuraev.PACK.ChunkChainDataTypes.ChunkListAndChainId;
import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.PackChunking.PackChunking;
import vadimsuraev.ReferencedTypes.ReferencedByteArray;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.PACK.FileManager.*;


public class ChunkAndChainFileManager
{
	public static LogUtility.LogLevels ModuleLogLevel = LogUtility.LogLevels.LEVEL_LOG_HIGH;
	public static long ChunkExists = 0;
	public static long ChunksCreated = 0;
	public static long DifferentChains = 0;
	public static long ChainsCreated = 0;
	public static long ChainsUpdated = 0;
	public static long LongerChainsFound = 0;
	public static long CannotFindChunkInChain = 0;
	public static long ChainExistInChunkChainsList = 0;
	public static long ChainAdded2ExistingChunk = 0;
	public static long CannotGetChain = 0;
	public static long FoundEqualChains = 0;
	public static long ChainsReturned = 0;
	public static long ChunksLoaded = 0;
	public static long ChainsLoaded = 0;

	//FileManager.FileManager fileManager;
	static HashMap<Long, ChunkCB> chunkMap;
	long chainId4Lookup;
	static FileManager fileManager = new NonRamFileManager("bin\\datafiles");
	
	public ChunkAndChainFileManager()
	{
		chainId4Lookup = 0;
	}

	static void GetChainAsByteArray(long[] chunks, ReferencedByteArray buffer)
	{
		buffer.val = new byte[(Long.SIZE/8) * chunks.length + (Long.SIZE/8)];
		long offset = 0;
		offset += ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer.val, offset, (long)chunks.length);
		
		for (int chunkIdx = 0; chunkIdx < chunks.length; chunkIdx++)
		{
			offset += ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer.val, offset, chunks[chunkIdx]);
		}
	}

	static void GetChunkCBAsByteArray(ChunkCB chunkCB,ReferencedByteArray buffer)
	{
		buffer.val = new byte[(int) ChunkCB.GetSize(chunkCB.ChainsListSize)];
		long offset = 0;
		offset += ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer.val, offset, chunkCB.size);
		offset += ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer.val, offset, chunkCB.sha1);
		buffer.val[(int) offset++] = chunkCB.hint;
		offset += ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer.val, offset, chunkCB.fo.file);
		offset += ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer.val, offset, chunkCB.fo.offset);
		offset += ByteArrayScalarTypeConversionLib.Uint2ByteArray(buffer.val, offset, chunkCB.ChainsListSize);
		
		for (int chainIdx = 0;chainIdx < chunkCB.chains.length;chainIdx++)
		{
			offset += ByteArrayScalarTypeConversionLib.Long2ByteArray(buffer.val, offset, chunkCB.chains[chainIdx]);
		}
	}

	static Chain GetChain(long chainId)
	{
		byte[] buff;
		buff = fileManager.ReadChainFile(chainId);
		if (buff == null)
		{
			return null;
		}
		Chain chain = new Chain();
		long offset = 0;
		ReferencedLong rl = new ReferencedLong();
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Uint(buff, offset, rl);
		chain.NumberOfEntries = rl.val;
		chain.chunkIds = new long[(int) chain.NumberOfEntries];
		for (int idx = 0; idx < chain.NumberOfEntries; idx++)
		{
			offset += ByteArrayScalarTypeConversionLib.ByteArray2Long(buff, offset, rl);
			chain.chunkIds[idx] = rl.val;
		}
		return chain;
	}

	public static int CompareChains(List<Long> chunkList, int successorsChunkListIdx, long[] chunkIds, int chunk_idx)
	{
		long idx = (long)chunk_idx;
		LogUtility.LogFile("chunk_idx "+ Integer.toString(chunk_idx) + " compare " + Long.toString(chunkList.size() - successorsChunkListIdx) + " to " + Long.toString(chunkIds.length - chunk_idx), LogUtility.LogLevels.LEVEL_LOG_HIGH);
		
		if(successorsChunkListIdx == chunkList.size())
		{
			return (chunkIds.length - chunk_idx);
		}
		List<Long> subList = chunkList.subList(successorsChunkListIdx, chunkList.size());
		Iterator<Long> itr = subList.iterator();
		Long successorChunk;
		/* if successors list is longer than found chain, 
		 * test existing chunks for match. If existing chunks differ, the chain does not match */
		 if ((chunkList.size() - successorsChunkListIdx) > (chunkIds.length - chunk_idx))
		 {
			 while ((idx < chunkIds.length)&&(itr.hasNext()))
			 {
				 successorChunk = itr.next();
				 LogUtility.LogFile("successorChunk " + Long.toHexString(successorChunk) + " chunkIds[(int) idx] " + Long.toHexString(chunkIds[(int) idx]), LogUtility.LogLevels.LEVEL_LOG_HIGH);
				 if (successorChunk != chunkIds[(int) idx])
				 {
					 LogUtility.LogFile("chunk " + Long.toHexString(successorChunk) + " #not match ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
					 return -1;
				 }
				 else
				 {
					 //LogUtility.LogFile("#chunk " + Long.toHexString(successorChunk) + " match ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
				 }
				 idx++;
			 }
			 LogUtility.LogFile(" equal ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
			 return 0;
		 }
		/* for each chunk in successors list check if equals */
		 while (itr.hasNext())
		 {
			 successorChunk = itr.next();
			 LogUtility.LogFile("successorChunk " + Long.toHexString(successorChunk) + " len " + Integer.toString(PackChunking.chunkToLen(successorChunk)) + " chunkIds[(int) idx] " + Long.toHexString(chunkIds[(int) idx]) + " len " + Integer.toString(PackChunking.chunkToLen(chunkIds[(int) idx])) , LogUtility.LogLevels.LEVEL_LOG_HIGH);
			 if (successorChunk != chunkIds[(int) idx])
			 {
				 LogUtility.LogFile("chunk " + Long.toHexString(successorChunk) + " not match ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
				 return -1;
			 }
			 else
			 {
				 LogUtility.LogFile("chunk " + Long.toHexString(successorChunk) + " match ", LogUtility.LogLevels.LEVEL_LOG_HIGH);
			 }
			 idx++;
		 }
		 LogUtility.LogFile("returned len " + Long.toString(chunkIds.length - idx), LogUtility.LogLevels.LEVEL_LOG_HIGH);
		 return (int)(chunkIds.length - idx);
	}

	static boolean FindChunkInChain(long []chain,long chunk,ReferencedInteger chunk_idx)
	{
		int idx;

		if(chain == null)
		{
			chunk_idx.val = 0;
			return false;
		}
		chunk_idx.val = chain.length;
		idx = 0;
		for(int chainIdx = 0;chainIdx < chain.length;chainIdx++)
		{
			if(chain[chainIdx] == chunk)
			{
				chunk_idx.val = idx;
				return true;
			}
			idx++;
		}
		return false;
	}

	static boolean FindChainInChunksChainList(long[] chainList, long chainId, ReferencedInteger chain_idx)
	{
		int idx;

		if (chainList == null)
		{
			chain_idx.val = 0;
			return false;
		}
		chain_idx.val = chainList.length;

		for (int chainIdx = 0;chainIdx < chainList.length;chainIdx++)
		{
			if (chainList[chainIdx] == chainId)
			{
				chain_idx.val = chainIdx;
				return true;
			}
		}
		return false;
	}
	static boolean ChainIsPresentInChunkCB(ChunkCB chunkCB,long chain4Lookup)
	{
		for (int idx = 0; idx < chunkCB.ChainsListSize; idx++)
		{
			if (chunkCB.chains[idx] == chain4Lookup)
			{
				return true;
			}
		}
		return false;
	}
	boolean ChunkIsPresentInChain(Chain chain, long chunk)
	{
		for (int idx = 0; idx < chain.NumberOfEntries; idx++)
		{
			if (chain.chunkIds[idx] == chunk)
			{
				return true;
			}
		}
		return false;
	}
	static public void Restart()
	{
		try {
			fileManager.Restart();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	static public boolean SaveChain(List<Long> chunkList, int chunkListIdx, int LastChunkId, byte[] data, int offset,ChunkAndChainFileManager chunkAndChainFileManager)
	{
		ChunkCB chunkCB;
		ReferencedByteArray buf = new ReferencedByteArray();
		boolean presentChunks = false;
		int savedChunkListIdx = chunkListIdx;
		int idx;
		Chain chain4Lookup;

		if (chunkAndChainFileManager.chainId4Lookup == 0)
		{
			GetChainAsByteArray(new long[0], buf);
			chunkAndChainFileManager.chainId4Lookup = fileManager.AddUpdateChainFile(buf.val, chunkAndChainFileManager.chainId4Lookup);
			//                LogUtility.LogUtility.LogFile("****created new chain " + Integer.toString(chainId4Lookup), ModuleLogLevel);
			ChainsCreated++;
		}
		else
		{
			//                LogUtility.LogUtility.LogFile("****updating chain " + Integer.toString(chainId4Lookup), ModuleLogLevel);
			ChainsUpdated++;
		}

		for (; chunkListIdx <= LastChunkId; chunkListIdx++)
		{
			chunkCB = GetChunkCB(chunkList.get(chunkListIdx));
			if (chunkCB == null)
			{
				chunkCB = new ChunkCB();
				chunkCB.sha1 = PackChunking.chunkToSha1(chunkList.get(chunkListIdx));
				chunkCB.size = (long)PackChunking.chunkToLen(chunkList.get(chunkListIdx));
				chunkCB.fo = fileManager.AddNewChunk(data, (long)offset, chunkCB.size, chunkList.get(chunkListIdx));
				chunkCB.hint = PackChunking.GetChunkHint(data, (long)offset, chunkCB.size);
				chunkCB.chains = new long[1];
				chunkCB.chains[0] = chunkAndChainFileManager.chainId4Lookup;
				chunkCB.ChainsListSize = 1;
				LogUtility.LogFile("****creating new chunk CB " + Long.toHexString(chunkList.get(chunkListIdx)) + " " + Long.toString(chunkCB.size) + " " + " hint " + Integer.toString(chunkCB.hint) + " sha1 " + Long.toString(chunkCB.sha1), ModuleLogLevel);
				ChunksCreated++;
			}
			else if (!ChainIsPresentInChunkCB(chunkCB, chunkAndChainFileManager.chainId4Lookup))
			{
				presentChunks = true;
				long[] newChainList = new long[chunkCB.chains.length + 1];
				ByteArrayUtils.CopyBytes(chunkCB.chains,newChainList,0);
				newChainList[chunkCB.chains.length] = chunkAndChainFileManager.chainId4Lookup;
				chunkCB.chains = newChainList;
				chunkCB.ChainsListSize++;
				LogUtility.LogFile("****updating existing chunk CB " + Long.toString(chunkCB.size) + " " + Long.toHexString(chunkList.get(chunkListIdx)) + " hint " + Integer.toString(chunkCB.hint) + " sha1 " + Long.toString(chunkCB.sha1), ModuleLogLevel);
				ChainAdded2ExistingChunk++;
			}
			else
			{
				LogUtility.LogFile("****chain exists in chunk CB " + Long.toHexString(chunkList.get(chunkListIdx)) + " " + Long.toString(PackChunking.chunkToLen(chunkList.get(chunkListIdx))) + " chain " + Long.toString(chunkAndChainFileManager.chainId4Lookup) + " hint " + Integer.toString(chunkCB.hint) + " sha1 " + Long.toString(chunkCB.sha1), ModuleLogLevel);
				offset += (int)chunkCB.size;
				ChainExistInChunkChainsList++;
				continue;
			}
			offset += (int)chunkCB.size;
			ReferencedByteArray rb = new ReferencedByteArray();
			GetChunkCBAsByteArray(chunkCB, buf);
			fileManager.AddUpdateChunkCtrlFile(chunkList.get(chunkListIdx), buf.val, 0, (long)buf.val.length);
		}
		chain4Lookup = GetChain(chunkAndChainFileManager.chainId4Lookup);
		if (chain4Lookup == null)
		{
			LogUtility.LogFile("****cannot get chain!!! " + Long.toString(chunkAndChainFileManager.chainId4Lookup), ModuleLogLevel);
			CannotGetChain++;
			return presentChunks;
		}
		//            LogUtility.LogUtility.LogFile("prepare chain, now " + Integer.toString(chain4Lookup.NumberOfEntries) + " ( " + Integer.toString(chain4Lookup.chunkIds.Length) + ")" + " will be added " + Integer.toString(chunkList.Count-savedChunkListIdx), ModuleLogLevel);
		long[] newChunkList = new long[(int) (chain4Lookup.NumberOfEntries + ((LastChunkId+1) - savedChunkListIdx))];
		ByteArrayUtils.CopyBytes(chain4Lookup.chunkIds, newChunkList, 0);
		idx = (int)chain4Lookup.NumberOfEntries;
		for (chunkListIdx = savedChunkListIdx; chunkListIdx <= LastChunkId; chunkListIdx++)
		{
			newChunkList[idx++] = chunkList.get(chunkListIdx);
			LogUtility.LogFile("****new chunk " + Long.toHexString(newChunkList[idx-1]), ModuleLogLevel);
		}
		chain4Lookup.chunkIds = newChunkList;
		chain4Lookup.NumberOfEntries = (long)newChunkList.length;
		GetChainAsByteArray(chain4Lookup.chunkIds, buf);
		//save on disk
		chunkAndChainFileManager.chainId4Lookup = fileManager.AddUpdateChainFile(buf.val, chunkAndChainFileManager.chainId4Lookup);
		return presentChunks;
	}
	public int GetChainAfterChunk(long chunk, List<ChunkListAndChainId> chainsChunksList)
	{
		Chain chain = GetChain(chainId4Lookup);

		if (chain == null)
		{
			LogUtility.LogFile("Cannot get chain!!!! " + Long.toString(chainId4Lookup), ModuleLogLevel);
			CannotGetChain++;
			return -1;
		}
		//LogUtility.LogUtility.LogFile("chain " + Integer.toString(ch) + " chunks " + Integer.toString(chain.chunkIds.Length), ModuleLogLevel);
		ReferencedInteger chunk_idx = new ReferencedInteger();
		if (!FindChunkInChain(chain.chunkIds, chunk, chunk_idx))
		{
			//                    LogUtility.LogUtility.LogFile("Cannot find chunk!!!! " + Integer.toString(chunkList[chunkListIdx]) + " in chain " + Integer.toString(ch), ModuleLogLevel);
			CannotFindChunkInChain++;
			return -2;
		}
		ChunkListAndChainId chunkListAndChainId = new ChunkListAndChainId();
		chunkListAndChainId.chainId = chainId4Lookup;
		chunkListAndChainId.chunks = chain.chunkIds;
		chunkListAndChainId.firstChunkIdx = (long)chunk_idx.val + 1;
		chainsChunksList.add(chunkListAndChainId);
		return 0;
	}
	public int ChainMatch(List<Long> chunkList,int chunkListIdx,List<ChunkListAndChainId> chainsChunksList,List<Long> sentChainsList)
	{
		Chain chain;
		long longest_chain = 0;
		int foundChainLength;
		ChunkCB chunkCB;
		ChunkListAndChainId chunkListAndChainId;
		boolean AtLeastEqualChainFound = false;
		long currentChunk = chunkList.get(chunkListIdx);

		chunkCB = GetChunkCB(currentChunk); 
		if (chunkCB == null)
		{
			LogUtility.LogFile("Cannot get chunk " + Long.toString(currentChunk), LogUtility.LogLevels.LEVEL_LOG_MEDIUM);
			return -1;
		}
		LogUtility.LogFile("****chunk to match found " + Integer.toString(PackChunking.chunkToLen(currentChunk)) + " " + Integer.toString(chunkListIdx), ModuleLogLevel);
		for (int chainIdx = 0;chainIdx < chunkCB.chains.length;chainIdx++)
		{
			if (sentChainsList.contains(chunkCB.chains[chainIdx]))
			{
				AtLeastEqualChainFound = true;
				LogUtility.LogFile("Chain already sent " + Long.toString(chunkCB.chains[chainIdx]), ModuleLogLevel);
				continue;
			}
			chain = GetChain(chunkCB.chains[chainIdx]);
			if (chain == null)
			{
				LogUtility.LogFile("Cannot get chain!!!! " + Long.toString(chunkCB.chains[chainIdx]), ModuleLogLevel);
				CannotGetChain++;
				continue;
			}
			//LogUtility.LogUtility.LogFile("chain " + Integer.toString(ch) + " chunks " + Integer.toString(chain.chunkIds.Length), ModuleLogLevel);
			ReferencedInteger chunk_idx = new ReferencedInteger();
			if (!FindChunkInChain(chain.chunkIds, currentChunk, chunk_idx))
			{
				//                    LogUtility.LogUtility.LogFile("Cannot find chunk!!!! " + Integer.toString(chunkList[chunkListIdx]) + " in chain " + Integer.toString(ch), ModuleLogLevel);
				CannotFindChunkInChain++;
				continue;
			}
			//LogUtility.LogUtility.LogFile("chunk id in chain " + Integer.toString(chunk_idx), ModuleLogLevel);
			foundChainLength = CompareChains(chunkList, chunkListIdx+1, chain.chunkIds, chunk_idx.val + 1);
			switch (foundChainLength)
			{
			case -1: /* different */
				//Vadim 10/01/13 chainId4Lookup = 0;
				chainId4Lookup = 0;//Vadim 28/01/13
				DifferentChains++;
				break;
			case 0:/* match but no longer */
				LogUtility.LogFile("equal chain found " + Long.toString(longest_chain) , ModuleLogLevel);
				FoundEqualChains++;
				AtLeastEqualChainFound = true;
				if (chainId4Lookup == 0)
				{
					//                            LogUtility.LogUtility.LogFile("equal chain found " + Integer.toString(longest_chain), ModuleLogLevel);
					chainId4Lookup = chunkCB.chains[chainIdx];
				}
				break;
			default:
			{
				LongerChainsFound++;
				if (foundChainLength > longest_chain)
				{
					longest_chain = (long)foundChainLength;
					chainId4Lookup = chunkCB.chains[chainIdx];
				}
				AtLeastEqualChainFound = true;
				LogUtility.LogFile("****longer chain found " + Long.toString(longest_chain), ModuleLogLevel);
				boolean chainFound = false;
				Iterator<ChunkListAndChainId> itr = chainsChunksList.iterator();
				while (itr.hasNext())
				{
					ChunkListAndChainId c = itr.next();
					if (c.chainId == chunkCB.chains[chainIdx])
					{
						LogUtility.LogFile("****already in list", ModuleLogLevel);
						chainFound = true;
					}
				}
				if (!chainFound)
				{
					chunkListAndChainId = new ChunkListAndChainId();
					chunkListAndChainId.chainId = chunkCB.chains[chainIdx];
					chunkListAndChainId.chunks = chain.chunkIds;
					chunkListAndChainId.firstChunkIdx = (long)chunk_idx.val + (long)(chunkList.size() - chunkListIdx);
					chainsChunksList.add(chunkListAndChainId);
					LogUtility.LogFile("****added to list, first chunk " + Long.toString(chunkListAndChainId.firstChunkIdx), ModuleLogLevel);
				}
			}
			break;
			}
		}
		if (!AtLeastEqualChainFound)
		{
			return -1;
		}
		//chainId4Lookup = 0;
		return (int)longest_chain;
	}
	static public byte GetChunkHint(long chunk)
	{
		ChunkCB chunkCB = GetChunkCB(chunk);
		
		if (chunkCB != null)
		{
			return chunkCB.hint;
	    }
		return 0;
	}

	static public byte[] GetChunkData(long chunkId)
	{
		ChunkCB chunkCB = GetChunkCB(chunkId);
		
		if (chunkCB == null)
		{
			return null;
		}
		return fileManager.GetChunk(chunkCB.fo, chunkCB.size);
	}

	public void OnDispose()
	{
	}

	static long GetChunkCB(byte[] buffer, long offset, ChunkCB chunkCB)
	{
		ReferencedLong rl = new ReferencedLong();
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Uint(buffer, offset, rl);
		chunkCB.size = rl.val;
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, offset, rl);
		chunkCB.sha1 = rl.val;
		chunkCB.hint = buffer[(int) offset++];
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, offset, rl);
		chunkCB.fo.file = rl.val;
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, offset, rl);
		chunkCB.fo.offset = rl.val;
		offset += ByteArrayScalarTypeConversionLib.ByteArray2Uint(buffer, offset, rl);
		chunkCB.ChainsListSize = rl.val;
		chunkCB.chains = new long[(int) chunkCB.ChainsListSize];
		return offset;  
	}

	static ChunkCB GetChunkCBAndChainList(byte[] buffer, long offset)
	{
		ChunkCB chunkCB = new ChunkCB();
		ReferencedLong rl = new ReferencedLong();
		offset = GetChunkCB(buffer, offset, chunkCB);
		long []chains = new long[(int) chunkCB.ChainsListSize];

		for (int idx = 0; idx < chunkCB.ChainsListSize; idx++)
		{
			offset += ByteArrayScalarTypeConversionLib.ByteArray2Long(buffer, offset, rl);
			chains[idx] = rl.val;
			ChainsLoaded++;
		}
		chunkCB.chains = chains;
		return chunkCB;
	}

	static ChunkCB GetChunkCB(long chunk)
	{
		ChunkCB chunkCB;
		//string filename = "datafiles" + "\\"+ FileManager.FileManager.LongName2StringName(chunk, (byte)FileManager.FileTypes.CHUNK_CTRL_FILE_TYPE);
		try
		{
			byte[] buff = fileManager.ReadFile(chunk);
			if (buff == null)
			{
				return null;
			}
			chunkCB = GetChunkCBAndChainList(buff, 0); 
			if (chunkCB != null)
			{
				ChunkExists++;
				return chunkCB;
			}
		}
		catch (Exception exc)
		{
			LogUtility.LogException("",exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
		}
		return null;
	}

	public static void Flush()
	{
		NonRamFileManager.OnExiting();
	}

	static public void Init()
	{
		//fileManager = new FileManager.FileManager("F:\\datafiles");
		LogUtility.LogFile("ChunkAndChainFileManager:Init", LogUtility.LogLevels.LEVEL_LOG_HIGH);
	}
	public String GetDebugInfo()
	{
		return "ChunksCreated " + Long.toString(ChunksCreated) + " ChunkExists " + Long.toString(ChunkExists) + " ChainsCreated " + Long.toString(ChainsCreated) + " ChainsUpdated " + Long.toString(ChainsUpdated) + " FoundEqualChains " + Long.toString(FoundEqualChains) + " DifferentChains " + Long.toString(DifferentChains) + " LongerChainsFound " + Long.toString(LongerChainsFound) + " CannotFindChunkInChain " + Long.toString(CannotFindChunkInChain) + " ChainExistInChunkChainsList " + Long.toString(ChainExistInChunkChainsList) + " ChainAdded2ExistingChunk " + Long.toString(ChainAdded2ExistingChunk) + " CannotGetChain " + Long.toString(CannotGetChain) + " ChainsReturned " + Long.toString(ChainsReturned);
	}
}
