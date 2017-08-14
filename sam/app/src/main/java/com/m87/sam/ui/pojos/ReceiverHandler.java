package com.m87.sam.ui.pojos;

import android.widget.Toast;

import com.m87.sam.ui.activity.HomeActivity;
import com.m87.sam.ui.util.Controls;
import com.m87.sam.ui.util.Logger;

import java.math.BigInteger;
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
    public Matrix roundMessageMatrix;
    public int roundMessageMatrixPtr;
    public Matrix gaussianMatrix;
    public boolean success = false;
    public String myMessage;
    public int test_type;
    public ArrayList<Byte> myDemoMessage = new ArrayList<Byte>();

    public ReceiverHandler(HomeActivity activity, int test_type) {

        this.activity = activity;

        this.test_type = test_type;
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
        IndexCodingMessage initMessage = IndexCodingMessage.buildInitMessage(m.sourceDevice.getId(), this.test_type);

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
                handleRetransmissionMessages(m);
                break;

            // Intermediate round of retransmission
            case IndexCodingMessage.ROUND_RETRANSMITTION_SUCCESS:
                break;

            // Final test done
            case IndexCodingMessage.ROUND_TEST_DONE:
                handleTestDoneMessages(m);
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

        String[] messages = extractMessages(m.messageSize, m.hexString);
        Logger.debug("Mess in bytes -->");

        Logger.debug(Arrays.toString(messages));

        // Clear the array of round messages
        roundMessages.clear();
        roundMessageMatrix = createRoundMatrix(m);  // create new matrix of messages received
        roundMessageMatrixPtr = 0;
        success = false;
        String receiveMsgStr = randomlyDropMessages(m, messages);

        roundMessageMatrix.show();  // print matrix

        // Send back message to transmitter declaring which messages were received
        IndexCodingMessage returnMessage = IndexCodingMessage.buildTestMessage(m.sourceDevice.getId());
        returnMessage.roundType = IndexCodingMessage.ROUND_FIRST;
        returnMessage.messageString = receiveMsgStr;
        returnMessage.messageSize = m.messageSize;
        returnMessage.destinationDeviceIdx = m.destinationDeviceIdx;
        returnMessage.numDevices = m.numDevices;

        returnMessage.fullMessage = returnMessage.buildTestFullMessage();

        Logger.debug("Return msg --> "+returnMessage.toString());

        activity.newMsg(returnMessage.destinationDevice.getId(), returnMessage.fullMessage);
    }

    public void handleRetransmissionMessages(IndexCodingMessage m) {
        Logger.debug("Hanldes retransmission round");

        roundMessageMatrix.setRow(roundMessageMatrixPtr, IndexCodingMessage.convertStringToBinaryArray(m.numDevices, Integer.toString(Integer.parseInt(m.retransmissionMetaData, 2))));
        roundMessageMatrixPtr++;
        roundMessages.add(m.messageString);

        if (gaussianMatrix == null) {
            gaussianMatrix = GaussianElimination.createGaussianMatrix(roundMessageMatrix);
        }

        Logger.debug("gaussianMatrix");
        gaussianMatrix.show();

        Logger.debug("gaussian elim");
        GaussianElimination.printMatrix(GaussianElimination.run(gaussianMatrix.vals));

        // Send success messages
        if (!success) {
            success = true;
            if (success) {
                IndexCodingMessage returnMessage = IndexCodingMessage.buildTestMessage(m.sourceDevice.getId());

                returnMessage.roundType = IndexCodingMessage.ROUND_RETRANSMITTION_SUCCESS;
                returnMessage.messageString = "1";
                returnMessage.messageSize = m.messageSize;
                returnMessage.destinationDeviceIdx = m.destinationDeviceIdx;
                returnMessage.numDevices = m.numDevices;

                returnMessage.fullMessage = returnMessage.buildTestFullMessage();

                Logger.debug("Return msg --> " + returnMessage.toString());

                activity.newMsg(returnMessage.destinationDevice.getId(), returnMessage.fullMessage);

//                Toast msg = Toast.makeText(this.activity, "My message = "+myMessage, Toast.LENGTH_LONG);
//                msg.show();

            }
        }
    }

    public void handleTestDoneMessages(IndexCodingMessage m) {
        int rand = 0 + (int)(Math.random() * 4000);

        try {
            Thread.sleep(rand);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.activity.showImageViewLayout();

    }

    /**
     * Create new round of message matrix
     * @param m
     * @return
     */
    public Matrix createRoundMatrix(IndexCodingMessage m) {
        return new Matrix(m.numDevices*2, m.numDevices);
    }

    /**
     * Randomly drops messages given the string and sends back status msg
     *  - Aka if 5 messages total, and recieved 1,3,5, then return would be '10101'
     *
     * @param messages
     * @return
     */
    public String randomlyDropMessages(IndexCodingMessage m, String[] messages) {
        Random random = new Random();
        String receiveStr = "";
        for (int i = 0; i < messages.length; i++) {
            double flip = (random.nextInt(100)+1) / 100.0;

            if (Controls.getInstance().dontReceiveSelf && (i == m.destinationDeviceIdx)) {
                Logger.debug("Dropped!");
                receiveStr += "0";
                myMessage = messages[i];
                if (test_type == 1) {
                    addDemoMessage(myMessage);
                }

            }
            else if ( flip <= Controls.getInstance().getReceiveProb() ) {
                Logger.debug("Receieved!");
                roundMessages.add(messages[i]);
                roundMessageMatrix.setVal(roundMessageMatrixPtr, i, 1);
                roundMessageMatrixPtr++;
                receiveStr += "1";
            }
            else {
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

    public void addDemoMessage(String message) {
        byte[] b = new BigInteger(message,16).toByteArray();

        for (int i = 0; i < b.length; i++) {
            myDemoMessage.add(b[i]);
        }
    }

}
