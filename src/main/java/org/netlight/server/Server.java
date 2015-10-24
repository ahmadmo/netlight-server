package org.netlight.server;

import io.netty.channel.ChannelFuture;
import io.netty.handler.ssl.SslContext;

/**
 * @author ahmad
 */
public interface Server extends AutoCloseable {

    boolean bind();

    boolean isRunning();

    ChannelFuture closeFuture();

    int getPort();

    ServerContext getServerContext();

    SslContext getSslContext();

    ServerChannelInitializer getChannelInitializer();

}
