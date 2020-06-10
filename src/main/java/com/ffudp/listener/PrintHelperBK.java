package com.ffudp.listener;

import com.ffudp.msg.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PrintHelperBK implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(PrintHelperBK.class);
    private InputStream inputStream;
    private OutputStream outputStream;
    private String type;
    private PublishService publishService;
    private Socket socket;
    public PrintHelperBK(Socket socket, String type, PublishService publishService) throws  Exception{
        this.socket = socket;
        this.inputStream= socket.getInputStream();
        this.type=type;
        this.publishService = publishService;
        this.outputStream = socket.getOutputStream();
    }

    public void run() {
        byte[] bytes = new byte[1024];
        int length;
        try {
            while (true){
                StringBuilder sb = new StringBuilder();
                if (((length=inputStream.read(bytes))>1)){
                    String s = new String(bytes, 0, length,"UTF-8");
                    sb.append(s);
                }
                if(sb.length()>0){
                    logger.info(sb.toString());
                    publishService.publish("Parsing2",sb.toString());
                    outputStream.write(sb.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    sb.setLength(0);
                    if(!socket.isClosed()){
                        socket.close();
                    }
                    break;
                }
                sb.setLength(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
