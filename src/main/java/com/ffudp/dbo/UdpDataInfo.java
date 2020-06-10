package com.ffudp.dbo;

import java.io.Serializable;
import java.util.Date;

/**
 * 监听udp端口接收到的数据
 */
public class UdpDataInfo implements Serializable {
    public long tmid;//时间点，long类型
    public long sbid;//设备号
    public int type;//数据类型
    public String dmt;//格式化以后的时间
    public String typeStr;//字符串数据类型
    public String strInfo;//字符串数据信息（可能会出现乱码）
    public byte[] bs;//byte类型数据
    public Date speedtime;//时间点
    public String tkid;//任务号
    public Date datetime;//系统当前时间/数据接收时间
    public String hexData;//

}
