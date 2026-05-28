package connector.socket.netty;

import operato.logis.connector.socket.netty.NettyClient;
import operato.logis.connector.socket.netty.NettyServer;
import org.junit.jupiter.api.Test;

public class NettyTest {

    private NettyServer server1;
    private NettyClient client1;
    @Test
    public void 네티소켓_테스트() {
        try {
            server();
            Thread.sleep(1000);
            client();
            Thread.sleep(1000);
            Thread.sleep(500);
            server1.sendToClient("127.0.0.1", "hola");
            Thread.sleep(1000);
            server1.stop();
            Thread.sleep(10* 1000);
            client1.stop();
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }

    @Test
    public void 서버(){
        try {
            server();
            Thread.sleep(10*1000);
            server1.stop();
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }

    @Test
    public void 클라이언트(){
        try {
            client();
            Thread.sleep(10*1000);
            client1.stop();
        }catch (Exception e){
            System.out.println(e.toString());
        }
    }

    private void server(){
        new Thread(() -> {
            try {
                server1 = new NettyServer(8101, "server1", new NettyServerHandlerSample());
                server1.start();
            } catch (Exception e){
                System.out.println(e);
                try {
                    throw e;
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }).start();
    }

    private void client(){
        new Thread(() -> {
            try {
                client1 = new NettyClient("localhost", 8101, "client1", new NettyClientHandlerSample());
                client1.start();
            }catch (Exception e){
                System.out.println(e);
                throw e;
            }
        }).start();

    }

}
