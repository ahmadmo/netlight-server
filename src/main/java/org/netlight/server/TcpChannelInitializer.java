package org.netlight.server;

import io.netty.channel.*;
import org.netlight.encoding.EncodingProtocol;

import java.util.Objects;

/**
 * @author ahmad
 */
public final class TcpChannelInitializer extends ChannelInitializer<Channel> {

    private final EncodingProtocol protocol;
    private final TcpServerHandler handler;

    public TcpChannelInitializer(EncodingProtocol protocol, ServerContext serverCtx) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
        handler = new TcpServerHandler(serverCtx);
    }

    @Override
    public void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", protocol.decoder());
        p.addLast("encoder", protocol.encoder());
        p.addLast("handler", handler);
    }

    public EncodingProtocol getProtocol() {
        return protocol;
    }

    public TcpServerHandler getHandler() {
        return handler;
    }

}
