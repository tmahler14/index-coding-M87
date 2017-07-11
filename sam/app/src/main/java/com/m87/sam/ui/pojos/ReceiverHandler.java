package com.m87.sam.ui.pojos;

import com.m87.sam.ui.activity.HomeActivity;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by tim-azul on 7/9/17.
 */

public class ReceiverHandler {

    public ArrayList<IndexCodingMessage> messageList = new ArrayList<IndexCodingMessage>();
    public HomeActivity activity;

    public ArrayList<String> roundMessages = new ArrayList<String>();

    public ReceiverHandler(HomeActivity activity) {
        this.activity = activity;
    }

    public void handleMessage(IndexCodingMessage m) {
        Logger.debug("Receiver received message");

        messageList.add(m);

        Logger.debug(m.toString());

        switch (m.messageType) {
            // Init message
            case IndexCodingMessage.TYPE_INIT:
                handleInitMessage(m);
                break;
            case IndexCodingMessage.TYPE_TEST:
                handleTestMessage(m);
                break;
        }
    }

    // Handle init message
    public void handleInitMessage(IndexCodingMessage m) {
        IndexCodingMessage initMessage = IndexCodingMessage.buildInitMessage(m.sourceDevice.getId());

        messageList.add(initMessage);

        // Send msg
        activity.newMsg(initMessage.destinationDevice.getId(), initMessage.fullMessage);
    }

    // Handle test message
    public void handleTestMessage(IndexCodingMessage m) {

        switch (m.roundType) {
            // First round of messages
            case IndexCodingMessage.ROUND_FIRST:
                handleFirstRoundMessage(m);
                break;

            // Final round of retransmission
            case IndexCodingMessage.ROUND_RETRANSMITTION_FINAL:
                break;

            // Intermediate round of retransmission
            case IndexCodingMessage.ROUND_RETRANSMITTION_INTERMEDIATE:
                break;
        }
    }

    public String[] extractMessages(int mSize, String message) {
        int numChunks = message.length() / mSize;
        String[] messages = new String[numChunks];

        Logger.debug("Mess --> "+message);
        Logger.debug("numChunks --> "+numChunks);

        for (int i = 0; i < numChunks; i++) {
            int startIdx = i * mSize;

            messages[i] = message.substring(startIdx, startIdx+mSize);
        }

        return messages;
    }

    public void handleFirstRoundMessage(IndexCodingMessage m) {
        Logger.debug("Hanldes 1st round");

        String[] messages = extractMessages(m.messageSize, m.messageString);
        Logger.debug("Mess in bytes -->");

        Logger.debug(Arrays.toString(messages));

        // Clear the array of round messages
        roundMessages.clear();
        String receiveMsgStr = randomlyDropMessages(messages);

        // Send back message to transmitter declaring which messages were received
        IndexCodingMessage returnMessage = IndexCodingMessage.buildTestMessage(m.sourceDevice.getId());
        returnMessage.roundType = IndexCodingMessage.ROUND_FIRST;
        returnMessage.messageString = receiveMsgStr;
        returnMessage.messageSize = m.messageSize;
        returnMessage.messageByteArr = IndexCodingMessage.convertStringToBinaryArray(receiveMsgStr.length(), receiveMsgStr);
        returnMessage.destinationDeviceIdx = m.destinationDeviceIdx;
        returnMessage.fullMessage = returnMessage.buildTestFullMessage();

        Logger.debug("Return msg --> "+returnMessage.toString());

        activity.newMsg(returnMessage.destinationDevice.getId(), returnMessage.fullMessage);
    }

    /**
     * Randomly drops messages given the string and sends back status msg
     *  - Aka if 5 messages total, and recieved 1,3,5, then return would be '10101'
     *
     * @param messages
     * @return
     */
    public String randomlyDropMessages(String[] messages) {
        Random random = new Random();
        String receiveStr = "";
        for (int i = 0; i < messages.length; i++) {
            double flip = (random.nextInt(100)+1) / 100.0;
            if (flip <= Controls.getInstance().getReceiveProb()) {
                Logger.debug("Receieved!");
                roundMessages.add(messages[i]);
                receiveStr += "1";
            } else {
                Logger.debug("Dropped!");
                receiveStr += "0";
            }
        }

        // If doesnt contain 1, force it to contain a 1
        if (!receiveStr.contains("1")) {
            int rando = random.nextInt(receiveStr.length());
            receiveStr = IndexCodingMessage.replaceCharAt(receiveStr, rando, '1');
        }

        return receiveStr;
    }
}
