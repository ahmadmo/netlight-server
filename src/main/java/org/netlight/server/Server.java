package org.netlight.server;

import io.netty.channel.ChannelFuture;
import io.netty.handler.ssl.SslContext;
import org.netlight.channel.ChannelState;
import org.netlight.channel.ChannelStateListener;

/**
 * @author ahmad
 */
public interface Server extends AutoCloseable {

    boolean bind();

    boolean isBound();

    ChannelFuture closeFuture();

    int port();

    ServerContext getServerContext();

    SslContext getSslContext();

    ServerChannelInitializer getChannelInitializer();

    void addChannelStateListener(ChannelStateListener channelStateListener);

    void removeChannelStateListener(ChannelStateListener channelStateListener);

    void fireChannelStateChanged(ChannelState state);

}
