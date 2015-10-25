package org.netlight.server;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.netlight.encoding.JsonEncodingProtocol;
import org.netlight.messaging.*;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.util.concurrent.ForkJoinPool;

/**
 * @author ahmad
 */
public class Test {

    public static void main(String[] args) throws Exception {
        final ServerContext serverCtx = newServerContext();
        final SslContext sslCtx = newSslContext();
        final Server server = new NettyServer(18874, JsonEncodingProtocol.INSTANCE, serverCtx, sslCtx);
        server.bind();
        boolean closed = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String s;
            while ((s = reader.readLine()) != null) {
                if (s.equalsIgnoreCase("q")) {
                    closed = close(server);
                    break;
                }
            }
        } finally {
            if (!closed) {
                close(server);
            }
        }
        System.exit(0);
    }

    private static ServerContext newServerContext() {
        final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        final MessageQueueLoopHandler handler = new MessageQueueLoopHandler() {
            @Override
            public void onMessage(MessageQueueLoop loop, Message message) {
                final Number id = message.getNumber("message_id");
                System.out.println(message.getString("user_input"));
                Message resp = new Message();
                resp.put("reply", "time = " + System.currentTimeMillis());
                send(loop.getConnectionContext(), id, resp);
            }

            @Override
            public void exceptionCaught(MessageQueueLoop loop, Message message, Throwable cause) {
                cause.printStackTrace();
            }
        };
        final MessageQueueLoopGroup loopGroup
                = new MessageQueueLoopGroup(new ForkJoinPool(128), handler, new MessageQueuePerConnectionStrategy(), new LoopShiftingStrategy());
        return new NettyServerContext(channelGroup, loopGroup);
    }

    private static SslContext newSslContext() throws CertificateException, SSLException {
        final SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    private static MessagePromise send(ConnectionContext ctx, Number id, Message message) {
        if (id != null) {
            message.put("correlation_id", id);
        }
        MessagePromise promise = new DefaultMessagePromise(message, ctx.remoteAddress());
        ctx.sendMessage(promise);
        return promise;
    }

    private static boolean close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
        return true;
    }

    private static int getInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable t) {
            return def;
        }
    }

}
