package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.netlight.messaging.MessagePromise;

import java.net.SocketAddress;
import java.util.Collection;

/**
 * @author ahmad
 */
public final class NettyConnectionContext implements ConnectionContext {

    private static final long serialVersionUID = -6946413113609439074L;

    private final String id;
    private final ChannelHandlerContext channelHandlerContext;
    private final ServerHandler serverHandler;

    public NettyConnectionContext(String id, ChannelHandlerContext channelHandlerContext, ServerHandler serverHandler) {
        this.id = id;
        this.channelHandlerContext = channelHandlerContext;
        this.serverHandler = serverHandler;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public ChannelHandlerContext channelHandlerContext() {
        return channelHandlerContext;
    }

    @Override
    public Channel channel() {
        return channelHandlerContext.channel();
    }

    @Override
    public ServerHandler serverHandler() {
        return serverHandler;
    }

    @Override
    public SocketAddress remoteAddress() {
        return channelHandlerContext.channel().remoteAddress();
    }

    @Override
    public void sendMessage(MessagePromise message) {
        serverHandler.sendMessage(channelHandlerContext, message);
    }

    @Override
    public void sendMessages(Collection<MessagePromise> promises) {
        serverHandler.sendMessages(channelHandlerContext, promises);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj instanceof NettyConnectionContext && id.equals(((NettyConnectionContext) obj).id);
    }

}
