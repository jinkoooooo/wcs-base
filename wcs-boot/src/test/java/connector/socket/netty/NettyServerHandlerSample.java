package connector.socket.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import operato.logis.connector.socket.netty.handler.BaseServerHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class NettyServerHandlerSample extends BaseServerHandler {

    private ChannelGroup clients;
    private final Map<String, Channel> clientMap;

    public NettyServerHandlerSample() {
        this.clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.clientMap = new HashMap<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        clients.add(channel);

        String clientIp = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        clientMap.put(clientIp, channel);

        System.out.println("[NettyServerHandlerSample] connected: " + clientIp);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        clients.remove(channel);

        String clientIp = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        clientMap.remove(clientIp);

        System.out.println("[NettyServerHandlerSample] disconnected: " + clientIp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String strMessage = new String(bytes);
        System.out.println("[NettyServerHandlerSample] Received message: " + strMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    @Override
    public void sendToAll(String message) {
        clients.writeAndFlush(Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
    }

    @Override
    public void sendToClient(String ip, String message) {
        Channel channel = clientMap.get(ip);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
        } else {
            System.out.println("[NettyServerHandlerSample] Client with IP " + ip + " not found or inactive.");
        }
    }

}
