package org.netlight.server;

import io.netty.channel.group.ChannelGroup;
import org.netlight.messaging.MessageQueueLoopGroup;

import java.util.Objects;

/**
 * @author ahmad
 */
public final class NetLightServerContext implements ServerContext {

    private final ChannelGroup channels;
    private final MessageQueueLoopGroup loopGroup;

    public NetLightServerContext(ChannelGroup channels, MessageQueueLoopGroup loopGroup) {
        Objects.requireNonNull(channels);
        Objects.requireNonNull(loopGroup);
        this.channels = channels;
        this.loopGroup = loopGroup;
    }

    @Override
    public ChannelGroup channels() {
        return channels;
    }

    @Override
    public MessageQueueLoopGroup loopGroup() {
        return loopGroup;
    }

    @Override
    public void close() {
        channels.close().awaitUninterruptibly();
        loopGroup.shutdownGracefully();
    }

}
