package com.ffudp.dbo;

import java.io.Serializable;

public class OBItemInfo implements Serializable {
    public OBItemInfo(){}
    private String tkid;//飞防任务
    private Long id;//设备ID
    private String speedtime;//飞行时间
    private float longitude;//经度
    private float latitude;//纬度
    private float speed;//速度
    private float height;//高度
    private float flow;//流量
    private float sumfolw;//总流量
    private float temperature;//温度
    private float pressure;//压力
    private String datetime;//时间
    private boolean Effective;//定位状态是否有效
    private String direction;//方向

    public String getTkid() {
        return tkid;
    }

    public void setTkid(String tkid) {
        this.tkid = tkid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpeedtime() {
        return speedtime;
    }

    public void setSpeedtime(String speedtime) {
        this.speedtime = speedtime;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getFlow() {
        return flow;
    }

    public void setFlow(float flow) {
        this.flow = flow;
    }

    public float getSumfolw() {
        return sumfolw;
    }

    public void setSumfolw(float sumfolw) {
        this.sumfolw = sumfolw;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public boolean isEffective() {
        return Effective;
    }

    public void setEffective(boolean effective) {
        Effective = effective;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
