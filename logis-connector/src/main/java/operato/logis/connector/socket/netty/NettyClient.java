package operato.logis.connector.socket.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import operato.logis.connector.socket.netty.handler.BaseClientHandler;
import operato.logis.connector.socket.netty.handler.IReconnectable;

import java.util.concurrent.TimeUnit;

@Getter
public class NettyClient implements IReconnectable {
    private final String host;
    private final int port;
    private final String clientId;
    private final BaseClientHandler clientHandler;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Bootstrap bootstrap;
    private Channel clientChannel;
    private final int reconnectDelay = 3; // 재연결 딜레이 (초)


    public NettyClient(String host, int port, String clientId, BaseClientHandler clientHandler) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.clientHandler = clientHandler;
        this.clientHandler.setReconnectable(this);
    }


    public void start() {
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(clientHandler);

                    }
                });

        connect();
    }

    public void connect() {
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                clientChannel = future.channel();
                System.out.println("[NettyClient] [" + clientId + "] connected.");
            } else {
                System.out.println("[NettyClient] Connection failed. Retrying...");
                future.channel().eventLoop().schedule(this::connect, reconnectDelay, TimeUnit.SECONDS);
            }
        });
    }

    public void stop() {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }


    public void send(String message) {
        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(message);
        } else {
            System.out.println("[NettyClient] is not connected.");
        }
    }

    @Override
    public void reconnect() {
        connect();
    }
}
