package org.netlight.server.messaging;

/**
 * @author ahmad
 */
public interface MessageQueueLoopStrategy {

    Message next(MessageQueue queue);

    void poke();

    boolean stopIfEmpty();

}
