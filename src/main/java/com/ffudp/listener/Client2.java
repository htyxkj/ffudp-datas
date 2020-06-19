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
//        static String  host = "211.144.37.205";
        static String  host = "192.168.124.12";
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
            String w1 = "01049C0000800000838000AABE4B9E4027800198E441E20000800058F5413A82CF" ;

            for (int i=0;i<10000;i++){
                long li = System.currentTimeMillis();
                StringBuffer sb = new StringBuffer();
                sb.append("C").append(ICL.DIV_1F).append(li);
                byte[] bs = Tools.hexStr2Byte(w1);
                sb.append(ICL.DIV_1F).append(862808036298720L).append(ICL.DIV_1F).append(bs.toString()).append(ICL.DIV_1E);
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
