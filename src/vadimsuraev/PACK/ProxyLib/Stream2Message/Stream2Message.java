package vadimsuraev.PACK.ProxyLib.Stream2Message;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.PACK.PackMsg.*;
import vadimsuraev.ReferencedTypes.ReferencedByte;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;

public class Stream2Message
{
	public static LogUtility.LogLevels ModuleLogLevel = LogUtility.LogLevels.LEVEL_LOG_HIGH;
	enum Receiver_State_e
	{
		RECEIVER_HEADER_STATE,
		RECEIVER_MSG_BODY_STATE
	};
    byte mState;
	long mBufferIdx;
	byte[] mBuff;
	ReferencedLong mExpectedNumberOfBytes;
	ReferencedByte mMsgKind;
	ReferencedByte mFlags;

	public Stream2Message()
	{
		InitRxStateMachine();
	}

	void InitRxStateMachine()
	{
		mState = (byte)Receiver_State_e.RECEIVER_HEADER_STATE.ordinal();
		mBufferIdx = 0;
		mBuff = null;
		mExpectedNumberOfBytes = new ReferencedLong();
		mExpectedNumberOfBytes.val = 0;
		mMsgKind = new ReferencedByte();
		mMsgKind.val = (byte)PackMsg.MsgKind_e.PACK_DUMMY_MSG_KIND.ordinal();
		mFlags = new ReferencedByte();
		mFlags.val = 0;
	}

	protected long CopyBytes(byte[] src, byte[] dst, long src_offset, long dst_offset, long number_of_bytes_2_copy)
	{
		long idx = 0;

		for (idx = 0; ((dst_offset + idx) < dst.length) && ((src_offset + idx) < src.length) && (idx < number_of_bytes_2_copy); idx++)
		{
			dst[(int) (dst_offset + idx)] = src[(int) (src_offset + idx)];
		}
		return idx;
	}

	public void OnDataByteMode(byte[] packet, int room_space)
	{
		long bytes_copied = 0;
		long overall_bytes_copied = 0;
		ReferencedInteger offset =  new ReferencedInteger();
		byte[] returnMsg = null;
		
		offset.val = 0;

		LogUtility.LogFile("processing " + Integer.toString(packet.length) + " bytes", ModuleLogLevel);
		while (overall_bytes_copied < packet.length)
		{
			LogUtility.LogFile("remain " + Long.toString(packet.length - overall_bytes_copied) + " bytes", ModuleLogLevel);
			switch (Receiver_State_e.values()[mState])
			{
			case RECEIVER_HEADER_STATE:
				LogUtility.LogFile("HEADER STATE", ModuleLogLevel);
			if (mBuff == null)
			{
				mBuff = new byte[(int) PackMsg.GetMsgHeaderSize()];
				mBufferIdx = 0;
			}
			bytes_copied = CopyBytes(packet, mBuff, overall_bytes_copied, mBufferIdx, PackMsg.GetMsgHeaderSize() - mBufferIdx);
			overall_bytes_copied += bytes_copied;
			mBufferIdx += bytes_copied;
			LogUtility.LogFile("copied " + Long.toString(bytes_copied) + " bytes", ModuleLogLevel);
			if (mBufferIdx != PackMsg.GetMsgHeaderSize())
			{
				LogUtility.LogFile("still missing " + Long.toString(PackMsg.GetMsgHeaderSize() - mBufferIdx) + " bytes", ModuleLogLevel);
				break;
			}
			if (PackMsg.DecodeMsg(mBuff, mExpectedNumberOfBytes, mFlags, mMsgKind, offset) != 0)
			{
				InitRxStateMachine();
				LogUtility.LogFile("unable to decode message", ModuleLogLevel);
				// process the rest of buffer, write above lines as a function
				break;
			}
			mBuff = null;
			mBuff = new byte[(int) mExpectedNumberOfBytes.val];
			mBufferIdx = 0;
			mState = (byte)Receiver_State_e.RECEIVER_MSG_BODY_STATE.ordinal();
			break;
			case RECEIVER_MSG_BODY_STATE:
				bytes_copied = CopyBytes(packet, mBuff, overall_bytes_copied, mBufferIdx, mExpectedNumberOfBytes.val - mBufferIdx);
			LogUtility.LogFile("BODY STATE  " + Long.toString(bytes_copied) + " bytes copied", ModuleLogLevel);
			mBufferIdx += bytes_copied;
			overall_bytes_copied += bytes_copied;
			// calculate remaining number of bytes in packet
			if (mBufferIdx == mExpectedNumberOfBytes.val)
			{
				switch(PackMsg.MsgKind_e.values()[mMsgKind.val])
				{
				case PACK_PRED_MSG_KIND:
					returnMsg = ProcessPredMsg(mBuff, 0,mFlags.val, room_space);
					break;
				case PACK_PRED_ACK_MSG_KIND:
					LogUtility.LogFile("PRED ACK msg", ModuleLogLevel);
					returnMsg = ProcessPredAckMsg(mBuff, 0,mFlags.val, room_space);
					break;
				case PACK_DATA_MSG_KIND:
					LogUtility.LogFile("DATA msg", ModuleLogLevel);
					returnMsg = ProcessDataMsg(mBuff, 0,mFlags.val, room_space);
					break;
				case PACK_FINALLY_PROCESSED_DATA_MSG_KIND:
					LogUtility.LogFile("Finally processed DATA", ModuleLogLevel);
					returnMsg = ProcessFinallyProcessedDataMsg(mBuff, 0,mFlags.val, room_space);
					break;
				}
				if (returnMsg != null)
				{
					OnMsgRead4Tx(returnMsg,false);
				}
				InitRxStateMachine();
			}
			else
			{
				LogUtility.LogFile("mBufferIdx != mExpectedNumberOfBytes.val " + Long.toString(mBufferIdx) + " " + Long.toString(mExpectedNumberOfBytes.val), ModuleLogLevel);
			}
			break;
			default:
				overall_bytes_copied += bytes_copied;
				LogUtility.LogFile("Default case state", ModuleLogLevel);
				break;
			}
		}
	}
	public void OnDataReceived(byte[] data,int offset,int length)
	{
		return;
	}
	public void OnMsgRead4Tx(byte[] msg,boolean dummy)
	{
		return;
	}
	public byte []ProcessDataMsg(byte []packet,int offset,byte Flags,int room_space)
	{
		return null;
	}
	public byte []ProcessPredMsg(byte []packet,int offset,byte Flags,int room_space)
	{
		return null;
	}
	public byte []ProcessPredAckMsg(byte[] packet, int offset,byte Flags,int dummy_room_space)
	{
		return null;
	}
	public byte []ProcessFinallyProcessedDataMsg(byte []packet,int offset,byte Flags,int room_space)
	{
		return null;
	}
	public String GetDebugInfo()
	{
		String debugInfo = "";
		debugInfo += "mBufferIdx " + Long.toString(mBufferIdx) + " mExpectedNumberOfBytes " + Long.toString(mExpectedNumberOfBytes.val) + " mFlags " + Byte.toString(mFlags.val) + " mMsgKind " + Byte.toString(mMsgKind.val) + " mState " + Integer.toString(mState);
		return debugInfo;
	}
}
