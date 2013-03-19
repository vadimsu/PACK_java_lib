package vadimsuraev.PACK.ProxyLib.RxTxStateMachine;
import vadimsuraev.ByteArrayScalarTypeConversionLib.ByteArrayScalarTypeConversionLib;
import vadimsuraev.LogUtility.*;
import vadimsuraev.ReferencedTypes.ReferencedLong;

import java.net.SocketAddress;

public class RxStateMachine extends FramingStateMachine
{
	OnMessageCallback onMsgReceived;
    public RxStateMachine(SocketAddress id)
    {
    	super(id);
        onMsgReceived = null;
    }

    long GetLength()
    {
        /*long length = m_Header[4];
        length |= (long)(m_Header[5] << 8);
        length |= (long)(m_Header[6] << 16);
        length |= (long)(m_Header[7] << 24);*/
    	ReferencedLong length = new ReferencedLong();
        ByteArrayScalarTypeConversionLib.ByteArray2Uint(m_Header, 4, length);
        return length.val;
    }
    public void SetCallback(OnMessageCallback onMessageReceived)
    {
        onMsgReceived = onMessageReceived;
    }
    public byte GetKind()
    {
        return m_Header[8];
    }
    void CopyBytes(byte []src,byte []dst,int src_offset,int dst_offset,int bytes2copy)
    {
        for(int idx = 0;idx < bytes2copy;idx++)
        {
            dst[dst_offset + idx] = src[src_offset + idx];
        }
    }
    public void OnRxComplete(byte []buff,int RealSize)
    {
        try
        {
            int bytes2process = RealSize;
            int remaining_bytes;
            LogUtility.LogFile("OnRxComplete " + Integer.toString(RealSize), ModuleLogLevel);
            while (bytes2process > 0)
            {
                LogUtility.LogFile("State " + Integer.toString(m_State) + " bytes2process " + Integer.toString(bytes2process) + " byte counter " + Long.toString(m_ByteCounter), ModuleLogLevel);
                switch (PackTxRxState_e.values()[m_State])
                {
                    case PACK_DUMMY_STATE:
                    case PACK_HEADER_STATE:
                        m_State = (byte)PackTxRxState_e.PACK_HEADER_STATE.ordinal();
                        remaining_bytes = (int)(m_Header.length - m_ByteCounter);
                        if (remaining_bytes >= bytes2process)
                        {
                            CopyBytes(buff, m_Header, RealSize - bytes2process, (int)m_ByteCounter, bytes2process);
                            m_ByteCounter += (long)bytes2process;
                            bytes2process = 0;
                            LogUtility.LogFile("All bytes processed ", ModuleLogLevel);
                        }
                        else
                        {
                            CopyBytes(buff, m_Header, RealSize - bytes2process, (int)m_ByteCounter, remaining_bytes);
                            m_ByteCounter += (long)remaining_bytes;
                            bytes2process -= remaining_bytes;
                            LogUtility.LogFile("After copying remain " + Integer.toString(bytes2process), ModuleLogLevel);
                        }

                        if (m_ByteCounter == m_Header.length)
                        {
                            /*long preamble = m_Header[0];
                            preamble |= (long)(m_Header[1] << 8);
                            preamble |= (long)(m_Header[2] << 16);
                            preamble |= (long)(m_Header[3] << 24);*/
                        	ReferencedLong preamble = new ReferencedLong();
                        	ByteArrayScalarTypeConversionLib.ByteArray2Uint(m_Header, 0, preamble);
                            LogUtility.LogFile("Header received ", ModuleLogLevel);
                            if (preamble.val != PackPreamble)
                            {
                                m_ByteCounter = 0;
                                m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal();
                                LogUtility.LogFile("PREAMBLE MISMATCH!!!!!!! " + Long.toHexString(m_Header[0]) + " " + Long.toHexString(m_Header[1]) + " " + Long.toHexString(m_Header[2]) + " " + Long.toHexString(m_Header[3]), ModuleLogLevel);
                                return;
                            }
                            m_State = (byte)PackTxRxState_e.PACK_BODY_STATE.ordinal();
                            if (m_Body == null)
                            {
                                try
                                {
                                    LogUtility.LogFile("Allocating body " + Long.toString(GetLength()), ModuleLogLevel);
                                    m_Body = new byte[(int) GetLength()];
                                }
                                catch (Exception exc)
                                {
                                    LogUtility.LogException(m_Id.toString(),exc, LogUtility.LogLevels.LEVEL_LOG_HIGH);
                                }
                            }
                            m_ByteCounter = 0;
                        }
                        break;
                    case PACK_BODY_STATE:
                        remaining_bytes = (int)(m_Body.length - m_ByteCounter);
                        if (remaining_bytes >= bytes2process)
                        {
                            CopyBytes(buff, m_Body, RealSize - bytes2process, (int)m_ByteCounter, bytes2process);
                            m_ByteCounter += (long)bytes2process;
                            bytes2process = 0;
                            LogUtility.LogFile("All bytes copyied ", ModuleLogLevel);
                        }
                        else
                        {
                            CopyBytes(buff, m_Body, RealSize - bytes2process, (int)m_ByteCounter, remaining_bytes);
                            m_ByteCounter += (long)remaining_bytes;
                            bytes2process -= remaining_bytes;
                            LogUtility.LogFile("After copying remain " + Integer.toString(bytes2process), ModuleLogLevel);
                        }
                        if (m_ByteCounter == m_Body.length)
                        {
                            //m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE;
                            LogUtility.LogFile("Message received", ModuleLogLevel);
                            if (onMsgReceived != null)
                            {
                                onMsgReceived.OnMsgReceived();
                            }
                        }
                        break;
                }
            }
            LogUtility.LogFile("Leaving OnRxComplete", ModuleLogLevel);
        }
        catch (Exception exc)
        {
            try {
				throw new Exception("Exception in OnRxComplete", exc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    public byte[] GetMsgBody()
    {
        return m_Body;
    }

    public long GetByteCounter()
    {
        return m_ByteCounter;
    }
    public long GetExpectedBytes()
    {
        if (m_Body == null)
        {
            return 0;
        }
        return (long)m_Body.length;
    }
};
