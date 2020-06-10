package com.ffudp.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ffudp.FFUdpApp;
import com.ffudp.cl.ICL;
import com.ffudp.dbo.OBItemInfo;
import com.ffudp.dbo.RtnEntity;
import com.ffudp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  这是一个通用的springboot启动，该类用于处理消费实时数据按照设备id进行查询
 *  后期是否需要扩展传如时间查询，待定！！
 *  DruidConfig,配置了数据库访问监听
 */
@RestController
public class TestServe {
    private static Logger _log = LoggerFactory.getLogger(TestServe.class);

    @Autowired
    private RedisTemplate redisTemplate;



    @RequestMapping(value = "/ffdata",method = {RequestMethod.POST, RequestMethod.GET},produces = "application/json;charset=utf-8")
    public RtnEntity order(@RequestParam("id")String id) throws Exception {
        _log.info("获取设备数据Id："+id);
        RtnEntity rtn = new RtnEntity();
        Calendar calrd = Calendar.getInstance();
        String currKey = ICL.CURR_KEY+id+ICL.DIV_D+ Tools.getSplitZu(calrd);
        String currKey1 = ICL.CURR_KEY+id+ICL.DIV_D+"GT";
        if(redisTemplate.hasKey(currKey1)){
            _log.info("访问过key:"+currKey1);
            JSONObject info = (JSONObject) redisTemplate.opsForValue().get(currKey1);
            rtn = JSONObject.parseObject(info.toJSONString(),RtnEntity.class);
            return  rtn;
        }else{
            long key = redisTemplate.opsForList().size(currKey);
            if(key>0){
                List<String> infokeys = (List<String>)redisTemplate.opsForList().range(currKey,0,-1);
                for(int i=0;i<infokeys.size();i++){
//                    String infokey = (String)redisTemplate.opsForList().leftPop(currKey);//移除数据
                    String infokey = infokeys.get(i);
                    JSONObject info = (JSONObject) redisTemplate.opsForValue().get(infokey);
                    rtn.addRow(info);
                }
                rtn.setId(0);
                rtn.setMsg("OK");
                redisTemplate.opsForValue().set(currKey1,rtn,5, TimeUnit.SECONDS);
            }else{
                rtn.setId(-1);
                rtn.setMsg("没有数据");
            }
            return rtn;
        }

    }
}
