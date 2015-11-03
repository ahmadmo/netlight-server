package org.netlight.server;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.netlight.encoding.EncodingProtocol;
import org.netlight.encoding.JsonEncodingProtocol;
import org.netlight.util.CommonUtils;
import org.netlight.util.concurrent.AtomicBooleanField;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author ahmad
 */
public final class Endpoint implements AutoCloseable {

    private static final EncodingProtocol DEFAULT_ENCODING_PROTOCOL = JsonEncodingProtocol.INSTANCE;

    private final int port;
//    private final MessageQueueLoopGroup loopGroup;
//    private final Server server;
    private final AtomicBooleanField closed = new AtomicBooleanField();

    public Endpoint(int port, EncodingProtocol encodingProtocol) {
        this.port = port;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
//            ((NettyServer) server).close();
//            loopGroup.shutdownGracefully();
        }
    }

    public static final class EndpointBuilder {

        private int port;
        private EncodingProtocol encodingProtocol;

        public EndpointBuilder port(int port) {
            this.port = port;
            return this;
        }

        public EndpointBuilder encodingProtocol(EncodingProtocol encodingProtocol) {
            this.encodingProtocol = encodingProtocol;
            return this;
        }

        public Endpoint build() {
            return new Endpoint(port, CommonUtils.getOrDefault(encodingProtocol, DEFAULT_ENCODING_PROTOCOL));
        }

    }

    public static EndpointBuilder on(int port) {
        return new EndpointBuilder();
    }

    private static SslContext getSslContext() {
        try {
            final SelfSignedCertificate ssc = new SelfSignedCertificate();
            return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } catch (CertificateException | SSLException e) {
            return null;
        }
    }

}
