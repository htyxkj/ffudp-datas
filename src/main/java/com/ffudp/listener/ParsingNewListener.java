package com.ffudp.listener;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.UdpDataInfo;
import com.ffudp.serv.UDPDataServiceNew;
import com.ffudp.utils.Tools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
@Service
@Slf4j
public class ParsingNewListener implements MessageListener {
    @Autowired
    @Lazy
    private DBInvoke invoke;
    @Autowired
    @Lazy
    private RedisTemplate redisTemplate;

//    ExecutorService threadPool = Executors.newFixedThreadPool(1);
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] bs = message.getBody();
            String inf = new String(bs);
            if(!StringUtils.isEmpty(inf)) {
                inf = inf.substring(1, inf.length()-1);
                byte[] cc  = Base64.getDecoder().decode(inf.getBytes());
                inf = new String(cc);
                log.info("获取到数据:"+inf);
                if (inf.length() > 0) {
                    Runnable runnable = new ParseRunnable(inf,redisTemplate,invoke);
                    runnable.run();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

@Slf4j
class ParseRunnable implements Runnable{
    @Autowired
    private DBInvoke invoke;
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    @Autowired
    private RedisTemplate redisTemplate;
    private String inf;
    public ParseRunnable(String inf,RedisTemplate redisTemplate,DBInvoke invoke){
        this.inf = inf;
        this.invoke = invoke;
        this.redisTemplate = redisTemplate;
    }
    @Override
    public void run() {
        try {
            UdpDataInfo info = new UdpDataInfo();
            info.strInfo = Base64.getEncoder().encodeToString(inf.getBytes());
            info.datetime = new Date();
            info.typeStr = "OTH";
            int index = inf.indexOf(ICL.DIV_1E);
            while (index !=-1){
                String str = inf.substring(0,index);
                String s0 = str;
                int _idx = s0.indexOf(ICL.DIV_1F);
                int _idx2 = s0.indexOf(ICL.DIV_1F,_idx+1);
                if(_idx2 == -1){
                    break;
                }
                String strTime = s0.substring(_idx+1,_idx2);//数据时间戳
                long d1 = Long.parseLong(strTime);//数据时间
                d1 = d1/1000;
                d1 = d1*1000;
                s0 = s0.substring(_idx2+1);
                _idx = s0.indexOf(ICL.DIV_1F);
                String sbid = s0.substring(0,_idx);//设备编码
                s0 = s0.substring(_idx+1);

                String key0 =  sbid+ ICL.DIV_D+d1;//设备编码_数据时间
                info.sbid = Long.parseLong(sbid);
                synchronized (key0.intern()) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(d1);
                    String currKey = ICL.CURR_KEY + sbid + ICL.DIV_D + Tools.getSplitZu(cal);//分段KEY
                    if(!redisTemplate.hasKey(currKey)){//没有分段记录
                        SaveDBTask saveDBTask = new SaveDBTask(currKey,invoke,redisTemplate);
                        executorService.submit(saveDBTask);
                    }
                    ObTaskB tskB = null;
                    if (redisTemplate.hasKey(key0)) {
                        JSONObject info0 = (JSONObject) redisTemplate.opsForValue().get(key0);
                        tskB = JSONObject.parseObject(info0.toJSONString(), ObTaskB.class);
                    } else {
                        tskB = new ObTaskB();
                        redisTemplate.opsForList().rightPush(currKey, key0);
                        redisTemplate.expire(currKey, 10, TimeUnit.MINUTES);
                        tskB.setDatetime(new Date());//数据上传时间
                    }
                    tskB.setSbid(Long.parseLong(sbid));
                    tskB.setSpeedtime(cal.getTime());
                    if (str.startsWith("G")) {//GPS 数据
                        tskB = makeGPSData(s0,tskB);
                        info.typeStr = "GPS";
                        info.type = 5;
                    } else if (str.startsWith("C")) {//传感器 数据
                        tskB = makeInfoData(s0,tskB);
                        info.typeStr = "DATA-INFO";
                        info.type = 8;
                    }else{
                        info.typeStr = "OTH";
                        info.strInfo = "OTH";
                    }
                    redisTemplate.opsForValue().set(key0,tskB,10, TimeUnit.MINUTES);
                    if (tskB.getLatitude() != 0 && tskB.getLongitude() != 0) {
                        redisTemplate.opsForValue().set(sbid, tskB, 24, TimeUnit.HOURS);
                    }
                    info.tmid = d1;
                    info.speedtime = tskB.getSpeedtime();
                    info.datetime = new Date();

                    if(inf.length()>index){
                        inf = inf.substring(index+1);
                    }
                    index = inf.indexOf(ICL.DIV_1E);
                    if(inf.length()<50){
                        index =-1;
                    }
                }
            }
            invoke.insertFFLogData(info);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //解析GPS数据
    public ObTaskB makeGPSData(String s0,ObTaskB taskB){
        if(!StringUtils.isEmpty(s0)){
//          G{1F}1591780157000{1F}863284044714018{1F}#IMEI:863284044714018 Time:2020-06-10 17:09:17 LNG:113.853472E LAT:22.585746N ALT:26.000000 SPEED:0.048152
            s0 = s0.substring(1);//去掉#号
            int _idx = s0.indexOf(" ");
            while (_idx>0){
                String s1 = "";
                if(s0.startsWith("TIME")){
                    _idx = s0.indexOf(" ",_idx+1);
                    s1 = s0.substring(0,_idx);
                    int index = s1.indexOf(":");
                    String[] kv = new String[2];
                    kv[0] = s1.substring(0,index);
                    kv[1] = s1.substring(index+1);
                    taskB = makeTaskB(taskB, kv);
                }else{
                    s1 = s0.substring(0,_idx);
                    String[] kv = s1.split(":");
                    taskB = makeTaskB(taskB, kv);
                }
                s0 = s0.substring(_idx+1);
                _idx = s0.indexOf(" ");
            }
            if(s0.length()>0){
                taskB = makeTaskB(taskB, s0.split(":"));
            }
        }
        return taskB;
    }

    //解析传感器数据
    public ObTaskB makeInfoData(String s0,ObTaskB taskB){
//        C{1F}1591780499000{1F}863284044714018{1F}{01}{02}{03}{04}{05}{06}{07}{08}{09}{1E}
//        byte[] bs = hexStr2Byte(s0);
        s0 = s0.replaceAll("\n","");
        s0 = s0.replaceAll("\r","");
        byte[] bs = s0.getBytes();
        String str = bytesToHexString(bs);
        int index = str.indexOf("0104");
        if(index>-1){
            str = str.substring(index);
        }
        byte[] bbs = hexStr2Byte(str);
        taskB = getFlowData(taskB,bbs);
        if(taskB.getSumflow() <= 1 || taskB.getFlow() <=0.00001 ){
            log.info(taskB.toString());
            log.info("解析传感器数据错误，总流量为0 ："+str);
        }
        return taskB;
    }

    //生成GPS TaskB
    private ObTaskB makeTaskB(ObTaskB task, String[] kv) {
        if("IMEI".equals(kv[0])){
            if(StringUtils.isEmpty(kv[1])){
                kv[1] = "0";
            }
            task.setSbid(Long.parseLong(kv[1]));
        }
        if("TIME".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            try {
                task.setSpeedtime(DateFormat.getDateTimeInstance().parse(kv[1]));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if("LNG".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            k0 = k0.substring(0,k0.length()-1);
            task.setLongitude(Float.parseFloat(k0));
        }
        if("LAT".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            k0 = k0.substring(0,k0.length()-1);
            task.setLatitude(Float.parseFloat(k0));
        }
        if("ALT".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            task.setHeight(Float.parseFloat(k0));
        }
        if("SPEED".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            task.setSpeed(Float.parseFloat(k0));
        }
        return task;
    }

    //生成传感器 TaskB
    public static ObTaskB getFlowData(ObTaskB taskB, byte[] bs){
        try {
            if(bs.length>= 30) {
                byte sbaddr = bs[0];//设备地址
                byte readcmd = bs[1];//读命令
                if (sbaddr == 1 && readcmd == 4) {
                    byte[] ssll = new byte[4];//瞬时流量
                    System.arraycopy(bs, 3, ssll, 0, ssll.length);
                    float flow = Tools.bytes2Float(ssll);
                    flow = Tools.keepDecimal(flow,6);
                    taskB.setFlow(flow);//瞬时流量
                    byte[] total = new byte[8];//总量
                    System.arraycopy(bs, 9, total, 0, total.length);
                    float sumFlow = Tools.bytes2Float(total);
                    sumFlow = Tools.keepDecimal(sumFlow,6);
                    taskB.setSumflow(sumFlow);//总量
                    byte[] temp = new byte[4];//温度
                    System.arraycopy(bs, 19, temp, 0, temp.length);
                    float temper = Tools.bytes2Float(temp);
                    temper = Tools.keepDecimal(temper,6);
                    taskB.setTemperature(temper);//温度
                    temp = new byte[4];//压力
                    System.arraycopy(bs, 23, temp, 0, temp.length);
                    float press = Tools.bytes2Float(temp);
                    press = Tools.keepDecimal(press,6);
                    taskB.setPressure(press);//压力
                    temp = new byte[4];//总量
                    System.arraycopy(bs, 27, temp, 0, temp.length);
                    sumFlow = Tools.bytes2Float(temp);
                    sumFlow = Tools.keepDecimal(sumFlow,6);
                    taskB.setSumflow(sumFlow);//总量
                }
            }
        }catch (Exception e){
            log.error("解析传感器数据错误",e);
        }finally {
            return taskB;
        }
    }

    //16进制转byte[]
    public static byte[] hexStr2Byte(String hex) {
        ByteBuffer bf = ByteBuffer.allocate(hex.length() / 2);
        for (int i = 0; i < hex.length(); i++) {
            String hexStr = hex.charAt(i) + "";
            i++;
            hexStr += hex.charAt(i);
            byte b = (byte) Integer.parseInt(hexStr, 16);
            bf.put(b);
        }
        return bf.array();
    }

    public static void main(String[] args) {
        String str = "01041C20202020200306471E5B2063403F20012020202020202020031944403F3F";

        byte[] bs = hexStr2Byte(str);
        ObTaskB tkb = new ObTaskB();
        tkb = getFlowData(tkb,bs);
        System.out.println(tkb.toString());
    }

    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }
}