package com.ffudp.serv;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.utils.Tools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class SaveTaskbTask {
    @Autowired
    @Lazy
    private DBInvoke invoke;
    @Autowired
    @Lazy
    private RedisTemplate redisTemplate;

    @Scheduled(fixedRate=4000)
    public void saveTaskb(){
        long d1 = System.currentTimeMillis();
        d1 = d1 - (8*1000);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(d1);
        String saveKey = ICL.SAVE_KEY+ICL.DIV_D+Tools.getSplitZu(cal);
        log.info("开始保存作业记录，saveKey："+saveKey);
        doSaveDB(saveKey);
    }
    public void doSaveDB(String currKey) {
        List<ObTaskB> listB = new ArrayList<ObTaskB>();
        try {
            Set<String> set = redisTemplate.opsForSet().members(currKey);
            if(set != null) {
                for (String str : set) {
                    ObTaskB tskB = getTaskB(str);
                    if(tskB != null) {
                        listB.add(tskB);
                    }
                }
                if(listB.size()>0) {
                    invoke.batchSaveObTaskB(listB);
                }
            }
            redisTemplate.delete(currKey);
        } catch (Exception e) {
            log.info("保存数据时出错");
            for (int i=0;i<listB.size();i++){
                log.error(listB.get(i).toString());
            }
            e.printStackTrace();
        }
    }
    private ObTaskB getTaskB(String key0) {
        try {
            if (redisTemplate.hasKey(key0)) {
                JSONObject info0 = (JSONObject) redisTemplate.opsForValue().get(key0);
                ObTaskB tskB = JSONObject.parseObject(info0.toJSONString(), ObTaskB.class);
                String rKey = tskB.getSbid()+"_TKID";
                Object tkid = redisTemplate.opsForValue().get(rKey);
                if(tkid == null) {
                    try {
                        tkid = invoke.getTkidBYsbid(tskB.getSbid()+"");
                    }catch (Exception e){
                        log.error("查询任务编码出错！",e);
                    }
                    if(tkid == null){
                        tkid = tskB.getSbid()+"_NOTASKID";
                    }else {
                        redisTemplate.opsForValue().set(rKey, tkid, 5, TimeUnit.MINUTES);
                    }
                }
                tskB.setTkid(tkid.toString());
                PkObTask pk = new PkObTask();
                pk.setTkid(tkid.toString());
                pk.setSpeedtime(tskB.getSpeedtime());
                boolean oo = invoke.exits( pk );
                if(oo){
//                    return null;
//                    if(tskB.getParsingNum() == 3){//判断当前数据GPS 传感器 是否全部解析完成  3 全部解析过
//                        return null;
//                    }else{//当条记录在数据库中存在 但是 10分钟内只解析过一次
                        ObTaskB tskB1 =  invoke.getObTaskB(pk);
                        log.info("当前记录数据库中存在：" + JSONObject.toJSONString(tskB1));
                        if((tskB1.getLongitude() ==0 && tskB1.getLatitude() ==0) || tskB1.getSumflow() ==0) {
                            if ((tskB.getParsingNum() & 1) > 0) {//解析过GPS数据
                                if (tskB.getLatitude() != 0 && tskB.getLongitude() != 0 && tskB1.getLatitude() == 0 && tskB1.getLongitude() == 0) {
                                    //更新GPS数据
                                    tskB1.setLatitude(tskB.getLatitude());
                                    tskB1.setLongitude(tskB.getLongitude());
                                    tskB1.setSpeed(tskB.getSpeed());
                                    tskB1.setHeight(tskB.getHeight());
                                    tskB1.setDirection(tskB.getDirection());
                                }
                            }
                            if ((tskB.getParsingNum() & 2) > 0) {//解析过传感器数据
                                if (tskB.getSumflow() != 0 && tskB1.getSumflow() == 0) {
                                    tskB1.setFlow(tskB.getFlow());
                                    tskB1.setSumflow(tskB.getSumflow());
                                    tskB1.setTemperature(tskB.getTemperature());
                                    tskB1.setPressure2(tskB.getPressure2());
                                    tskB1.setPressure(tskB.getPressure());
                                    tskB1.setHumidity(tskB.getHumidity());
                                    tskB1.setWindspeed(tskB.getWindspeed());
                                }
                            }
                            log.info("修正后数据：" + JSONObject.toJSONString(tskB1));
                            return tskB1;
                        }
                        return null;
//                    }
                }
                return tskB;
            } else {
                return null;
            }
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
