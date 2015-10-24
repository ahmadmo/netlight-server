package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.util.Objects;

/**
 * @author ahmad
 */
public final class HttpChannelInitializer extends ChannelInitializer<Channel> {

//    private final HttpServerHandler handler;

    public HttpChannelInitializer(ServerContext serverCtx) {
        Objects.requireNonNull(serverCtx);
//        this.handler = new HttpServerHandler(serverCtx);
    }

    @Override
    public void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("deflater", new HttpContentCompressor());
//        p.addLast("handler", new HttpSnoopServerHandler()); // TODO replace with HttpServerHandler
//        p.addLast("handler", handler);
    }

//    public HttpServerHandler getHandler() {
//        return handler;
//    }

}
