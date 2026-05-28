package operato.logis.connector.socket.netty.handler;

import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.Setter;

public abstract class BaseClientHandler extends ChannelInboundHandlerAdapter implements INettyClientHandler {

    @Getter
    @Setter
    private IReconnectable reconnectable;
}
