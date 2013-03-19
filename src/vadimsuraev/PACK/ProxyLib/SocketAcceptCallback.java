package vadimsuraev.PACK.ProxyLib;

import java.nio.channels.SocketChannel;

public interface SocketAcceptCallback {
    public void OnAccepted(SocketChannel socketChannel);
}
