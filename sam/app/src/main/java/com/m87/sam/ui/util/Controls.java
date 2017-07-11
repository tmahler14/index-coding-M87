/**
 * Controls for experiment
 *
 * @author - Tim Mahler
 */

package com.m87.sam.ui.util;

import android.os.Build;

public class Controls {
    private static Controls instance = null;

    public double receiveProb = 0.65; // Fictitious chances the receiver receives the message
    public Boolean isTransmitter = false;   // Set to true if transmitting devicxze
    public int numTests = 1;

    // Algo type
    //  - 0: greedy
    //  - 1: clique
    public int algoType = 0;

    // Test type
    //  - 0: basic
    //  - 1: final demo
    public int testType = 0;

    public Boolean useIndexCoding = true;   // true if use index coding

    public Boolean sendResults = false; // true if send results to server

    public static final String serialNumber = Build.SERIAL;

    // Subscribe channel
    public static final String SUBSCRIBE_CHANNEL = "sam";
    public static final int SUBSCRIBE_RANGE = 6;

    // Serial number of device (tell which device is which)
    public static final String TX1_SERIAL = "062a20f200511a45";
    public static final String RX1_SERIAL = "04f227430b35e69a";
    public static final String RX2_SERIAL = "07f0fb7d0c8cebda";
    public static final String RX3_SERIAL = "0647de8e00612817";
    public static final String RX4_SERIAL = "038ca0dbf0b59709";
    public static final String RX5_SERIAL = "06c23ed00075d4f3";

    // Device id representation
    public static final String TX1 = "TX1";
    public static final String RX1 = "RX1";
    public static final String RX2 = "RX2";
    public static final String RX3 = "RX3";
    public static final String RX4 = "RX4";
    public static final String RX5 = "RX5";

    // Subscribe messages
    public static final String TX1_PUBLISH = SUBSCRIBE_CHANNEL+"."+TX1;
    public static final String RX1_PUBLISH = SUBSCRIBE_CHANNEL+"."+RX1;
    public static final String RX2_PUBLISH = SUBSCRIBE_CHANNEL+"."+RX2;
    public static final String RX3_PUBLISH = SUBSCRIBE_CHANNEL+"."+RX3;
    public static final String RX4_PUBLISH = SUBSCRIBE_CHANNEL+"."+RX4;
    public static final String RX5_PUBLISH = SUBSCRIBE_CHANNEL+"."+RX5;

    public String deviceId;
    public String deviceLabel;
    public String publishMessage;
    public String subscribeMessage;

    // Control instance
    public static Controls getInstance() {
        if(instance == null) {
            instance = new Controls();
        }
        return instance;
    }

    public Controls() {
        subscribeMessage = SUBSCRIBE_CHANNEL;
        Logger.debug("Serial");
        Logger.debug(serialNumber);
        switch (serialNumber) {
            case TX1_SERIAL:
                publishMessage = TX1_PUBLISH;
                isTransmitter = true;
                deviceLabel = TX1;
                break;
            case RX1_SERIAL:
                publishMessage = RX1_PUBLISH;
                isTransmitter = false;
                deviceLabel = RX1;
                break;
            case RX2_SERIAL:
                publishMessage = RX2_PUBLISH;
                isTransmitter = false;
                deviceLabel = RX2;
                break;
            case RX3_SERIAL:
                publishMessage = RX3_PUBLISH;
                isTransmitter = false;
                deviceLabel = RX3;
                break;
            case RX4_SERIAL:
                publishMessage = RX4_PUBLISH;
                isTransmitter = false;
                deviceLabel = RX4;
                break;
            case RX5_SERIAL:
                publishMessage = RX5_PUBLISH;
                isTransmitter = false;
                deviceLabel = RX5;
                break;
        }
    }

    // Getter and setters
    public double getReceiveProb() {
        return receiveProb;
    }

    public void setReceiveProb(double receiveProb) {
        this.receiveProb = receiveProb;
    }

    public Boolean getTransmitter() {
        return isTransmitter;
    }

    public void setTransmitter(Boolean transmitter) {
        isTransmitter = transmitter;
    }

    public int getNumTests() {
        return numTests;
    }

    public void setNumTests(int numTests) {
        this.numTests = numTests;
    }

    public int getAlgoType() {
        return algoType;
    }

    public void setAlgoType(int algoType) {
        this.algoType = algoType;
    }

    public Boolean getUseIndexCoding() {
        return useIndexCoding;
    }

    public void setUseIndexCoding(Boolean useIndexCoding) {
        this.useIndexCoding = useIndexCoding;
    }

    public int getTestType() {
        return testType;
    }

    public void setTestType(int testType) {
        this.testType = testType;
    }

    public Boolean getSendResults() {
        return sendResults;
    }

    public void setSendResults(Boolean sendResults) {
        this.sendResults = sendResults;
    }
}
