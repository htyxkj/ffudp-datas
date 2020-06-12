package com.ffudp.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-04
 */
public class ClientPortT {
    public static void main(String[] args) throws Exception {
        String msg = "#IMEI:867935030001654 Time:%s LNG:113.853485E LAT:22.585745N ALT:37.100000 SPEED:0.012347";
        ExecutorService threadPool = Executors.newFixedThreadPool(20);
        for(int i=0;i<2000;i++){
            Thread.sleep(20);
            LocalDateTime nowLocalDate = LocalDateTime.now();
            String time = nowLocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String msg1 = String.format(msg,time);
            Runnable run = new MulRun("",msg1);
            threadPool.submit(run);
        }
        threadPool.shutdown();
    }


}

class MulRun implements Runnable{
    private String msg;
    private String name;
    String host = "127.0.0.1";
    int port = 55685;
    public MulRun(){}
    public MulRun(String name,String msg){
        this.name = name;
        this.msg = msg;
    }
    @Override
    public void run() {
        try{
            // 与服务端建立连接
            Socket socket = new Socket(host, port);
            // 建立连接后获得输出流
            String message = msg;
            socket.getOutputStream().write(message.getBytes("UTF-8"));
            //通过shutdownOutput高速服务器已经发送完数据，后续只能接受数据
            socket.shutdownOutput();
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("连接失败！！！");
        }

    }
}
