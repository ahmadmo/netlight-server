package org.netlight.server;

import io.netty.channel.group.ChannelGroup;
import org.netlight.server.messaging.MessageQueueLoopGroup;

/**
 * @author ahmad
 */
public interface ServerContext extends AutoCloseable {

    ChannelGroup channels();

    MessageQueueLoopGroup loopGroup();

}
