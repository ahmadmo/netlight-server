package org.netlight.server;

import org.netlight.channel.RichChannelHandler;

/**
 * @author ahmad
 */
public interface ServerHandler extends RichChannelHandler {

    ServerContext getServerContext();

}
