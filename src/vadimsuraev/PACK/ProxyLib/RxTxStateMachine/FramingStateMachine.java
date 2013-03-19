/*using System;
using System.Collections.Generic;
using System.Net;
using System.Linq;
using System.Text;*/
package vadimsuraev.PACK.ProxyLib.RxTxStateMachine;
import java.net.SocketAddress;

import vadimsuraev.LogUtility.LogUtility;

public class FramingStateMachine
{
 	enum PackTxRxState_e
    {
        PACK_DUMMY_STATE,
        PACK_HEADER_STATE,
        PACK_BODY_STATE,
        PACK_WHOLE_PACKET_STATE
    };
        
    public static LogUtility.LogLevels ModuleLogLevel = LogUtility.LogLevels.LEVEL_LOG_MEDIUM;
    protected final long PackPreamble = 0xA57F5AF7;
    protected long m_ByteCounter;
    protected byte[] m_Header;
    protected byte[] m_Body;
    protected byte m_State;
    protected SocketAddress m_Id;

    public FramingStateMachine(SocketAddress id)
    {
        m_ByteCounter = 0;
        m_Body = null;
        m_Header = new byte[9];
        m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal();
        m_Id = id;
    }
    public boolean IsTransactionCompleted()
    {
        if ((m_State == (byte)PackTxRxState_e.PACK_BODY_STATE.ordinal()) &&
            (m_Body != null) &&
            (m_ByteCounter == m_Body.length))
        {
            return true;
        }
        else if ((m_State == (byte)PackTxRxState_e.PACK_WHOLE_PACKET_STATE.ordinal()) &&
                (m_Body != null) &&
                (m_ByteCounter == (m_Header.length + m_Body.length)))
        {
            return true;
        }
        return false;
    }
    public void ClearMsgBody()
    {
        m_Body = null;
        m_ByteCounter = 0;
        m_State = (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal();
    }
    public boolean IsBusy()
    {
        return (m_State != (byte)PackTxRxState_e.PACK_DUMMY_STATE.ordinal());
    }
    public void SetEndPoint(SocketAddress sa)
    {
    	m_Id = sa;
    }
    public String GetDebugInfo()
    {
        return " State " + Integer.toString(m_State) + " byte counter " + Long.toString(m_ByteCounter);
    }
}
