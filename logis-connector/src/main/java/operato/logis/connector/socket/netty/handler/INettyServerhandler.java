package operato.logis.connector.socket.netty.handler;

public interface INettyServerhandler{
    void sendToAll(String message);
    void sendToClient(String ip, String message);

}
