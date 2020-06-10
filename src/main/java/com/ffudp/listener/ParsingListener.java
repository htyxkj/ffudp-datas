package com.ffudp.listener;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.serv.UDPDataServiceNew;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
@Service
public class ParsingListener implements MessageListener {
    @Autowired
    @Lazy
    private UDPDataServiceNew udpService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 缓存消息是序列化的，需要反序列化。然而new String()可以反序列化，但静态方法valueOf()不可以
        String inf = new String(message.getBody());
        byte[] bb = message.getBody();
        inf = inf.substring(1,inf.length()-1);
        byte[] bc = hexStr2Byte(inf);
        try {
            udpService.service(bc,0,bc.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 16进制转byte[]
     * @param hex
     * @return
     */
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
}
