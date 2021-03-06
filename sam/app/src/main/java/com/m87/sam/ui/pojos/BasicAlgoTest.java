package com.m87.sam.ui.pojos;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.m87.sam.ui.activity.HomeActivity;
import com.m87.sam.ui.activity.HomeFragment;
import com.m87.sam.ui.util.Logger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class BasicAlgoTest extends Thread {
    public Context context;
    public HomeActivity activity;
    public int numReceivers;

    public ArrayList<IndexCodingMessage> messageList = new ArrayList<IndexCodingMessage>();
    public ArrayList<IndexCodingMessage> initMessageList = new ArrayList<IndexCodingMessage>();
    public ArrayList<IndexCodingMessage> round1MessageList = new ArrayList<>();
    public ArrayList<IndexCodingMessage> successMessages = new ArrayList<>();
    public ArrayList<Message> testMessages = new ArrayList<Message>();
    public int[] retransmissionDevices;

    public Matrix entireMatrix;
    public int[] diags;
    public Matrix reducedMatrix;

    public static int MESSAGE_SIZE = 4;

    public BasicAlgoTest(Context context, HomeActivity activity){
        this.context = context;
        this.activity = activity;
        this.numReceivers = HomeFragment.neighborList.size() - 1;
    }

    // Message containing both the device and actual binary message
    public class Message {

        public M87ProximityDevice device;
        public String binaryMessage;
        public boolean isOriginalMsg;

        public Message(M87ProximityDevice d, String bm, boolean isOriginalMsg) {
            this.device = d;
            this.binaryMessage = bm;
            this.isOriginalMsg = isOriginalMsg;
        }

        @Override
        public String toString() {
            return String.format("device = %s, msg = %s", device.label, binaryMessage);
        }
    }

    @Override
    public void run() {
        super.run();

        Logger.debug("TEST start thread");

        sendInitMessages();

    }


    public void handleMessage(IndexCodingMessage m) {
        Logger.debug("TEST: Receievd messages");

        Logger.debug(m.toString());

        switch (m.messageType) {
            // Init message
            case 0:
                handleInitMessage(m);
                break;

            // Handle round 1 message
            case 1:
                if (m.roundType == IndexCodingMessage.ROUND_FIRST) {
                    handleRound1Message(m);
                }
                else if (m.roundType == IndexCodingMessage.ROUND_RETRANSMITTION_SUCCESS) {
                    handleRoundSuccessMessage(m);
                }

                break;
        }
    }

    public void sendInitMessages() {
        Logger.debug("TEST: Sending init messages");

        Toast msg = Toast.makeText(this.context, "Sending init messages", Toast.LENGTH_SHORT);
        msg.show();

        for (int i = 0; i < HomeFragment.neighborList.size(); i++) {
            Logger.debug("TEST: Neighbor = "+HomeFragment.neighborList.get(i).getId());

            if (!HomeFragment.neighborList.get(i).isSelf()) {
                IndexCodingMessage initMessage = IndexCodingMessage.buildInitMessage(HomeFragment.neighborList.get(i).getId(), 0);
                messageList.add(initMessage);
                activity.newMsg(initMessage.destinationDevice.id, initMessage.fullMessage);
            }
        }
    }

    public void handleInitMessage(IndexCodingMessage m) {
        // Update device label and index
        m.sourceDevice.setLabel(m.source);

        // Based on the label 'RX2'
        //m.sourceDevice.setDeviceIdx(Integer.parseInt(Character.toString(m.source.charAt(2))) - 1);
        initMessageList.add(m);
        // Based on received order
        m.sourceDevice.setDeviceIdx(initMessageList.indexOf(m));

        Logger.debug("TEST: Source device");
        Logger.debug(m.sourceDevice.toString());


        // If received all messages, then start test
        if (hasReceivedAllInitMessages()) {
            startTest();
        }
    }

    public boolean hasReceivedAllInitMessages() {
        return initMessageList.size() == this.numReceivers;
    }

    public void handleRound1Message(IndexCodingMessage m) {
        entireMatrix.setRow(m.destinationDeviceIdx, m.messageByteArr);

        round1MessageList.add(m);

        if (hasReceivedAllRound1Messages()) {
            runIndexCodingAlgo();
        }
    }

    public void handleRoundSuccessMessage(IndexCodingMessage m) {

        successMessages.add(m);

        if (hasReceivedAllSuccessMessages()) {
            Toast msg = Toast.makeText(this.context, "Test DONE", Toast.LENGTH_SHORT);
            msg.show();
        }

    }

    public boolean hasReceivedAllSuccessMessages() {
        return successMessages.size() == this.retransmissionDevices.length;
    }

    public void runIndexCodingAlgo() {
        Logger.debug("GOT ALL ROUND 1 MSGs");
        entireMatrix.show();

        Logger.debug("DIAGS");
        diags = entireMatrix.getDiagonals();
        Logger.debug(Arrays.toString(diags));

        Logger.debug("RDEVICES");
        retransmissionDevices = IndexCoding.getRetransmissionDevices(diags);
        Logger.debug(Arrays.toString(retransmissionDevices));

        reducedMatrix = IndexCoding.reduceMatrix(entireMatrix);
        Logger.debug("REDUCED");
        reducedMatrix.show();

        GreedyColoring gc = new GreedyColoring();
        Matrix retransmissionMatrix = gc.greedyColoring(reducedMatrix);
        Matrix fullRetransmissionMatrix = new Matrix(retransmissionMatrix.rowSize, entireMatrix.colSize);
        fullRetransmissionMatrix.setMatrixFromIndices(retransmissionMatrix, retransmissionDevices);

        Logger.debug("retransmission");
        retransmissionMatrix.show();

        Logger.debug("retransmissionDevices");
        Logger.debug(Arrays.toString(retransmissionDevices));

        int[] originalMessages = new int[retransmissionMatrix.colSize];

        for (int i = 0; i < retransmissionDevices.length; i++) {
            Message m = getMessageGivenDeviceIndex(retransmissionDevices[i]);
            Logger.debug("MESSAGE14");
            Logger.debug(m.toString());
            originalMessages[i] = Integer.parseInt(m.binaryMessage, 2);
        }

        Logger.debug("ORIGINAL");
        Logger.debug(Arrays.toString(originalMessages));

        int[] retransmissionMessages = IndexCoding.constructRetransmissionMessages(retransmissionMatrix, originalMessages);

        Logger.debug("R MESSAGES");
        Logger.debug(Arrays.toString(retransmissionMessages));

        // Send retransmission messages
        Logger.debug("TEST sending retransmission messages");
        for (int i = 0; i < retransmissionDevices.length; i++) {
            Message msg = getMessageGivenDeviceIndex(retransmissionDevices[i]);

            // Send each retransmission device
            for (int j = 0; j < retransmissionMessages.length; j++) {
                IndexCodingMessage m = IndexCodingMessage.buildTestMessage(msg.device.getId());
                m.roundType = IndexCodingMessage.ROUND_RETRANSMITTION_FINAL;
                m.messageString = Integer.toString(retransmissionMessages[j], 16);
                m.messageSize = MESSAGE_SIZE;
                m.destinationDeviceIdx = msg.device.getDeviceIdx();
                m.numDevices = this.numReceivers;
                m.retransmissionMetaData = fullRetransmissionMatrix.rowToString(j);

                m.fullMessage = m.buildTestRetransmissionFullMessage();

                Logger.debug("TEST retransmission message --> "+m.toString());
                Logger.debug(m.fullMessage);
                activity.newMsg(msg.device.getId(), m.fullMessage);
            }

        }

    }

    public Message getMessageGivenDeviceIndex(int index) {
        Message m = null;
        for (int i = 0; i < testMessages.size(); i++) {
            if (testMessages.get(i).device.deviceIdx == index) {
                return testMessages.get(i);
            }
        }
        return m;
    }

    public boolean hasReceivedAllRound1Messages() {
        return round1MessageList.size() == this.numReceivers;
    }

    public String createTestMessages(boolean isOriginalMsg) {
        // Generate test messages composed of binary strings
        for (int i = 0; i < initMessageList.size(); i++) {

            // Make sure not self
            if (!initMessageList.get(i).sourceDevice.isSelf()) {
                Message m = new Message(initMessageList.get(i).sourceDevice, IndexCodingMessage.generateRandomBinaryMessage(MESSAGE_SIZE), isOriginalMsg);
                Logger.debug("TEST Message --> "+m.toString());
                testMessages.add(m);
            }
        }

        // Build total message
        String totalMessage = "";
        for (int i = 0; i < testMessages.size(); i++) {
            totalMessage += testMessages.get(i).binaryMessage;
        }

        Logger.debug("TEST Total Message");
        Logger.debug(totalMessage);

        return totalMessage;
    }

    public void startTest() {

        Logger.debug("TEST Start test");

        Logger.debug("NUM REC:"+this.numReceivers);

        Toast msg = Toast.makeText(this.context, "Init received, starting test", Toast.LENGTH_SHORT);
        msg.show();

        // Create test messages
        String finalMessage = createTestMessages(true);
        entireMatrix = new Matrix(this.numReceivers, this.numReceivers);

        Logger.debug("TEST sending test messages");
        for (int i = 0; i < testMessages.size(); i++) {
            IndexCodingMessage m = IndexCodingMessage.buildTestMessage(testMessages.get(i).device.getId());
            m.roundType = IndexCodingMessage.ROUND_FIRST;
            m.messageString = IndexCodingMessage.binaryToHex(finalMessage);
            m.messageSize = 1;
            m.destinationDeviceIdx = testMessages.get(i).device.getDeviceIdx();
            m.numDevices = this.numReceivers;

            m.fullMessage = m.buildTestFullMessage();

            Logger.debug("TEST message --> "+m.toString());
            Logger.debug(m.fullMessage);
            activity.newMsg(testMessages.get(i).device.getId(), m.fullMessage);
        }

    }
}
