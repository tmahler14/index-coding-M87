/**
 * Controls for experiment
 *
 * @author - Tim Mahler
 */

package com.m87.sam.ui.util;

import android.os.Build;

public class Controls {
    private static Controls instance = null;

    public double receiveProb = 0.75; // Fictitious chances the receiver receives the message
    public Boolean isTransmitter = false;   // Set to true if transmitting devicxze
    public int numTests = 1;

    // Algo type
    //  - 0: greedy
    //  - 1: clique
    public int algoType;

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
    public String publishMessage;

    // Control instance
    public static Controls getInstance() {
        if(instance == null) {
            instance = new Controls();
        }
        return instance;
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
}
