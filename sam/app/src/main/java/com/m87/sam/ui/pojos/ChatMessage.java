package com.m87.sam.ui.pojos;

import java.util.Date;

/**
 * Created by tim-azul on 7/8/17.
 */

public class ChatMessage {
    public int sourceId;
    public int destinationId;
    public boolean received;
    public String message;
    public Date timestamp;

    public ChatMessage(int souceId, int destinationId, String message, boolean received) {
        super();
        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.received = received;
        this.message = message;
        this.timestamp = new Date();
    }
}
