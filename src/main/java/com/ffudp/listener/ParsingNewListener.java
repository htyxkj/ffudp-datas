package com.ffudp.listener;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.UdpDataInfo;
import com.ffudp.utils.DateUtils;
import com.ffudp.utils.Tools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
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

    private static String prefix = "040314";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] bs = message.getBody();
            String inf = new String(bs);
            if(!StringUtils.isEmpty(inf)) {
                inf = inf.substring(1, inf.length()-1);
                byte[] bb = Base64.getDecoder().decode(inf.getBytes());
                inf = new String(bb);
//                log.info("获取到数据:{}",inf);
                inf = inf.replaceAll("\u0004","04");
                String[] cc = inf.split("\u001e");
                for (String ss:cc) {
                    inf = ss;
                    if (inf.length() > 0) {
                        parsing(inf);
                    }
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
//            String _info = inf;
//            int index = _info.indexOf(ICL.DIV_1E);
//            while (index !=-1){
                String str = inf;//_info.substring(0,index);
                String s0 = str;
                int _idx = s0.indexOf(ICL.DIV_1F);
                int _idx2 = s0.indexOf(ICL.DIV_1F,_idx+1);
                if(_idx2 == -1){
                    return;
                }
                String strTime = s0.substring(_idx+1,_idx2);//数据时间戳
                long d1 = Long.parseLong(strTime);//数据时间
                d1 = d1/1000;
                d1 = d1*1000;
                s0 = s0.substring(_idx2+1);
                _idx = s0.indexOf(ICL.DIV_1F);
                if(_idx == -1){
                    return;
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
                    if (str.startsWith("G") || str.startsWith("\u001eG")) {//GPS 数据
                        tskB = makeGPSData(s0,tskB);
                        info.typeStr = "GPS";
                        info.type = 5;
                        info.strInfo = str;
                        tskB.setParsingNum(tskB.getParsingNum()|1);
                    } else if (str.startsWith("C") || str.startsWith("\u001eC")) {//传感器 数据
                        tskB = makeInfoData(s0,tskB);
                        info.typeStr = "DATA-INFO";
                        info.type = 8;
                        s0 = s0.replaceAll("\u001e","");
                        info.bs = hexStr2Byte(s0);
                        tskB.setParsingNum(tskB.getParsingNum()|2);
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
                    info.dmt = DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss",new Date(d1));
//                    if(_info.length()>index){
//                        _info = _info.substring(index+1);
//                    }
//                    index = _info.indexOf(ICL.DIV_1E);
//                    if(_info.length()<50){
//                        index =-1;
//                    }
                }
                invoke.insertFFLogData(info);
//            }
        }catch (Exception e){
            log.error("数据解析异常：",e);
            log.error(inf);
            e.printStackTrace();
        }
    }
    //解析GPS数据
    public ObTaskB makeGPSData(String s0,ObTaskB taskB){
        if(!StringUtils.isEmpty(s0)){
//            log.info("开始解析GPS数据："+s0);
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
        String str = s0;
        int index = str.indexOf(prefix);
        while(index>-1){
            int idx2 = str.indexOf(prefix,index ==0?1:index);//获取第二个开头;
            if(idx2>0){
                //十多条记录
                String s1 = str.substring(index,idx2);
                str = str.substring(idx2);//0104;
                //chaifen s1
                if (s1.length()>=48){
                    //数据完整
                    taskB = getFlowData(taskB, str);
                }
            }else{
                if (str.length()>=48){
                    //数据完整
                    taskB = getFlowData(taskB, str);
                }
                str = "";
            }
            index = str.indexOf(prefix);
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
        try {
            if(inf.length()>=48) {
                try {
                    if(inf.indexOf(prefix)==0){//字头
//                        log.info("开始解析传感器数据："+inf);
                        String fl = inf.substring(6,14);//瞬时流量
//                        Integer flow = Tools.hexStringToInt(fl, 16);
//                        tskB.setFlow((float) (flow/1000.0));
//                        String sfld = inf.substring(14,22);//累积低位
//                        Integer sumFlowD = Tools.hexStringToInt(sfld, 16);
//                        String sflg = inf.substring(22,30);//累积高位
//                        Integer sumFlowG = Tools.hexStringToInt(sflg, 16);
//                        tskB.setSumflow((float) ((sumFlowD+sumFlowG)/1000.0));
                        ByteBuffer bsf = ByteBuffer.wrap(Tools.hightLowTrans(Tools.hexStr2Byte(fl)));
                        tskB.setFlow(bsf.getFloat());
                        String sfld = inf.substring(14,22);//累积流量
                        bsf = ByteBuffer.wrap(Tools.hightLowTrans(Tools.hexStr2Byte(sfld)));
                        tskB.setSumflow(bsf.getFloat());

                        String sd = inf.substring(30,34);//湿度
                        Integer humidity = Tools.hexStringToInt(sd, 16);
                        tskB.setHumidity((float) (humidity/10.0));
                        String temp = inf.substring(34,38);//温度
                        Integer temper =  Tools.hexStringToInt(temp, 16);
                        tskB.setTemperature((float) (temper/10.0));
                        String per = inf.substring(38,42);//压力
                        Integer press =  Tools.hexStringToInt(per, 16);
                        tskB.setPressure((float) (press/10.0));
                        String per2 = inf.substring(42,46);//压力2
                        Integer press2 =  Tools.hexStringToInt(per2, 16);
                        tskB.setPressure2((float) (press2/10.0));
                    }
                } catch (Exception e) {
                    log.error("error:", e);
                    e.printStackTrace();
                }
            }
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
        String str = "04031400000000bd4c413f000000000264003f00000000b0ad";
//        String str = "04031400000000BD4C41F0000000000264008700000000B0AD";
//        String str = "04031420202020E7B583413F202020200264203F20202020E7A28D";
        ObTaskB taskB = new ObTaskB();
//        getFlowData(taskB,str);

        ByteBuffer bsf = ByteBuffer.wrap(Tools.hightLowTrans(Tools.hexStr2Byte(str)));
        String sfld = str.substring(14,22);//累积流量
        bsf = ByteBuffer.wrap(Tools.hightLowTrans(Tools.hexStr2Byte(sfld)));
        System.out.println(bsf.getFloat());
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