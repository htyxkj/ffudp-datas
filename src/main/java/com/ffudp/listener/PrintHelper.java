package com.ffudp.listener;

import com.ffudp.msg.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class PrintHelper implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(PrintHelper.class);
    private InputStream inputStream;
    private OutputStream outputStream;
    private String type;
    private PublishService publishService;
    private Socket socket;
    private InputStreamReader isr = null;
    private BufferedReader br = null;

    public PrintHelper(Socket socket, String type, PublishService publishService) throws IOException {
        this.socket = socket;
        this.inputStream=socket.getInputStream();
        this.type=type;
        this.publishService = publishService;
        this.outputStream = socket.getOutputStream();

    }

    public void run() {
        try {
            byte[] bytes = new byte[1024];
            int length;
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
                }
                sb.setLength(0);
            }


//            isr = new InputStreamReader(this.inputStream,"UTF-8");
//            br = new BufferedReader(isr);
//            String inf = null;
//            while ((inf = br.readLine()) != null){
//                if(inf.length()>0){
//                    logger.info(inf);
//                    publishService.publish("Parsing2",inf);
//                    outputStream.write(inf.getBytes());
//                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
                outputStream.flush();
                outputStream.close();
                if(!socket.isClosed()){
                    socket.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

}
