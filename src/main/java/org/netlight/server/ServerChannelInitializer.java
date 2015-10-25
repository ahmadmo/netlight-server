package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.ssl.SslContext;
import org.netlight.encoding.EncodingProtocol;

import java.util.Objects;

/**
 * @author ahmad
 */
public final class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private final SslContext sslCtx;
    private final HttpChannelInitializer httpChannelInitializer;
    private final TcpChannelInitializer tcpChannelInitializer;

    public ServerChannelInitializer(ServerContext serverCtx, SslContext sslCtx, EncodingProtocol protocol) {
        Objects.requireNonNull(sslCtx);
        this.sslCtx = sslCtx;
        this.httpChannelInitializer = new HttpChannelInitializer(serverCtx);
        this.tcpChannelInitializer = new TcpChannelInitializer(protocol, serverCtx);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new PortUnificationServerHandler(sslCtx, httpChannelInitializer, tcpChannelInitializer));
    }

    public HttpChannelInitializer getHttpChannelInitializer() {
        return httpChannelInitializer;
    }

    public TcpChannelInitializer getTcpChannelInitializer() {
        return tcpChannelInitializer;
    }

}
