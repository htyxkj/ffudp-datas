package com.ffudp.listener;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */

public class SaveDBTask implements Runnable{
    private static Logger _log = LoggerFactory.getLogger(SaveDBTask.class);
    private RedisTemplate redisTemplate;
    private DBInvoke invoke;

    private String currKey ;

//    public SaveDBTask(){
//        this.redisTemplate = ApplicationContextProvider.getBean(RedisTemplate.class);
//        this.invoke = ApplicationContextProvider.getBean(DBInvoke.class);
//    }

    public SaveDBTask(String currKey,DBInvoke invoke,RedisTemplate redisTemplate){
        this.currKey = currKey;
        this.invoke =  invoke;
        this.redisTemplate =redisTemplate;
    }
//    public void setCurrKey(String currKey){
//        this.currKey = currKey;
//    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            doSaveDB(this.currKey);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void doSaveDB(String currKey) {
        try {
            List<Object> listKey = null;
            listKey = (List<Object>)redisTemplate.opsForList().range(currKey,0,-1);
            _log.info("原始数据量："+listKey.size());
            if(listKey != null) {
                _log.info("开始保存数据："+currKey);
                List<ObTaskB> listB = new ArrayList<ObTaskB>();
                for (int i = 0; i < listKey.size(); i++) {
                    ObTaskB tskB = getTaskB(listKey.get(i).toString());
                    if(tskB != null)
                        listB.add(tskB);
                }
                _log.info("数据条数："+listB.size());
                if(listB.size()>0) {
                    invoke.batchSaveObTaskB(listB);
                }
            }
        } catch (Exception e) {
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
//                        _log.error("查询任务编码出错！",e);
                    }
                    if(tkid == null){
                        tkid = tskB.getSbid()+"_NOTASKID";
                    }else {
                        redisTemplate.opsForValue().set(rKey, tkid, 5, TimeUnit.MINUTES);
                    }
                }
                tskB.setTkid(tkid.toString());
                return tskB;
            } else {
                return null;
            }
        }catch (Exception e) {
//            e.printStackTrace();
            return null;
        }
    }
}
