package com.m87.sam.ui.pojos;

import com.m87.sdk.ProximityEntry;


public class M87ProximityDevice {

    public int id;
    public String label;
    public boolean isSelf;
    public int deviceIdx;

    public String metaData;
    public String expression;
    public int connectionStatus;
    public int hopCount;

    public M87ProximityDevice(ProximityEntry e) {
        this.id = e.getId();
        this.connectionStatus = e.getConnectionStatus();
        this.metaData = e.getMetaData();
        this.isSelf = e.isSelf();
        this.hopCount = e.getHopCount();
        this.expression = e.getExpression();
    }

    public static void copy(M87ProximityDevice n, ProximityEntry e) {
        n.id = e.getId();
        n.connectionStatus = e.getConnectionStatus();
        n.metaData = e.getMetaData();
        n.isSelf = e.isSelf();
        n.hopCount = e.getHopCount();
        n.expression = e.getExpression();
    }

    // Getters and setters


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean self) {
        isSelf = self;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public int getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(int connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public int getDeviceIdx() {
        return deviceIdx;
    }

    public void setDeviceIdx(int deviceIdx) {
        this.deviceIdx = deviceIdx;
    }

    @Override
    public String toString() {
        return String.format("id: %d, label = %s, isSelf = %s, deviceIdx = %d", this.id, this.label, this.isSelf, this.deviceIdx);
    }
}
