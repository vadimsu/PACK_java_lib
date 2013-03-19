package vadimsuraev.PACK.senderPackLib;

public interface OnMsgReady4Tx {
	public void OnMessageReadyToTx(Object param,byte []msg,boolean submit2Head);
}
