package com.ffudp.listener;

import com.ffudp.cl.ICL;
import com.ffudp.utils.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-10
 */
public class Client2 {
//        static String  host = "192.168.3.200";
//        static String  host = "192.168.3.5";
        static String  host = "211.144.37.205";
//        static String  host = "http://htyxkj08.vaiwan.com/";
    static int port = 8089;
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
            String w1 = "040314000000000000140200000000019d015200000000e5d9" ;
            for (int i=0;i<1;i++){
                long li = System.currentTimeMillis();
                StringBuffer sb = new StringBuffer();
                sb.append("C").append(ICL.DIV_1F).append(li);
                byte[] bs = Tools.hexStr2Byte(w1);
                sb.append(ICL.DIV_1F).append(862808036298720L).append(ICL.DIV_1F).append(new String(bs)).append(ICL.DIV_1E);
                out.write(sb.toString().getBytes("UTF-8"));

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
