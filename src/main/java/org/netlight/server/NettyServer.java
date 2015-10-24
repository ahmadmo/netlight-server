package org.netlight.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.netlight.server.messaging.Message;
import org.netlight.util.OSValidator;
import org.netlight.util.serialization.ObjectSerializer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ahmad
 */
public final class NettyServer implements Server {

    private final int port;
    private final ServerContext serverCtx;
    private final SslContext sslCtx;
    private final ChannelGroup channels;
    private final ServerChannelInitializer channelInitializer;
    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    public NettyServer(int port, ObjectSerializer<Message> serializer, ServerContext serverCtx, SslContext sslCtx) {
        Objects.requireNonNull(serverCtx);
        Objects.requireNonNull(sslCtx);
        this.port = port;
        this.serverCtx = serverCtx;
        this.sslCtx = sslCtx;
        this.channels = serverCtx.channels();
        this.channelInitializer = new ServerChannelInitializer(serializer, serverCtx, sslCtx);
    }

    @Override
    public boolean bind() {
        if (!running.compareAndSet(false, true)) {
            return true;
        }
        final ServerBootstrap b = configureBootstrap(new ServerBootstrap());
        try {
            final Channel ch = b.bind().sync().channel();
            final EventLoopGroup bossGroup = b.group(), workerGroup = b.childGroup();
            channels.add(ch);
            channel.set(ch);
            ch.closeFuture().addListener(f -> {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                running.set(false);
            });
            return true;
        } catch (Exception e) {
            running.set(false);
            e.printStackTrace(); // TODO log
        }
        return false;
    }

    private ServerBootstrap configureBootstrap(ServerBootstrap b) {
        boolean unix = OSValidator.isUnix();
        return configureBootstrap(b,
                unix ? new EpollEventLoopGroup() : new NioEventLoopGroup(),
                unix ? new EpollEventLoopGroup() : new NioEventLoopGroup());
    }

    private ServerBootstrap configureBootstrap(ServerBootstrap b, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        b.group(bossGroup, workerGroup)
                .channel(OSValidator.isUnix() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(port)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(channelInitializer);
        return b;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public ChannelFuture closeFuture() {
        Channel ch = channel.get();
        return ch == null ? null : ch.closeFuture();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public ServerContext getServerContext() {
        return serverCtx;
    }

    @Override
    public SslContext getSslContext() {
        return sslCtx;
    }

    @Override
    public ServerChannelInitializer getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    public void close() {
        ((NettyServerContext) serverCtx).close();
        channel.set(null);
    }

}
