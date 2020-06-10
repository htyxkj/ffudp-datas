package com.ffudp.listener;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
@Service
public class SaveDBListener implements MessageListener {
    private static Logger _log = LoggerFactory.getLogger(SaveDBListener.class);
    @Autowired
    @Lazy
    private RedisTemplate redisTemplate;
    @Autowired
    @Lazy
    private DBInvoke invoke;
    public ConcurrentHashMap<String,String> kMap = new ConcurrentHashMap<String,String>();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 缓存消息是序列化的，需要反序列化。然而new String()可以反序列化，但静态方法valueOf()不可以
        String key0 = new String(message.getBody());
        key0 = key0.substring(1,key0.length()-1);
        if(kMap.contains(key0))
            return;
        kMap.put(key0,key0);
        try {

            List<Object> listKey = null;
            listKey = (List<Object>)redisTemplate.opsForList().range(key0,0,-1);

            if(listKey != null) {
                _log.info("开始保存数据："+key0);
                List<ObTaskB> listB = new ArrayList<ObTaskB>();
                for (int i = 0; i < listKey.size(); i++) {
                    ObTaskB tskB = getTaskB(listKey.get(i).toString());
                    if(tskB != null)
                        listB.add(tskB);
                }
                _log.info("数据条数："+listB.size());
                if(listB.size()>0) {
                    invoke.batchSaveObTaskB(listB);
                    redisTemplate.delete(key0);
                }
            }
            kMap.remove(key0);
        } catch (Exception e) {
            e.printStackTrace();
            kMap.remove(key0);
        }
    }
    private ObTaskB getTaskB(String key0) {
        try {
            if (redisTemplate.hasKey(key0)) {
                JSONObject info0 = (JSONObject) redisTemplate.opsForValue().get(key0);
                ObTaskB tskB = JSONObject.parseObject(info0.toJSONString(), ObTaskB.class);
                key0 = "CUUINFO_"+key0;
                if(redisTemplate.hasKey(key0)) {
                    JSONObject info = (JSONObject) redisTemplate.opsForValue().get(key0);
                    ObTaskB infoTskB = JSONObject.parseObject(info.toJSONString(), ObTaskB.class);
                    tskB.setFlow(infoTskB.getFlow());
                    tskB.setTemperature(infoTskB.getTemperature());
                    tskB.setPressure(infoTskB.getPressure());
                    tskB.setSumflow(infoTskB.getSumflow());

                }
                String rKey = tskB.getSbid()+"_TKID";
                Object tkid = redisTemplate.opsForValue().get(rKey);
                if(tkid == null) {
                    try {
                        tkid = invoke.getTkidBYsbid(tskB.getSbid()+"");
                    }catch (Exception e){
                        _log.error("查询任务编码出错！",e);
                    }
                    if(tkid == null){
                        tkid = tskB.getSbid()+"_NOTASKID";
                    }else {
                        redisTemplate.opsForValue().set(rKey, tkid, 5, TimeUnit.MINUTES);
                    }
                }
                tskB.setTkid(tkid.toString());
                return tskB;
            }else  if(redisTemplate.hasKey( "CUUINFO_"+key0)) {
                key0 = "CUUINFO_"+key0;
                JSONObject info = (JSONObject) redisTemplate.opsForValue().get(key0);
                ObTaskB infoTskB = JSONObject.parseObject(info.toJSONString(), ObTaskB.class);
                String rKey = infoTskB.getSbid()+"_TKID";
                Object tkid = redisTemplate.opsForValue().get(rKey);
                if(tkid == null) {
                    try {
                        tkid = invoke.getTkidBYsbid(infoTskB.getSbid()+"");
                    }catch (Exception e){
                        _log.error("查询任务编码出错！",e);
                    }
                    if(tkid == null){
                        tkid = infoTskB.getSbid()+"_NOTASKID";
                    }else {
                        redisTemplate.opsForValue().set(rKey, tkid, 5, TimeUnit.MINUTES);
                    }
                }
                infoTskB.setTkid(tkid.toString());
                return infoTskB;
            } else {
                return null;
            }
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
