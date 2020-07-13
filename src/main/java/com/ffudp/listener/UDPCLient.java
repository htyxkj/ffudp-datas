package com.ffudp.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: 729002330@qq.com
 * @date :2020-05-29
 */
public class UDPCLient {

    public static void main(String[] args) throws Exception {
        int threadNum = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadNum);
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            MutilThread thread = new MutilThread();
            thread.setCountDownLatch(countDownLatch);
            thread.setSbid(i+1);
            executorService.submit(thread);
        }
        countDownLatch.await();
        executorService.shutdown();
        System.err.println("OVer");
    }
}
class MutilThread extends Thread{

    private CountDownLatch countDownLatch;
    private int sbid = 6001;

    public void setSbid(int id){
        sbid = id;
    }
    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }
    @Override
    public void run() {
        try {
            String  host = "127.0.0.1";
//    static String  host = "192.168.3.200";
//            String  host = "211.144.37.205";
            InetAddress inet = InetAddress.getByName(host);
            DatagramSocket ds = new DatagramSocket();
            String d0="04031200004650000D881A00000000021501370001BD9E";
                     //01049C0000800000838000AABE4B9E4027800198E441E20000800058F5413A82CF
            for(int i=0;i<99999;i++){
                long l1 = System.currentTimeMillis();
                byte[] date =hexStr2Byte(d0);
                int len = 16+date.length;
                ByteBuffer buffer = ByteBuffer.allocate(len);
                buffer.putInt(8);
                buffer.putInt(sbid);
                buffer.putLong(l1);
                buffer.put(date);
                //创建InetAdress对象，封装自己的IP地址
                DatagramPacket dp = new DatagramPacket(buffer.array(), len, inet,8340);
                //调用ds对象的方法send，发送数据包
                ds.send(dp);
                Thread.sleep(1000);
                System.out.println("发送数据"+i);
            }
            ds.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }

    }

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
