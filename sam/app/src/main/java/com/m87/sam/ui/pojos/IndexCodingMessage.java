package com.m87.sam.ui.pojos;

import com.m87.sam.ui.activity.HomeFragment;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;

import java.util.Random;

public class IndexCodingMessage {
    public static final int TYPE_INIT = 0;
    public static final int TYPE_TEST = 1;

    public static final int ROUND_FIRST = 0;
    public static final int ROUND_RETRANSMITTION_FINAL = 1;
    public static final int ROUND_RETRANSMITTION_INTERMEDIATE = 2;


    public String source;
    public String fullMessage;
    public int messageType;
    public int numDevices;
    public int roundType;
    public int destinationDeviceIdx;
    public int messageSize;
    public int[] messageByteArr;
    public String messageString;
    public boolean received;
    public M87ProximityDevice sourceDevice;
    public M87ProximityDevice destinationDevice;

    /**
     * Gets the device given the id
     */
    public static M87ProximityDevice getDeviceGivenId(int id) {
        for (int i = 0; i < HomeFragment.neighborList.size(); i++) {
            if (id == HomeFragment.neighborList.get(i).getId()) {
                return HomeFragment.neighborList.get(i);
            }
        }
        return null;
    }

    /**
     * Gets the device given the id
     */
    public static M87ProximityDevice getSelfDevice() {
        for (int i = 0; i < HomeFragment.neighborList.size(); i++) {
            if (HomeFragment.neighborList.get(i).isSelf()) {
                return HomeFragment.neighborList.get(i);
            }
        }
        return null;
    }

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
    public static IndexCodingMessage parseMessage(int sourceId, String message) {
        IndexCodingMessage m = new IndexCodingMessage();

        String[] messageTokens = message.split("\\.");

        if (messageTokens.length == 1) {
            return null;
        }

        String source = messageTokens[0];
        int type = Integer.parseInt(messageTokens[1]);

        // Basic settings
        m.source = messageTokens[0];
        m.fullMessage = message;
        m.received = true;
        m.sourceDevice = getDeviceGivenId(sourceId);
        m.destinationDevice = getSelfDevice();

        switch (type) {
            // Init message
            case TYPE_INIT:
                m = parseInitMessage(m);
                break;

            // IndexCoding algo message
            case TYPE_TEST:
                m = parseAlgoMessage(m, messageTokens);
                break;
        }

        return m;
    }

    public static IndexCodingMessage parseInitMessage(IndexCodingMessage m) {
        m.messageType = 0;
        return m;
    }

    public static IndexCodingMessage parseAlgoMessage(IndexCodingMessage m, String[] messageTokens) {

        m.messageType = 1;
        m.numDevices = Integer.parseInt(messageTokens[2]);
        m.roundType = Integer.parseInt(messageTokens[3]);
        m.destinationDeviceIdx = Integer.parseInt(messageTokens[4]);
        m.messageSize = Integer.parseInt(messageTokens[5]);
        m.messageByteArr = convertStringToBinaryArray(m.messageSize, messageTokens[6]);
        m.messageString = paddBinaryString(m.messageSize, Integer.toBinaryString(Integer.parseInt(messageTokens[6])));

        return m;
    }

    public static IndexCodingMessage buildInitMessage(int destinationId) {
        IndexCodingMessage m = new IndexCodingMessage();

        m.received = false;
        m.source = Controls.getInstance().deviceLabel;
        m.messageType = TYPE_INIT;

        m.fullMessage = m.source+"."+"0"+"."+"test_init";

        m.destinationDevice = getDeviceGivenId(destinationId);
        m.sourceDevice = getSelfDevice();

        return m;
    }

    public static IndexCodingMessage buildTestMessage(int destinationId) {
        IndexCodingMessage m = new IndexCodingMessage();

        m.received = false;
        m.source = Controls.getInstance().deviceLabel;
        m.messageType = TYPE_TEST;

        m.destinationDevice = getDeviceGivenId(destinationId);
        m.sourceDevice = getSelfDevice();

        return m;
    }

    public String buildTestFullMessage() {
        return this.source+"."+this.messageType+"."+this.numDevices+"."+this.roundType+"."+this.destinationDeviceIdx+"."+this.messageSize+"."+this.messageString;
    }



    /*----------------
     Binary
    ----------------*/
    public static String paddBinaryString(int mSize, String binStr) {
        while (binStr.length() % mSize != 0) {
            binStr = "0"+binStr;
        }
        return binStr;
    }


    public static int[] convertStringToBinaryArray(int mSize, String message) {
        int m = Integer.parseInt(message);
        int[] binaryArray = new int[1000];
        String binaryIntInStr = paddBinaryString(mSize, Integer.toBinaryString(m));

        for (int i = 0; i < binaryIntInStr.length(); i++) {
            binaryArray[i] = Integer.parseInt(String.valueOf(binaryIntInStr.charAt(i)));
        }

        Logger.debug(binaryArray.toString());

        return binaryArray;
    }

    public static String generateRandomBinaryMessage(int size) {
        Random random = new Random();
        int nextNumber = random.nextInt((int)Math.pow((double)2, (double)size) - 1) + 1;
        String randomString = Integer.toBinaryString(nextNumber);

        while (randomString.length() < size) {
            randomString = "0"+randomString;
        }

        Logger.debug("Radnom binary");
        Logger.debug(randomString);

        return randomString;
    }

    public static int binaryToInt(String binary) {
        return Integer.parseInt(binary, 2);
    }

    public static String replaceCharAt(String s, int pos, char c) {
        return s.substring(0,pos) + c + s.substring(pos+1);
    }


    @Override
    public String toString() {
        return String.format("Source: %s, type = %d, num devices = %d, round type = %d, dest = %d (%s), messageSize = %d, message = %s", this.source, this.messageType, this.numDevices, this.roundType, this.destinationDeviceIdx, this.destinationDevice.label, this.messageSize, this.messageString);
    }
}
