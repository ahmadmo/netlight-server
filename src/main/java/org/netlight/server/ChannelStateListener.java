package org.netlight.server;

/**
 * @author ahmad
 */
public interface ChannelStateListener {

    void stateChanged(ChannelState state, Server client);

}
