package com.m87.sam.ui.pojos;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.m87.sam.R;
import com.m87.sam.ui.activity.HomeActivity;
import com.m87.sam.ui.activity.HomeFragment;
import com.m87.sam.ui.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static com.m87.sam.ui.pojos.FileReader.convertResStreamToByteArray;

/**
 * Created by tim-azul on 8/13/17.
 */

public class FinalDemoTest extends Thread {
    public Context context;
    public HomeActivity activity;
    public int numReceivers;

    public ArrayList<IndexCodingMessage> messageList = new ArrayList<IndexCodingMessage>();
    public ArrayList<IndexCodingMessage> initMessageList = new ArrayList<IndexCodingMessage>();
    public ArrayList<IndexCodingMessage> round1MessageList = new ArrayList<>();
    public ArrayList<IndexCodingMessage> successMessages = new ArrayList<>();
    public ArrayList<DemoMessage> testMessages = new ArrayList<DemoMessage>();
    public int[] retransmissionDevices;

    public Matrix entireMatrix;
    public int[] diags;
    public Matrix reducedMatrix;

    public int MESSAGE_SIZE = 4;

    public FinalDemoTest(Context context, HomeActivity activity){
        this.context = context;
        this.activity = activity;
        this.numReceivers = HomeFragment.neighborList.size() - 1;
    }

    // Message containing both the device and actual binary message
    public class DemoMessage {

        public M87ProximityDevice device;
        public String binaryMessage;
        public int byteMsgIndex;
        public byte[] byteMessageArr;
        public String lastHexMessage;

        public DemoMessage(M87ProximityDevice d, byte[] a) {
            this.device = d;
            this.byteMessageArr = a;
            this.byteMsgIndex = 0;
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

        Toast msg = Toast.makeText(this.context, "DEMO: Sending init messages", Toast.LENGTH_SHORT);
        msg.show();

        for (int i = 0; i < HomeFragment.neighborList.size(); i++) {
            Logger.debug("TEST: Neighbor = "+HomeFragment.neighborList.get(i).getId());

            if (!HomeFragment.neighborList.get(i).isSelf()) {
                IndexCodingMessage initMessage = IndexCodingMessage.buildInitMessage(HomeFragment.neighborList.get(i).getId(), 1);
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

        // Resend messages
        if (hasReceivedAllSuccessMessages()) {

            int status = sendNewMessageBatch();

            if (status <= 0) {
                Toast msg = Toast.makeText(this.context, "Test DONE", Toast.LENGTH_LONG);
                msg.show();

                sendTestDoneMessages();

            }

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

        String[] originalMessages = new String[retransmissionMatrix.colSize];

        for (int i = 0; i < retransmissionDevices.length; i++) {
            DemoMessage m = getMessageGivenDeviceIndex(retransmissionDevices[i]);
            Logger.debug("MESSAGE14");
            Logger.debug(m.toString());
            originalMessages[i] = m.lastHexMessage;
        }

        Logger.debug("ORIGINAL");
        Logger.debug(Arrays.toString(originalMessages));

        String[] retransmissionMessages = IndexCoding.constructRetransmissionMessagesHex(retransmissionMatrix, originalMessages);

        Logger.debug("R MESSAGES");
        Logger.debug(Arrays.toString(retransmissionMessages));

        // Send retransmission messages
        Logger.debug("TEST sending retransmission messages");
        for (int i = 0; i < retransmissionDevices.length; i++) {
            DemoMessage msg = getMessageGivenDeviceIndex(retransmissionDevices[i]);

            // Send each retransmission device
            for (int j = 0; j < retransmissionMessages.length; j++) {
                IndexCodingMessage m = IndexCodingMessage.buildTestMessage(msg.device.getId());
                m.roundType = IndexCodingMessage.ROUND_RETRANSMITTION_FINAL;
                m.messageString = retransmissionMessages[j];
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

    public DemoMessage getMessageGivenDeviceIndex(int index) {
        DemoMessage m = null;
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

    public void startTest() {

        Logger.debug("TEST Start test");

        Logger.debug("NUM REC:"+this.numReceivers);

        Toast msg = Toast.makeText(this.context, "Init received, starting test", Toast.LENGTH_SHORT);
        msg.show();

        // For each init message, read in image file
        for (int i = 0; i < initMessageList.size(); i++) {
            byte [] a = getDeviceFile(initMessageList.get(i).sourceDevice.label);

            Logger.debug("GOT BYTES");
            Logger.debug(Arrays.toString(a));
            Logger.debug("Length");
            Logger.debug(Integer.toString(a.length));

            DemoMessage x = new DemoMessage(initMessageList.get(i).sourceDevice, a);
            testMessages.add(x);
        }

        sendNewMessageBatch();


    }

    public int sendNewMessageBatch() {
        int numBytesPerMessage = 200;

        entireMatrix = new Matrix(this.numReceivers, this.numReceivers);
        round1MessageList = new ArrayList<>();
        successMessages = new ArrayList<>();

        // Create message
        String finalStr = "";
        for (int i = 0; i < testMessages.size(); i++) {
            DemoMessage z = testMessages.get(i);

            String hex = "";
            for (int j = 0; j < numBytesPerMessage/2; j++) {
                if (z.byteMsgIndex >= z.byteMessageArr.length) {
                    return -1;
                } else {
                    hex += String.format("%02X", z.byteMessageArr[z.byteMsgIndex++]);
                }
            }

            z.lastHexMessage = hex;

            finalStr += hex;
        }

        Logger.debug("TEST sending test messages");
        for (int i = 0; i < testMessages.size(); i++) {
            IndexCodingMessage m = IndexCodingMessage.buildTestMessage(testMessages.get(i).device.getId());
            m.roundType = IndexCodingMessage.ROUND_FIRST;
            m.messageString = finalStr;
            m.messageSize = numBytesPerMessage;
            m.destinationDeviceIdx = testMessages.get(i).device.getDeviceIdx();
            m.numDevices = this.numReceivers;

            m.fullMessage = m.buildTestFullMessage();

            Logger.debug("TEST message --> "+m.toString());
            Logger.debug(m.fullMessage);
            activity.newMsg(testMessages.get(i).device.getId(), m.fullMessage);
        }

        return 1;
    }

    public void sendTestDoneMessages(){
        for (int i = 0; i < testMessages.size(); i++) {
            IndexCodingMessage m = IndexCodingMessage.buildTestMessage(testMessages.get(i).device.getId());
            m.roundType = IndexCodingMessage.ROUND_TEST_DONE;
            m.messageString = "1";
            m.messageSize = 1;
            m.destinationDeviceIdx = testMessages.get(i).device.getDeviceIdx();
            m.numDevices = this.numReceivers;

            m.fullMessage = m.buildTestFullMessage();

            Logger.debug("TEST message --> "+m.toString());
            Logger.debug(m.fullMessage);
            activity.newMsg(testMessages.get(i).device.getId(), m.fullMessage);
        }
    }

    public byte[] getDeviceFile(String deviceLabel) {

        InputStream inStream;

        if (deviceLabel.contains("1")){
            inStream = context.getResources().openRawResource(R.raw.demo_one_small);
        }
        else if (deviceLabel.contains("2")){
            inStream = context.getResources().openRawResource(R.raw.demo_two_small);
        }
        else if (deviceLabel.contains("3")){
            inStream = context.getResources().openRawResource(R.raw.demo_three_small);
        }
        else {
            inStream = context.getResources().openRawResource(R.raw.demo_four_small);
        }

        byte[] arr = new byte[]{};
        try {
            arr = FileReader.convertResStreamToByteArray(inStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return arr;
        }
    }
}
