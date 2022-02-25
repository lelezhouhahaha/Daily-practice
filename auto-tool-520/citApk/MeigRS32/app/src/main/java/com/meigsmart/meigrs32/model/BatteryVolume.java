package com.meigsmart.meigrs32.model;

/**
 * Created by chenMeng on 2018/5/14.
 */
public class BatteryVolume {
    private String status ;
    private int level;// battery charge is
    private String plugged;//battery content
    private int voltage;// battery voltage

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getPlugged() {
        return plugged;
    }

    public void setPlugged(String plugged) {
        this.plugged = plugged;
    }

    public int getVoltage() {
        return voltage;
    }

    public void setVoltage(int voltage) {
        this.voltage = voltage;
    }
}

