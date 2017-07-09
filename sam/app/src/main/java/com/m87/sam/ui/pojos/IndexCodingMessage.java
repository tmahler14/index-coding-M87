package com.m87.sam.ui.pojos;

import com.m87.sam.ui.util.Logger;

public class IndexCodingMessage {

    public String source;
    public int messageType;
    public int numDevices;
    public int roundType;
    public int destination;
    public int messageSize;
    public int[] messageBytes;
    public String messageString;

    /**
     * Parses the index coding message
     *
     *  - Each message contains several pieces of info
     *      - Source: who sent the message (ex: 'TX')
     *      - Type: type of the message
     *          - 0: init message
     *          - 1: algo message
     *      - Number of devices: number of devices in the algo
     *      - Round Type: round in the transmission cycle
     *          - 0: First transmission of new message
     *          - 1: Retransmission (last message sent)
     *          - 2: Intermediate message (necessary for the between step)
     *      - Who the message is for: <int> who should be receiving the message
     *      - Size of each message: <int> number of bytes for each message
     *      - Message: bulk of the message containing the necessary info for each message
     *
     * @param message
     * @return
     */
    public static IndexCodingMessage parseMessage(String message) {
        IndexCodingMessage m = new IndexCodingMessage();

        String[] messageTokens = message.split("\\.");

        String source = messageTokens[0];
        int type = Integer.parseInt(messageTokens[1]);

        switch (type) {
            // Init message
            case 0:
                return parseInitMessage();

            // IndexCoding algo message
            case 1:
                return parseAlgoMessage(messageTokens);
        }

        return m;
    }

    public static IndexCodingMessage parseInitMessage() {
        IndexCodingMessage m = new IndexCodingMessage();
        m.messageType = 0;
        return m;
    }

    public static IndexCodingMessage parseAlgoMessage(String[] messageTokens) {
        IndexCodingMessage m = new IndexCodingMessage();

        m.messageType = 1;
        m.source = messageTokens[0];
        m.numDevices = Integer.parseInt(messageTokens[2]);
        m.roundType = Integer.parseInt(messageTokens[3]);
        m.destination = Integer.parseInt(messageTokens[4]);
        m.messageSize = Integer.parseInt(messageTokens[5]);
        m.messageBytes = convertStringToBinaryArray(messageTokens[6]);
        m.messageString = Integer.toBinaryString(Integer.parseInt(messageTokens[6]));

        return m;
    }

    public static int[] convertStringToBinaryArray(String message) {
        int m = Integer.parseInt(message);
        int[] binaryArray = new int[1000];
        String binaryIntInStr = Integer.toBinaryString(m);

        for (int i = 0; i < binaryIntInStr.length(); i++) {
            binaryArray[i] = Integer.parseInt(String.valueOf(binaryIntInStr.charAt(i)));
        }

        Logger.debug(binaryArray.toString());

        return binaryArray;
    }

    @Override
    public String toString() {
        return String.format("Source: %s, type = %d, num devices = %d, round type = %d, dest = %d, messageSize = %d, message = %s", this.source, this.messageType, this.numDevices, this.roundType, this.destination, this.messageSize, this.messageString);
    }
}
