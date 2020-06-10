package com.ffudp.listener;

import com.alibaba.druid.util.StringUtils;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.serv.UDPDataServiceNew;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
@Service
public class ParsingNewListener implements MessageListener {
    private Logger logger = LoggerFactory.getLogger(ParsingNewListener.class);
    @Autowired
    @Lazy
    private UDPDataServiceNew udpService;
    ExecutorService threadPool = Executors.newFixedThreadPool(2);
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 缓存消息是序列化的，需要反序列化。然而new String()可以反序列化，但静态方法valueOf()不可以
//        String inf = new String(message.getBody());
//        if(!StringUtils.isEmpty(inf)){
//            inf = inf.substring(1, inf.length() - 1);
//            if(inf.length()>0){
//                Runnable runnable = new ParseRunnable(udpService,inf);
//                threadPool.submit(runnable);
//            }
//
//        }

        String inf = new String(message.getBody());
        if(!StringUtils.isEmpty(inf)) {
            inf = inf.substring(1, inf.length() - 1);
            logger.info("获取到数据:"+inf);
            synchronized (inf.intern()) {
                if (inf.length() > 0) {
                    Runnable runnable = new ParseRunnable(udpService, inf);
                    threadPool.submit(runnable);
                }
            }
        }


    }
}

class ParseRunnable implements Runnable{
    private static Logger _log = LoggerFactory.getLogger(ParseRunnable.class);
    private UDPDataServiceNew udpService;

    private String inf;
    public ParseRunnable(UDPDataServiceNew udpService,String inf){
        this.udpService = udpService;
        this.inf = inf;
    }
    @Override
    public void run() {
        String[] infs = inf.split("#");
        for(int i=0;i<infs.length;i++){
            String s0 = infs[i];
            if(!StringUtils.isEmpty(s0)){
                s0 = s0.toUpperCase();
                int _idx = s0.indexOf(" ");
                ObTaskB task = new ObTaskB();
                while (_idx>0){
                    String s1 = "";
                    if(s0.startsWith("TIME")){
                        _idx = s0.indexOf(" ",_idx+1);
                        s1 = s0.substring(0,_idx);
                        int index = s1.indexOf(":");
                        String[] kv = new String[2];
                        kv[0] = s1.substring(0,index);
                        kv[1] = s1.substring(index+1);
                        makeTaskB(task, kv);
                    }else{
                        s1 = s0.substring(0,_idx);
                        String[] kv = s1.split(":");
                        makeTaskB(task, kv);
                    }

                    s0 = s0.substring(_idx+1);
                    _idx = s0.indexOf(" ");
                    if(_log.isDebugEnabled())
                        _log.debug(s1);
                }
                if(s0.length()>0){
                    makeTaskB(task, s0.split(":"));
                }
                udpService.saveObTaskBNew(task,inf);
            }
        }
//        _log.info(inf);
    }

    private void makeTaskB(ObTaskB task, String[] kv) {
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
    }
}
