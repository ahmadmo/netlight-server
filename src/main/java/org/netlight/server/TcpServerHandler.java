package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.util.concurrent.Future;
import org.netlight.server.messaging.Message;
import org.netlight.server.messaging.MessagePromise;
import org.netlight.server.messaging.MessageQueueLoopGroup;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author ahmad
 */
@ChannelHandler.Sharable
public final class TcpServerHandler extends SimpleChannelInboundHandler<Message> implements ServerHandler {

    private static final int FLUSH_COUNT = 5;

    private final ServerContext serverCtx;
    private final MessageQueueLoopGroup loopGroup;
    private final ChannelGroup channels;
    private final Map<String, ConnectionContext> connections = new ConcurrentHashMap<>();
    private final Map<String, Queue<MessagePromise>> pendingMessages = new ConcurrentHashMap<>();

    public TcpServerHandler(ServerContext serverCtx) {
        Objects.requireNonNull(serverCtx);
        this.serverCtx = serverCtx;
        this.loopGroup = serverCtx.loopGroup();
        this.channels = serverCtx.channels();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        final String id = channel.toString();
        pendingMessages.remove(id);
        connections.remove(id);
        channels.remove(channel);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (!msg.isEmpty()) {
            loopGroup.queueMessage(getConnectionContext(ctx), msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        if (channel.isWritable()) {
            sendMessages(ctx, pendingMessages.remove(channel.toString()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace(); // TODO log
        ctx.close();
    }

    @Override
    public ServerContext getServerContext() {
        return serverCtx;
    }

    @Override
    public void sendMessage(ChannelHandlerContext ctx, MessagePromise promise) {
        if (ctx == null || promise == null || promise.message().isEmpty()) {
            return;
        }
        final Channel channel = ctx.channel();
        if (channel.isActive()) {
            if (channel.isWritable()) {
                ctx.writeAndFlush(promise.message(), ctx.voidPromise());
            } else {
                promise.setCancellable(true);
                getQueue(channel.toString()).offer(promise);
            }
        } else {
            promise.setSuccess(false);
        }
    }

    @Override
    public void sendMessages(ChannelHandlerContext ctx, Collection<MessagePromise> promises) {
        if (ctx == null || promises == null || promises.isEmpty()) {
            return;
        }
        final Channel channel = ctx.channel();
        if (channel.isActive()) {
            if (channel.isWritable()) {
                channel.eventLoop().execute(new BatchMessageSender(ctx, promises));
            } else {
                promises.forEach(p -> p.setCancellable(true));
                enqueueMessages(channel.toString(), promises);
            }
        } else {
            promises.forEach(p -> p.setSuccess(false));
        }
    }

    private Queue<MessagePromise> getQueue(String key) {
        Queue<MessagePromise> queue = pendingMessages.get(key);
        if (queue == null) {
            final Queue<MessagePromise> q = pendingMessages.putIfAbsent(key, queue = new ConcurrentLinkedQueue<>());
            if (q != null) {
                queue = q;
            }
        }
        return queue;
    }

    private void enqueueMessages(String key, Collection<MessagePromise> promises) {
        Queue<MessagePromise> queue = pendingMessages.get(key);
        if (queue == null) {
            queue = pendingMessages.putIfAbsent(key, promises instanceof ConcurrentLinkedQueue
                    ? (Queue<MessagePromise>) promises
                    : new ConcurrentLinkedQueue<>(promises));
        }
        if (queue != null) {
            queue.addAll(promises);
        }
    }

    private ConnectionContext getConnectionContext(ChannelHandlerContext ctx) {
        final String id = ctx.channel().toString();
        ConnectionContext context = connections.get(id);
        if (context == null) {
            final ConnectionContext c = connections.putIfAbsent(id, context = new NettyConnectionContext(id, ctx, this));
            if (c != null) {
                context = c;
            }
        }
        return context;
    }

    private final class BatchMessageSender implements Runnable {

        private final ChannelHandlerContext ctx;
        private final Queue<MessagePromise> promises;

        private BatchMessageSender(ChannelHandlerContext ctx, Collection<MessagePromise> promises) {
            this.ctx = ctx;
            this.promises = promises instanceof ConcurrentLinkedQueue
                    ? (Queue<MessagePromise>) promises
                    : new ConcurrentLinkedQueue<>(promises);
        }

        @Override
        public void run() {
            final Channel channel = ctx.channel();
            MessagePromise promise;
            while (!promises.isEmpty() && channel.isActive() && channel.isWritable()) {
                for (int i = 0; i < FLUSH_COUNT && (promise = promises.poll()) != null; i++) {
                    if (!promise.isCancelled() && !promise.message().isEmpty()) {
                        final MessagePromise p = promise;
                        ctx.write(p.message()).addListener(f -> completePromise(p, f));
                    }
                }
                ctx.flush();
            }
            if (!promises.isEmpty()) {
                if (channel.isActive()) {
                    promises.forEach(p -> p.setCancellable(true));
                    enqueueMessages(channel.toString(), promises);
                } else {
                    promises.forEach(p -> p.setSuccess(false));
                    promises.clear();
                }
            }
        }

    }

    private static void completePromise(MessagePromise p, Future<? super Void> f) {
        if (f.isSuccess()) {
            p.setSuccess();
        } else {
            p.setFailure(f.cause());
        }
    }

}
