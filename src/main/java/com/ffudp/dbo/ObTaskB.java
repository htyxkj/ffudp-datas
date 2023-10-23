package com.ffudp.dbo;

import lombok.Data;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "ob_taska")
@IdClass(PkObTask.class)
@Proxy(lazy = false)
public class ObTaskB  implements Serializable {
    public ObTaskB(){}
    @Id
    @Column(columnDefinition = "tkid")
    private String tkid;//飞防任务
    @Id
    private Date speedtime;//飞行时间
    private Long sbid;//设备ID
    private float longitude;//经度
    private float latitude;//纬度
    private float speed;//速度
    private float height;//高度
    private float flow;//流量
    private float sumfolw;//总流量
    private float temperature;//温度
    private float pressure;//压力
    private float pressure2;//压力2
    private int effective;//定位状态是否有效
    private String direction;//方向
    private Date datetime;//保存时间/上传时间
    private float humidity;//湿度
    private float windspeed;//风速

    @Transient
    private int parsingNum = 0;//解析次数 判断是否对gps 传感器都解析过
    @Transient
    private int data_type = 1;//1正常数据 0 异常数据获取的上一秒记录

    public float getHumidity() {
        return humidity;
    }

    public float getWindspeed() {
        return windspeed;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public void setWindspeed(float windspeed) {
        this.windspeed = windspeed;
    }

    public String geTkid() {
        return tkid;
    }
    public void setTkid(String id) {
        this.tkid = id;
    }

    public Long getSbid() {
        return sbid;
    }

    public void setSbid(Long sbid) {
        this.sbid = sbid;
    }

    public Date getSpeedtime() {
        return speedtime;
    }

    public void setSpeedtime(Date speedtime) {
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

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public float getSumflow() {
        return sumfolw;
    }

    public void setSumflow(float sumFlow) {
        this.sumfolw = sumFlow;
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

    public float getPressure2() {
        return pressure2;
    }

    public void setPressure2(float pressure2) {
        this.pressure2 = pressure2;
    }

    public boolean isEffective() {
        return effective ==0?false:true;
    }

    public int getData_type() {
        return data_type;
    }

    public void setData_type(int data_type) {
        this.data_type = data_type;
    }

    public void setEffective(boolean effective) {
        if(effective)
            this.effective =1;
        else
            this.effective =0;
    }
    public int getEffective() {
        return this.effective;
    }
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
    @Override
    public String toString() {
        return "ObTaskB{" +
                "tkid='" + tkid + '\'' +
                ", speedtime=" + speedtime +
                ", sbid=" + sbid +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", speed=" + speed +
                ", height=" + height +
                ", flow=" + flow +
                ", sumfolw=" + sumfolw +
                ", temperature=" + temperature +
                ", pressure=" + pressure +
                ", pressure2=" + pressure2 +
                ", effective=" + effective +
                ", direction='" + direction + '\'' +
                ", datetime=" + datetime +
                '}';
    }

    public int getParsingNum() {
        return parsingNum;
    }

    public void setParsingNum(int parsingNum) {
        this.parsingNum = parsingNum;
    }
}
