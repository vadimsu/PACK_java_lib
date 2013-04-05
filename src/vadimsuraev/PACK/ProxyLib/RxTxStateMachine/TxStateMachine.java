package vadimsuraev.PACK.ProxyLib.RxTxStateMachine;

import java.net.SocketAddress;
import vadimsuraev.LogUtility.*;
import vadimsuraev.ByteArrayScalarTypeConversionLib.ByteArrayScalarTypeConversionLib;
import vadimsuraev.ByteArrayUtils.*;

public class TxStateMachine extends FramingStateMachine
{
    
    public TxStateMachine(SocketAddress id)
    {
    	super(id);
        /*m_Header[0] = (byte)(PackPreamble & 0xFF);
        m_Header[1] = (byte)((PackPreamble >> 8) & 0xFF);
        m_Header[2] = (byte)((PackPreamble >> 16) & 0xFF);
        m_Header[3] = (byte)((PackPreamble >> 24) & 0xFF);*/
    	ByteArrayScalarTypeConversionLib.Uint2ByteArray(m_Header, 0, PackPreamble);
    }

    void GoHeaderStateIfDummy()
    {
        if (m_State == (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal())
        {
            m_State = (byte)PackTxRxState_e.PACK_HEADER_STATE.ordinal();
        }
    }

    public void SetLength(long length)
    {
        /*m_Header[4] = (byte)(length & 0xFF);
        m_Header[5] = (byte)((length >> 8) & 0xFF);
        m_Header[6] = (byte)((length >> 16) & 0xFF);
        m_Header[7] = (byte)((length >> 24) & 0xFF);*/
    	ByteArrayScalarTypeConversionLib.Uint2ByteArray(m_Header, 4, length);
        GoHeaderStateIfDummy();
    }

    public void SetKind(byte kind)
    {
        m_Header[8] = kind;
        GoHeaderStateIfDummy();
    }

    public byte[] GetBytes()
    {
        try
        {
            LogUtility.LogFile(m_Id.toString() + " GetBytes Tx sm: state " + Integer.toString(m_State) + " byte counter " + Long.toString(m_ByteCounter), ModuleLogLevel);
            //LogUtility.LogFile(Environment.StackTrace);
            switch (PackTxRxState_e.values()[m_State])
            {
                case PACK_DUMMY_STATE:
                    m_State = (byte)PackTxRxState_e.PACK_HEADER_STATE.ordinal();
                    LogUtility.LogFile(m_Id.toString() + " Went header state", ModuleLogLevel);
                    return m_Header;
                case PACK_HEADER_STATE:
                    return m_Header;
                case PACK_BODY_STATE:
                    if (m_ByteCounter == m_Body.length)
                    {
                        return null;
                    }
                    return m_Body;
            }
            return null;
        }
        catch (Exception exc)
        {
            try {
				throw new Exception("Excetion in GetBytes " + exc.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		return null;
    }

    public byte[] GetBytes(int limit)
    {
        try
        {
            LogUtility.LogFile(m_Id.toString() + " GetBytes (limited) Tx sm: state " + Integer.toString(m_State) + " byte counter " + Long.toString(m_ByteCounter), ModuleLogLevel);
            byte []buff = null;
            switch (PackTxRxState_e.values()[m_State])
            {
                case PACK_DUMMY_STATE:
                    m_State = (byte)PackTxRxState_e.PACK_HEADER_STATE.ordinal();
                    LogUtility.LogFile(m_Id.toString() + " Went header state", ModuleLogLevel);
                    m_State = (byte)PackTxRxState_e.PACK_HEADER_STATE.ordinal();
                case PACK_HEADER_STATE:
                	if(limit >= ((m_Body.length - m_ByteCounter) + GetHeaderLength()))
                	{
                		LogUtility.LogFile(m_Id.toString() + " HEADER state: allocating " + Long.toString((m_Body.length - m_ByteCounter) + GetHeaderLength()), ModuleLogLevel);
                		buff = new byte[(int) (m_Body.length - m_ByteCounter) + GetHeaderLength()];
                	}
                	else
                	{
                		LogUtility.LogFile(m_Id.toString() + " HEADER state: allocating " + Long.toString(limit), ModuleLogLevel);
                		buff = new byte[limit];
                	}
                	ByteArrayUtils.CopyBytes(m_Header,0,buff,0,m_Header.length);
                	if(limit > m_Body.length)
                	{
                		m_State = (byte)PackTxRxState_e.PACK_WHOLE_PACKET_STATE.ordinal();
                	}
                	else
                	{
                		m_State = (byte)PackTxRxState_e.PACK_BODY_STATE.ordinal();
                	}
                case PACK_BODY_STATE:
                case PACK_WHOLE_PACKET_STATE:
                    if (m_ByteCounter == m_Body.length)
                    {
                        return buff;
                    }
                    if(buff != null)
                    {
                    	if((limit - GetHeaderLength()) >= (m_Body.length - m_ByteCounter))
                    	{
                    		LogUtility.LogFile(m_Id.toString() + " BODY state1: copying " + Long.toString(m_Body.length - m_ByteCounter), ModuleLogLevel);
                    		ByteArrayUtils.CopyBytes(m_Body,(int)m_ByteCounter,buff,m_Header.length,(int)(m_Body.length - m_ByteCounter));
                    		//m_ByteCounter += (m_Body.length - m_ByteCounter);
                    	}
                    	else
                    	{
                    		LogUtility.LogFile(m_Id.toString() + " BODY state2: copying " + Long.toString(limit - GetHeaderLength()) + " offset " + Long.toString(m_ByteCounter), ModuleLogLevel);
                    		ByteArrayUtils.CopyBytes(m_Body,(int)m_ByteCounter,buff,m_Header.length,(int)(limit - GetHeaderLength()));
                    		//m_ByteCounter += (limit - GetHeaderLength());
                    	}
                    	return buff;
                    }
                    if(limit >= (m_Body.length - m_ByteCounter))
                    {
                    	LogUtility.LogFile(m_Id.toString() + " BODY state1: allocating " + Long.toString(m_Body.length - m_ByteCounter) + " offset " + Long.toString(m_ByteCounter), ModuleLogLevel);
                    	buff = new byte[(int) (m_Body.length - m_ByteCounter)];
                    }
                    else
                    {
                    	LogUtility.LogFile(m_Id.toString() + " BODY state2: allocating " + Long.toString(limit) + " offset " + Long.toString(m_ByteCounter), ModuleLogLevel);
                    	buff = new byte[limit];
                    }
                    ByteArrayUtils.CopyBytes(m_Body,(int)m_ByteCounter,buff,0,buff.length);
                    return buff;
            }
        }
        catch (Exception exc)
        {
            try {
				throw new Exception("Excetion in GetBytes " + exc.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		return null;
    }
    public boolean IsInBody()
    {
        return ((m_State == (byte)PackTxRxState_e.PACK_BODY_STATE.ordinal())||(m_State == (byte)PackTxRxState_e.PACK_WHOLE_PACKET_STATE.ordinal()));
    }
    public boolean IsWholeMessage()
    {
        return (m_State == (byte)PackTxRxState_e.PACK_WHOLE_PACKET_STATE.ordinal());
    }
    public int GetHeaderLength()
    {
        return m_Header.length;
    }
    public boolean OnTxComplete(long bytes_count)
    {
        try
        {
            LogUtility.LogFile(m_Id.toString() + " OnTxComplete Tx sm: state " + Integer.toString(m_State) + " byte counter " + Long.toString(m_ByteCounter), ModuleLogLevel);
            switch (PackTxRxState_e.values()[m_State])
            {
                case PACK_HEADER_STATE:
                    //m_ByteCounter += bytes_count;
                    if (bytes_count == m_Header.length)
                    {
                        m_State = (byte)PackTxRxState_e.PACK_BODY_STATE.ordinal();
                        //m_ByteCounter = 0;
                    }
                    else
                    {
                        m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal();
                    }
                    break;
                case PACK_BODY_STATE:
                    if (bytes_count != m_Body.length)
                    {
                        return false;
                    }
                    m_ByteCounter += bytes_count;
                    if (m_ByteCounter == m_Body.length)
                    {
                        // m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE;
                    }
                    break;
                case PACK_WHOLE_PACKET_STATE:
                    if (bytes_count != (m_Header.length + m_Body.length))
                    {
                        return false;
                    }
                    m_ByteCounter += bytes_count;
                    break;
            }
            return true;
        }
        catch (Exception exc)
        {
            try {
				throw new Exception("exception in OnTxComplete " + exc.getMessage());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		return false;
    }

    public void SetMsgBody(byte[] body)
    {
        m_Body = body;
    }
};
