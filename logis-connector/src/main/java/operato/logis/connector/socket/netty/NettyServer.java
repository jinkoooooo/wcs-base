package operato.logis.connector.socket.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import operato.logis.connector.socket.netty.handler.BaseServerHandler;

@Getter
public class NettyServer {

    private final int port;
    private final String serverId;
    private final BaseServerHandler severHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyServer(int port, String serverId, BaseServerHandler severHandler) {
        this.port = port;
        this.serverId = serverId;
        this.severHandler = severHandler;
    }
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(severHandler);
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            serverChannel = f.channel();
            System.out.println("[NettyServer] [" + serverId + "] started on port " + port);

            // 서버는 끊긴 클라이언트가 다시 오면 그대로 받아줌
        } catch (Exception e) {
            stop();
            throw e;
        }
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    public void sendToAll(String message) {
        severHandler.sendToAll(message);
    }

    public void sendToClient(String ip, String message) {
        severHandler.sendToClient(ip, message);
    }

}
