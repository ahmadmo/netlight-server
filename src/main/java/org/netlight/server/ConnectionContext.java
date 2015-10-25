package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.netlight.messaging.MessagePromise;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Collection;

/**
 * @author ahmad
 */
public interface ConnectionContext extends Serializable {

    String id();

    ChannelHandlerContext channelHandlerContext();

    Channel channel();

    ServerHandler serverHandler();

    SocketAddress remoteAddress();

    void sendMessage(MessagePromise promise);

    void sendMessages(Collection<MessagePromise> promises);

}
