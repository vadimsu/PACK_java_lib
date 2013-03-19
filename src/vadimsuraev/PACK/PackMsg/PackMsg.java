package vadimsuraev.PACK.PackMsg;

import vadimsuraev.LogUtility.LogUtility;
import vadimsuraev.ReferencedTypes.ReferencedByte;
import vadimsuraev.ReferencedTypes.ReferencedInteger;
import vadimsuraev.ReferencedTypes.ReferencedLong;
import vadimsuraev.ByteArrayScalarTypeConversionLib.*;

public class PackMsg
{
    public static long PackPreamble = 0x5AA57FF7;
    public static byte LastChunkFlag = 0x1;

    public enum MsgKind_e
    {
        PACK_DUMMY_MSG_KIND,
        PACK_PRED_MSG_KIND,
        PACK_PRED_ACK_MSG_KIND,
        PACK_DATA_MSG_KIND,
        PACK_FINALLY_PROCESSED_DATA_MSG_KIND,
        PACK_MSG_NUMBER
    };

    public static byte[] AllocateMsgAndBuildHeader(long DataSize,byte Flags,byte MsgKind,ReferencedInteger offset)
    {
        byte[] msg = new byte[(int) (Integer.SIZE/8 + Byte.SIZE/8 + Byte.SIZE/8 + Integer.SIZE/8 + DataSize)];
        offset.val = 0;
        offset.val += (int)ByteArrayScalarTypeConversionLib.Uint2ByteArray(msg, (long)offset.val, PackPreamble);
        msg[(int) offset.val++] = MsgKind;
        msg[(int) offset.val++] = Flags;
        offset.val += (int)ByteArrayScalarTypeConversionLib.Uint2ByteArray(msg, (long)offset.val, DataSize);
        return msg;
    }
    public static long GetMsgHeaderSize()
    {
        return (Integer.SIZE/8 + Byte.SIZE/8 + Byte.SIZE/8 + Integer.SIZE/8);
    }
    public static int DecodeMsg(byte []msg,ReferencedLong DataSize, ReferencedByte Flags,ReferencedByte MsgKind, ReferencedInteger offset)
    {
        ReferencedLong packPreamble = new ReferencedLong();
        offset.val = 0;
        offset.val += (int)ByteArrayScalarTypeConversionLib.ByteArray2Uint(msg, (long)offset.val, packPreamble);
        if (packPreamble.val != PackPreamble)
        {
            MsgKind.val = (byte)MsgKind_e.PACK_DUMMY_MSG_KIND.ordinal();
            DataSize.val = 0;
            Flags.val = 0;
            LogUtility.LogFile("Preamble mismatch recv: " + Long.toHexString(PackPreamble) + " req " + Long.toHexString(packPreamble.val), LogUtility.LogLevels.LEVEL_LOG_HIGH);
            return 1;
        }
        MsgKind.val = msg[offset.val++];
        Flags.val = msg[offset.val++];
        switch (MsgKind_e.values()[MsgKind.val])
        {
            case PACK_DATA_MSG_KIND:
            case PACK_FINALLY_PROCESSED_DATA_MSG_KIND:
            case PACK_PRED_ACK_MSG_KIND:
            case PACK_PRED_MSG_KIND:
                break;
            default:
            	LogUtility.LogFile("Unknown message kind " + Long.toHexString(MsgKind.val), LogUtility.LogLevels.LEVEL_LOG_HIGH);
                DataSize.val = 0;
                return 2;
        }
        offset.val += (int)ByteArrayScalarTypeConversionLib.ByteArray2Uint(msg, (long)offset.val, DataSize);
        return 0;
    }
}
