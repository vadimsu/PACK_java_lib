package vadimsuraev.PACK.ProxyLib;

public interface SocketCallbacks {
    public void OnRead(Object data,int count);
    public void OnWritten(Object data,int count);
    public void OnConnectionBroken(Object data);
    public void OnConnected();
}
