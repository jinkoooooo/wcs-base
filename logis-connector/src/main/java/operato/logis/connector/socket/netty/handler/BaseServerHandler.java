package operato.logis.connector.socket.netty.handler;

import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.Setter;

public abstract class BaseServerHandler extends ChannelInboundHandlerAdapter implements INettyServerhandler {

    @Getter
    @Setter
    private IReconnectable reconnectable;
}
