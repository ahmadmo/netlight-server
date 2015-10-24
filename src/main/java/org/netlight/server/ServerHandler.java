package org.netlight.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.netlight.server.messaging.MessagePromise;

import java.util.Collection;

/**
 * @author ahmad
 */
public interface ServerHandler extends ChannelHandler {

    ServerContext getServerContext();

    void sendMessage(ChannelHandlerContext ctx, MessagePromise promise);

    void sendMessages(ChannelHandlerContext ctx, Collection<MessagePromise> promises);

}
