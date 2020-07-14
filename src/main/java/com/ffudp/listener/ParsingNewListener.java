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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    ExecutorService executorService = Executors.newFixedThreadPool(2);

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
                inf = inf.replaceAll("\n","");
                inf = inf.replaceAll("\r","");
                if (inf.length() > 0) {
                    parsing(inf);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void parsing(String inf) {
        try {
            UdpDataInfo info = new UdpDataInfo();
            info.datetime = new Date();
            String _info = inf;
            int index = _info.indexOf(ICL.DIV_1E);
            while (index !=-1){
                String str = _info.substring(0,index);
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
                if(_idx == -1){
                    break;
                }
                String sbid = s0.substring(0,_idx);//设备编码
                s0 = s0.substring(_idx+1);

                String key0 =  sbid+ ICL.DIV_D+d1;//设备编码_数据时间
                info.sbid = Long.parseLong(sbid);
                String saveKey = ICL.SAVE_KEY+ICL.DIV_D+Tools.getSplitZu();
                redisTemplate.opsForSet().add(saveKey, key0);
                synchronized (key0.intern()) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(d1);
                    String currKey = ICL.CURR_KEY + sbid + ICL.DIV_D + Tools.getSplitZu(cal);//分段KEY

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
                        info.strInfo = str;
                    } else if (str.startsWith("C")) {//传感器 数据
                        tskB = makeInfoData(s0,tskB);
                        info.typeStr = "DATA-INFO";
                        info.type = 8;
                        info.bs = str.getBytes();
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

                    if(_info.length()>index){
                        _info = _info.substring(index+1);
                    }
                    index = _info.indexOf(ICL.DIV_1E);
                    if(_info.length()<50){
                        index =-1;
                    }
                }
                invoke.insertFFLogData(info);
            }
        }catch (Exception e){
            log.error("数据解析异常：",e);
            log.error(inf);
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
        byte[] bs = s0.getBytes();
        String str = bytesToHexString(bs);
        int index = str.indexOf("040312");
        while(index>-1){
            int idx2 = str.indexOf("040312",index ==0?1:index);//获取第二个开头;
            if(idx2>0){
                //十多条记录
                String s1 = str.substring(index,idx2);
                str = str.substring(idx2);//0104;
                //chaifen s1
                if (s1.length()>=46){
                    //数据完整
                    taskB = getFlowData(taskB, str);
                }
            }else{
                if (str.length()>=46){
                    //数据完整
                    taskB = getFlowData(taskB, str);
                }
                str = "";
            }
            index = str.indexOf("0104");
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
            Float lng = Float.parseFloat(k0);
            lng = Tools.keepDecimal(lng,6);
            task.setLongitude(lng);
        }
        if("LAT".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            k0 = k0.substring(0,k0.length()-1);
            Float lat = Float.parseFloat(k0);
            lat = Tools.keepDecimal(lat,6);
            task.setLatitude(lat);
        }
        if("ALT".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            Float alt = Float.parseFloat(k0);
            alt = Tools.keepDecimal(alt,2);
            task.setHeight(alt);
        }
        if("SPEED".equals(kv[0])&&!StringUtils.isEmpty(kv[1])){
            String k0 = kv[1];
            Float speed = Float.parseFloat(k0);
            speed = Tools.keepDecimal(speed,2);
            task.setSpeed(speed);
        }
        return task;
    }

    //生成传感器 TaskB
    public static ObTaskB getFlowData(ObTaskB tskB, String inf){
        byte[] bs = hexStr2Byte(inf);
        try {
            if(inf.length()>=46) {
                try {
                    if(inf.indexOf("040312")==0){//字头
                        log.info("开始解析传感器数据："+inf);
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
//            if(bs.length>= 30) {
//                byte sbaddr = bs[0];//设备地址
//                byte readcmd = bs[1];//读命令
//                if (sbaddr == 1 && readcmd == 4) {
//                    byte[] ssll = new byte[4];//瞬时流量
//                    System.arraycopy(bs, 3, ssll, 0, ssll.length);
//                    float flow = Tools.bytes2Float(ssll);
//                    flow = Tools.keepDecimal(flow,6);
//                    tskB.setFlow(flow);//瞬时流量
//                    byte[] total = new byte[8];//总量
//                    System.arraycopy(bs, 9, total, 0, total.length);
//                    float sumFlow = Tools.bytes2Float(total);
//                    sumFlow = Tools.keepDecimal(sumFlow,6);
//                    tskB.setSumflow(sumFlow);//总量
//                    byte[] temp = new byte[4];//温度
//                    System.arraycopy(bs, 19, temp, 0, temp.length);
//                    float temper = Tools.bytes2Float(temp);
//                    temper = Tools.keepDecimal(temper,2);
//                    tskB.setTemperature(temper);//温度
//                    temp = new byte[4];//压力
//                    System.arraycopy(bs, 23, temp, 0, temp.length);
//                    float press = Tools.bytes2Float(temp);
//                    press = Tools.keepDecimal(press,3);
//                    tskB.setPressure(press);//压力
//                    temp = new byte[4];//总量
//                    System.arraycopy(bs, 27, temp, 0, temp.length);
//                    sumFlow = Tools.bytes2Float(temp);
//                    sumFlow = Tools.keepDecimal(sumFlow,6);
//                    tskB.setSumflow(sumFlow);//总量
//                }
//            }
        }catch (Exception e){
            log.error("解析传感器数据错误",e);
        }finally {
            return tskB;
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
        String str = "G1592795035000862808036298720#IMEI:862808036298720 Time:2020-06-22 11:03:55 LNG:116.444886E LAT:39.922741N ALT:64.800000 SPEED:0.024076C1592795035000862808036298720?v@ ??V??@?     :6?4?Dx??";
        ParsingNewListener pn = new ParsingNewListener();
        pn.parsing(str);
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