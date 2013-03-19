package vadimsuraev.PACK.ReceiverPackLib;

public interface ClientSideCallbacks {
	public void OnDataReceived(byte[] data,int offset,int length);
	public void OnMsgRead4Tx(byte[] msg,boolean dummy);
	public  void OnEnd();
}
