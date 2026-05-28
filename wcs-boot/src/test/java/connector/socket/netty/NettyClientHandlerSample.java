package connector.socket.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import operato.logis.connector.socket.netty.handler.BaseClientHandler;

public class NettyClientHandlerSample extends BaseClientHandler {


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[NettyClientHandlerSample] connected: " + ctx.channel().remoteAddress());
        // ctx.writeAndFlush(Unpooled.copiedBuffer("hi", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        String strMessage = new String(bytes);
        System.out.println("[NettyClientHandlerSample] Received message: " + strMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[NettyClientHandlerSample] channelInactive: " + ctx.channel().remoteAddress());
        reconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void reconnect() {
        getReconnectable().reconnect();
    }
}

