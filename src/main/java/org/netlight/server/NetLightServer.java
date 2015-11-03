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
import org.netlight.channel.ChannelState;
import org.netlight.channel.ChannelStateListener;
import org.netlight.encoding.EncodingProtocol;
import org.netlight.util.EventNotifier;
import org.netlight.util.EventNotifierHandler;
import org.netlight.util.OSValidator;
import org.netlight.util.concurrent.AtomicBooleanField;
import org.netlight.util.concurrent.AtomicReferenceField;

/**
 * @author ahmad
 */
public final class NetLightServer implements Server {

    private final int port;
    private final ServerContext serverCtx;
    private final SslContext sslCtx;
    private final ServerChannelInitializer channelInitializer;
    private final ChannelGroup channels;
    private final AtomicReferenceField<Channel> channel = new AtomicReferenceField<>();
    private final AtomicBooleanField bound = new AtomicBooleanField(false);
    private final EventNotifier<ChannelState, ChannelStateListener> channelStateNotifier;

    public NetLightServer(int port, EncodingProtocol protocol, ServerContext serverCtx, SslContext sslCtx) {
        this.port = port;
        this.serverCtx = serverCtx;
        this.sslCtx = sslCtx;
        this.channelInitializer = new ServerChannelInitializer(serverCtx, sslCtx, protocol);
        this.channels = serverCtx.channels();
        channelStateNotifier = new EventNotifier<>(new EventNotifierHandler<ChannelState, ChannelStateListener>() {
            @Override
            public void handle(ChannelState event, ChannelStateListener listener) {
                listener.stateChanged(event);
            }

            @Override
            public void exceptionCaught(Throwable cause) {
                channelStateNotifier.start();
            }
        }, ChannelState.class);
    }

    @Override
    public boolean bind() {
        if (bound.get()) {
            return true;
        }
        channelStateNotifier.start();
        final ServerBootstrap b = configureBootstrap(new ServerBootstrap());
        try {
            final Channel ch = b.bind().sync().channel();
            final EventLoopGroup bossGroup = b.group(), workerGroup = b.childGroup();
            channels.add(ch);
            channel.set(ch);
            ch.closeFuture().addListener(f -> closed(bossGroup, workerGroup));
            fireChannelStateChanged(ChannelState.CONNECTED);
            return true;
        } catch (Exception e) {
            bound.set(false);
            fireChannelStateChanged(ChannelState.CONNECTION_FAILED);
        }
        return false;
    }

    private void closed(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        bound.set(false);
        channel.set(null);
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        fireChannelStateChanged(ChannelState.DISCONNECTED);
        channelStateNotifier.stopLater();
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
    public boolean isBound() {
        return bound.get();
    }

    @Override
    public ChannelFuture closeFuture() {
        Channel ch = channel.get();
        return ch == null ? null : ch.closeFuture();
    }

    @Override
    public int port() {
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
    public void addChannelStateListener(ChannelStateListener channelStateListener) {
        channelStateNotifier.addListener(channelStateListener);
    }

    @Override
    public void removeChannelStateListener(ChannelStateListener channelStateListener) {
        channelStateNotifier.removeListener(channelStateListener);
    }

    @Override
    public void fireChannelStateChanged(ChannelState state) {
        channelStateNotifier.notify(state);
    }

    @Override
    public void close() {
        ((NetLightServerContext) serverCtx).close();
        channel.set(null);
    }

}
