package com.ffudp.listener;

import com.ffudp.cl.ICL;
import com.ffudp.utils.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-10
 */
public class Client2 {
//        static String  host = "192.168.3.200";
//        static String  host = "192.168.3.5";
//        static String  host = "211.144.37.205";
        static String  host = "127.0.0.1";
//        static String  host = "http://htyxkj08.vaiwan.com/";
    static int port = 55685;
    private static InputStreamReader isr = null;
    private static BufferedReader br = null;
    private  static Socket socket;
    public static void main(String[] args) {
        try {
            socket = new Socket(host, port);
            run(socket);

        }catch (Exception e){

        }finally {


        }

    }
    public static void run(Socket socket) throws Exception {
        try {
            // 建立连接后获得输出流
            String message = "msg";
            OutputStream out = socket.getOutputStream();
            String w1 = "04031400000000BD4C41F0000000000264008700000000B0AD" ;
            long li = System.currentTimeMillis();
            System.out.println(li);
            li = 1618073891000L;
            ByteBuffer buffer = ByteBuffer.allocate(1023);
            for (int i=0;i<1;i++){
                byte[] b0 = new byte[1024];
                StringBuffer sb = new StringBuffer();
                sb.append("C").append(ICL.DIV_1F).append(li);
                sb.append(ICL.DIV_1F).append(863284048316554L).append(ICL.DIV_1F);
                byte[] bs = Tools.hexStr2Byte(w1);
                System.out.println(Tools.bytesToHexString(bs));
                buffer.put(sb.toString().getBytes());
                buffer.put(bs);
                buffer.putChar(ICL.DIV_1E);

                System.out.println(Tools.bytesToHexString(bs));

                sb.append(new String(w1)).append(ICL.DIV_1E);
//                sb.append("C").append(ICL.DIV_1F).append(li);
//                sb.append(ICL.DIV_1F).append(863284048316554L).append(ICL.DIV_1F).append(new String(bs)).append(ICL.DIV_1E);
//                sb.append("C").append(ICL.DIV_1F).append(li);
//                sb.append(ICL.DIV_1F).append(863284048316554L).append(ICL.DIV_1F).append(new String(bs)).append(ICL.DIV_1E);
//                sb.append("C").append(ICL.DIV_1F).append(li);
//                sb.append(ICL.DIV_1F).append(863284048316554L).append(ICL.DIV_1F).append(new String(bs)).append(ICL.DIV_1E);
                out.write(buffer.array());
    //            new PrintHelper2(socket,"客户端").start();
                out.flush();
                Thread.sleep(2000);

            }
            socket.close();
        }catch (Exception e){
            socket.close();
            e.printStackTrace();
        }
    }

}
