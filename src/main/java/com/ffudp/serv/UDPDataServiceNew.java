package com.ffudp.serv;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.OBItemInfo;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.dbo.UdpDataInfo;
import com.ffudp.listener.SaveDBTask;
import com.ffudp.msg.PublishService;
import com.ffudp.utils.Tools;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 监听数据服务
 */
@Slf4j
@Service
public class UDPDataServiceNew {
    ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Value("${server.prefix}")
    private static String prefix;

    @Autowired
    private DBInvoke invoke;
    @Autowired
    private PublishService publishService;
    @Autowired
    private RedisTemplate redisTemplate;

    private String SK = "SAVE"+ICL.DIV_D+"KEY";

    public void service(byte[] bc,int offset,int len){
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if(len>=16) {
                UdpDataInfo logInfo = new UdpDataInfo();
                int type = Tools.getIntValue(bc, offset);//数据类型
                logInfo.type = type;
                String  CHART="GB2312";
                int sbid = Tools.getIntValue(bc,offset+4);//设备编码
                logInfo.sbid = sbid;
                long d1 = Tools.getLongValue(bc,offset+8);//数据时间
                d1 = d1/1000;
                d1 = d1*1000;
                logInfo.tmid = d1;
                String key0 =  sbid+ ICL.DIV_D+d1;//设备编码_数据时间
                String saveKey = ICL.SAVE_KEY+ICL.DIV_D+Tools.getSplitZu();
                redisTemplate.opsForSet().add(saveKey, key0);
                synchronized (key0.intern()) {
                    log.info(key0);
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(d1);
                    String dmt = Tools.dateToString(cal, true, 8);
                    if (dmt.indexOf(":") == -1) {
                        dmt = dmt + " 00:00:00";
                    }
                    String currKey = ICL.CURR_KEY + sbid + ICL.DIV_D + Tools.getSplitZu(cal);//分段KEY
//                    if(!redisTemplate.hasKey(currKey)){//没有分段记录
//                        SaveDBTask saveDBTask = new SaveDBTask(currKey,invoke,redisTemplate);
//                        executorService.submit(saveDBTask);
//                    }
                    ObTaskB tskB = null;
                    if (redisTemplate.hasKey(key0)) {
                        JSONObject info0 = (JSONObject) redisTemplate.opsForValue().get(key0);
                        tskB = JSONObject.parseObject(info0.toJSONString(), ObTaskB.class);
                    } else {
                        tskB = new ObTaskB();
                        redisTemplate.opsForList().rightPush(currKey, key0);
                        redisTemplate.expire(currKey, 10, TimeUnit.MINUTES);
                        String dtime = Tools.dateToString(cal, true, 8);
                        if (dtime.indexOf(":") == -1) {
                            dtime = dtime + " 00:00:00";
                        }
                        tskB.setDatetime(simpleDateFormat.parse(dtime));//数据上传时间
                    }

                    byte[] bbs = new byte[bc.length - 16];
                    System.arraycopy(bc, offset + 16, bbs, 0, bbs.length);
                    logInfo.bs = bbs;
                    long dl = System.currentTimeMillis();
                    cal.setTimeInMillis(dl);
                    String t = simpleDateFormat.format(cal.getTime());
                    logInfo.datetime = simpleDateFormat.parse(t);


                    tskB.setSbid((long)sbid);//设备编码
                    tskB.setSpeedtime(simpleDateFormat.parse(dmt));//数据时间
                    logInfo.dmt = dmt;
                    logInfo.speedtime = simpleDateFormat.parse(dmt);
                    if (type == 5) {//GPS 数据
                        String s1 = new String(bc, offset + 16, len - 16, CHART);
                        logInfo.strInfo = s1;
                        int firstD = s1.indexOf(",");
                        if (firstD > 0) {
                            tskB = makeGPSInfo(s1, firstD, tskB, logInfo);
                        }
                    } else if (type == 8) {
                        tskB = makeUserData(bbs, tskB, logInfo);
                    } else {
                        logInfo.typeStr = "OTH";
                        logInfo.strInfo = "OTH:" + type;
                    }
//                    log.info("设备编码：" + sbid);
//                    if (cal.getTimeInMillis() - d1 > 1000) {
//                        log.info("数据解析完成，时间：" + cal.getTimeInMillis() + "数据时间：" + d1);
//                    }

                    redisTemplate.opsForValue().set(key0,tskB,10, TimeUnit.MINUTES);
                    if (logInfo.type != 21) {
                        invoke.insertFFLogData(logInfo);
                    }
                    if (tskB.getLatitude() != 0 && tskB.getLongitude() != 0) {
                        redisTemplate.opsForValue().set(logInfo.sbid + "", tskB, 24, TimeUnit.HOURS);
                    }
                }
            }else {
                int k = Tools.getIntValue(bc,offset);
                log.info("type="+k);
                k = Tools.getIntValue(bc,offset+4);
                log.info("sbid="+k);
                log.info("系统不处理数据>>>>>>数据小于17，数据长度："+len);
            }
        }catch (Exception e){
            log.error("解析数据错误",e);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(baos));
            String exception = baos.toString();
            log.error(exception);
            e.printStackTrace();
        }
    }

    /**
     * 解析GPS数据
     * @param strInfo　数据
     * @param firstD
     * @param tskB　　　
     */
    private ObTaskB makeGPSInfo(String strInfo,int firstD,ObTaskB tskB,UdpDataInfo logInfo) {
        String typeStr = strInfo.substring(0, firstD);
        logInfo.typeStr = typeStr;
        String s1 = strInfo;
        s1 = s1.substring(firstD + 1);
        try {
            if (typeStr.equals("$GPRMC") || typeStr.equals("$GNRMC")) {
                String[] vvs = s1.split(",");
                if ("A".equals(vvs[1])) {
                    float lat = Float.parseFloat(vvs[2]);
                    lat = (int) (lat / 100) + (lat % 100 / 60);
                    tskB.setLatitude(lat);
                    float lng = Float.parseFloat(vvs[4]);
                    lng = (int) (lng / 100) + (lng % 100 / 60);
                    tskB.setLongitude(lng);
                    tskB.setSpeed(Float.parseFloat(vvs[6]) * 1.852f);//速度
                    tskB.setDirection(vvs[7]);//方向
                    String heightKey = tskB.getSbid() + "_height";
                    Object height = redisTemplate.opsForValue().get(heightKey);
                    if (height == null || height.equals(""))
                        height = "0";
                    tskB.setHeight(Float.parseFloat(height.toString()));
                }
            }
            if (typeStr.equals("$GPGGA") || typeStr.equals("$GNGGA")) {
                String[] vvs = s1.split(",");
                if(vvs.length >=14) {
                    if ("1".equals(vvs[5])) {
                        float lat = Float.parseFloat(vvs[1]);
                        lat = (int) (lat / 100) + (lat % 100 / 60);
                        tskB.setLatitude(lat);//纬度
                        float lng = Float.parseFloat(vvs[3]);
                        lng = (int) (lng / 100) + (lng % 100 / 60);
                        tskB.setLongitude(lng);//经度
                    }
                    //获取高度
                    String hi = vvs[8] == null ? "0" : vvs[8];
                    if (hi.length() > 0) {
                        tskB.setHeight(Float.parseFloat(vvs[8]));
                    } else {
                        hi = "0";
                    }
                    String heightKey = tskB.getSbid() + "_height";
                    redisTemplate.opsForValue().set(heightKey, hi, 20, TimeUnit.MINUTES);
                }
            }
            if (tskB.getLatitude() != 0 && tskB.getLongitude() != 0) {
                redisTemplate.opsForValue().set(logInfo.sbid + "", tskB, 24, TimeUnit.HOURS);
            }
        }catch (Exception e){
            log.error("解析GPS数据错误",e,s1);
            e.printStackTrace();
        }
        return tskB;
    }

    /**
     * 解析传感器数据
     * @param bs
     * @param tskB
     * @return
     */
    private ObTaskB makeUserData(byte[] bs, ObTaskB tskB,UdpDataInfo logInfo) {
        logInfo.typeStr = "type8";
        String inf = Tools.bytesToHexString(bs);
        log.info("开始解析传感器数据："+inf);
        if(inf.length()>=46) {
            try {
                if(inf.indexOf(prefix)==0){//字头
                    logInfo.typeStr = "8-DATA";
                    String fl = inf.substring(6,14);//瞬时流量
                    Integer flow = Integer.parseInt(fl, 16);
                    tskB.setFlow((float) (flow/1000.0));
                    String sfld = inf.substring(14,22);//累积低位
                    Integer sumFlowD = Integer.parseInt(sfld, 16);
                    String sflg = inf.substring(22,30);//累积高位
                    Integer sumFlowG = Integer.parseInt(sflg, 16);
                    tskB.setSumflow((float) ((sumFlowD+sumFlowG)/1000.0));
                    String sd = inf.substring(30,34);//湿度
                    Integer humidity = Integer.parseInt(sd, 16);
                    tskB.setHumidity((float) (humidity/10.0));
                    String temp = inf.substring(34,38);//温度
                    Integer temper =  Integer.parseInt(temp, 16);
                    tskB.setTemperature((float) (temper/10.0));
                    String per = inf.substring(38,42);//压力
                    Integer press =  Integer.parseInt(per, 16);
                    tskB.setPressure((float) (press/10.0));
                }
            } catch (Exception e) {
                log.error("error:", e);
                e.printStackTrace();
            }
        }
        return tskB;
    }



    public void saveObTaskBNew(ObTaskB taskB,String inf){
        synchronized (inf.intern()){
            Long id = Tools.getId();
            UdpDataInfo info = new UdpDataInfo();
            info.tmid = Tools.getId();
            info.sbid = taskB.getSbid();
            info.strInfo = inf;
            info.typeStr = "GPS";
            info.type = 5;
            info.speedtime = taskB.getSpeedtime();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(id);
            info.dmt = Tools.dateToString(cal,true,8);

            String key0 =  info.sbid+ ICL.DIV_D+taskB.getSpeedtime().getTime();//设备编码_数据时间
            cal.setTimeInMillis(taskB.getSpeedtime().getTime());

            String currKey = ICL.CURR_KEY + info.sbid + ICL.DIV_D + Tools.getSplitZu(cal);
            synchronized (key0.intern()) {
                if(!redisTemplate.hasKey(currKey)){
                    SaveDBTask saveDBTask = new SaveDBTask(currKey, invoke, redisTemplate);
                    executorService.submit(saveDBTask);
                }
                if (!redisTemplate.hasKey(key0)) {
                    redisTemplate.opsForList().rightPush(currKey, key0);
                    redisTemplate.opsForValue().set(key0, taskB, 10, TimeUnit.MINUTES);
                    if (taskB.getLatitude() != 0 && taskB.getLongitude() != 0) {
                        redisTemplate.opsForValue().set(taskB.getSbid() + "", taskB, 24, TimeUnit.HOURS);
                    }
                }
            }
            invoke.insertFFLogData(info);
        }
    }
}
