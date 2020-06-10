package com.ffudp.dbo;

import java.io.Serializable;
import java.util.Date;

public class PkObTask implements Serializable {
    private String tkid;//飞防任务
    private Date speedtime;//设备ID

    public String getTkid() {
        return tkid;
    }

    public Date getSpeedtime() {
        return speedtime;
    }

    public void setTkid(String tkid) {
        this.tkid = tkid;
    }

    public void setSpeedtime(Date speedtime) {
        this.speedtime = speedtime;
    }

    @Override
    public String toString() {
        return "PkObTask{" +
                "tkid=" + tkid +
                ", speedtime=" + speedtime +
                '}';
    }
}
