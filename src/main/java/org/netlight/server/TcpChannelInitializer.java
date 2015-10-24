package org.netlight.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import org.netlight.server.encoding.MessageDecoder;
import org.netlight.server.encoding.MessageEncoder;
import org.netlight.server.messaging.Message;
import org.netlight.util.serialization.ObjectSerializer;

/**
 * @author ahmad
 */
public final class TcpChannelInitializer extends ChannelInitializer<Channel> {

    private final MessageDecoder decoder;
    private final MessageEncoder encoder;
    private final TcpServerHandler handler;

    public TcpChannelInitializer(ObjectSerializer<Message> serializer, ServerContext serverCtx) {
        decoder = new MessageDecoder(serializer);
        encoder = new MessageEncoder(serializer);
        handler = new TcpServerHandler(serverCtx);
    }

    @Override
    public void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", decoder);
        p.addLast("encoder", encoder);
        p.addLast("handler", handler);
    }

    public MessageDecoder getDecoder() {
        return decoder;
    }

    public MessageEncoder getEncoder() {
        return encoder;
    }

    public TcpServerHandler getHandler() {
        return handler;
    }

}
