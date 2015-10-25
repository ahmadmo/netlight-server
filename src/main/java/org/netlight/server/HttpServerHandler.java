package org.netlight.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.netlight.messaging.MessagePromise;

import java.util.Collection;

/**
 * @author ahmad
 */
@ChannelHandler.Sharable
public final class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements ServerHandler {

    private final ServerContext serverCtx;

    public HttpServerHandler(ServerContext serverCtx) {
        this.serverCtx = serverCtx;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

    }

    @Override
    public ServerContext getServerContext() {
        return serverCtx;
    }

    @Override
    public void sendMessage(ChannelHandlerContext ctx, MessagePromise promise) {

    }

    @Override
    public void sendMessages(ChannelHandlerContext ctx, Collection<MessagePromise> promises) {

    }

}
