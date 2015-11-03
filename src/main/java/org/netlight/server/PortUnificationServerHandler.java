package org.netlight.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.netlight.channel.TcpChannelInitializer;

import java.util.List;
import java.util.Objects;

/**
 * @author ahmad
 */
public final class PortUnificationServerHandler extends ByteToMessageDecoder {

    private final SslContext sslCtx;
    private final HttpChannelInitializer httpChannelInitializer;
    private final TcpChannelInitializer tcpChannelInitializer;
    private final boolean detectSsl;
    private final boolean detectGzip;

    public PortUnificationServerHandler(SslContext sslCtx,
                                        HttpChannelInitializer httpChannelInitializer,
                                        TcpChannelInitializer tcpChannelInitializer) {
        this(sslCtx, httpChannelInitializer, tcpChannelInitializer, true, true);
    }

    private PortUnificationServerHandler(SslContext sslCtx,
                                         HttpChannelInitializer httpChannelInitializer,
                                         TcpChannelInitializer tcpChannelInitializer,
                                         boolean detectSsl, boolean detectGzip) {
        Objects.requireNonNull(sslCtx);
        Objects.requireNonNull(httpChannelInitializer);
        Objects.requireNonNull(tcpChannelInitializer);
        this.sslCtx = sslCtx;
        this.httpChannelInitializer = httpChannelInitializer;
        this.tcpChannelInitializer = tcpChannelInitializer;
        this.detectSsl = detectSsl;
        this.detectGzip = detectGzip;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 5) {
            return;
        }
        if (isSsl(in)) {
            enableSsl(ctx);
        } else {
            final int magic1 = in.getUnsignedByte(in.readerIndex());
            final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
            if (isGzip(magic1, magic2)) {
                enableGzip(ctx);
            } else if (isHttp(magic1, magic2)) {
                switchToHttp(ctx);
            } else {
                switchToTcp(ctx);
            }
        }
    }

    private boolean isSsl(ByteBuf buf) {
        return detectSsl && SslHandler.isEncrypted(buf);
    }

    private boolean isGzip(int magic1, int magic2) {
        return detectGzip && magic1 == 31 && magic2 == 139;
    }

    private boolean isHttp(int magic1, int magic2) {
        return magic1 == 'G' && magic2 == 'E' // GET
                || magic1 == 'P' && magic2 == 'O' // POST
                || magic1 == 'P' && magic2 == 'U' // PUT
                || magic1 == 'H' && magic2 == 'E' // HEAD
                || magic1 == 'O' && magic2 == 'P' // OPTIONS
                || magic1 == 'P' && magic2 == 'A' // PATCH
                || magic1 == 'D' && magic2 == 'E' // DELETE
                || magic1 == 'T' && magic2 == 'R' // TRACE
                || magic1 == 'C' && magic2 == 'O'; // CONNECT
    }

    private void enableSsl(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("ssl", sslCtx.newHandler(ctx.alloc()));
        p.addLast("unificationA", new PortUnificationServerHandler(sslCtx,
                httpChannelInitializer, tcpChannelInitializer, false, detectGzip));
        p.remove(this);
    }

    private void enableGzip(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("gzipdeflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
        p.addLast("gzipinflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        p.addLast("unificationB", new PortUnificationServerHandler(sslCtx,
                httpChannelInitializer, tcpChannelInitializer, detectSsl, false));
        p.remove(this);
    }

    private void switchToHttp(ChannelHandlerContext ctx) {
        httpChannelInitializer.initChannel(ctx.channel());
        ctx.pipeline().remove(this);
    }

    private void switchToTcp(ChannelHandlerContext ctx) {
        tcpChannelInitializer.initChannel(ctx.channel());
        ctx.pipeline().remove(this);
    }

}
