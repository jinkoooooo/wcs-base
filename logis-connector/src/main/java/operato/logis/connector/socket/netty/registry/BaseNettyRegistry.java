package operato.logis.connector.socket.netty.registry;


import operato.logis.connector.socket.netty.NettyClient;
import operato.logis.connector.socket.netty.NettyServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseNettyRegistry {

    private Map<String, NettyServer> serverMap = new ConcurrentHashMap<>();
    private Map<String, NettyClient> clientMap = new ConcurrentHashMap<>();

    // 서버 등록
    public void registerServer(NettyServer server) {
        serverMap.put(server.getServerId(), server);
    }

    // 클라이언트 등록
    public void registerClient(NettyClient client) {
        clientMap.put(client.getClientId(), client);
    }

    // 서버 조회
    public NettyServer getServer(String serverId) {
        return serverMap.get(serverId);
    }

    // 클라이언트 조회
    public NettyClient getClient(String clientId) {
        return clientMap.get(clientId);
    }


    public void sendToAllClient(String serverId, String message) throws Exception {
        NettyServer server = getServer(serverId);
        if (server != null) {
            server.sendToAll(message);
        } else {
            throw new Exception("Server not found: " + server.getServerId());
        }
    }

    public void sendToClient(String serverId, String clientIp, String message) throws Exception {
        NettyServer server = getServer(serverId);
        if (server != null) {
            server.sendToClient(clientIp, message);
        } else {
            throw new Exception("Server not found: " + server.getServerId());
        }
    }

    public void sendToServer(String clientId, String message) throws Exception {
        NettyClient client = getClient(clientId);
        if (client != null) {
            client.send(message);
        } else {
            throw new Exception("Client not found: " + client.getClientId());
        }
    }

}
