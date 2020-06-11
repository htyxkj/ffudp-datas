package com.ffudp.listener;

import com.ffudp.dbo.MMSG;
import com.ffudp.msg.PublishService;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

@Slf4j
public class PrintHelper implements Runnable{
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
                    byte[] bs =Base64.getEncoder().encode(sb.toString().getBytes());
                    String str = new String(bs);
                    publishService.publish("Parsing2",str);
//                    outputStream.write(sb.toString().getBytes());
                }
                sb.setLength(0);
            }


//            isr = new InputStreamReader(this.inputStream,"UTF-8");
//            br = new BufferedReader(isr);
//            String inf = null;
//            while ((inf = br.readLine()) != null){
//                if(inf.length()>0){
//                    log.info(inf);
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
