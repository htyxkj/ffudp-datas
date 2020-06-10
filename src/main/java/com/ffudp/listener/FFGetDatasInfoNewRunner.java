package com.ffudp.listener;

import com.ffudp.msg.PublishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 *  飞防硬件数据监听接口，在服务启动的时候，该监听自动启动
 *  实时数据按照每5秒作为一个数据单元存储，10分钟后自动过期清理
 *  同一台设备的$GPRMC数据可以一次接收到，其他的数据可能会分多次接收，
 *  但是数据格式还没有见到，！！！！！需要测试的时候注意一下！！！
 * @author fengzejiang
 *
 */
@Component
@Order(value=3)
public class FFGetDatasInfoNewRunner implements CommandLineRunner {
    private static Logger _log = LoggerFactory.getLogger(FFGetDatasInfoNewRunner.class);
    private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);


    @Value("${server.ffport2}")
    private int port;//服务启动监听接口

    @Value("${server.threadNum}")
    private int num;//线程数量

    private ExecutorService threadPool = null;

    @Autowired
    private PublishService publishService;

    @Override
    public void run(String... args) throws Exception {
        _log.info("FFGetDatasInfoNewRunner start");
        threadPool = Executors.newFixedThreadPool(num);
        new Thread(() -> {
            try {
                onDataComing();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void onDataComing() throws Exception {
        _log.info("启动端口:"+port);
        try{
            ServerSocket server = new ServerSocket(port);
            //如果使用多线程，那就需要线程池，防止并发过高时创建过多线程耗尽资源
            while(true){
                Socket socket = server.accept();
                Runnable runnable = new PrintHelper(socket,"0",publishService);
                threadPool.submit(runnable);
            }
//            Socket socket = server.accept();
//            Runnable runnable = new PrintHelper(socket.getInputStream(),"0",publishService,socket.getOutputStream());
//            es.scheduleWithFixedDelay(runnable, 0L, 2L, TimeUnit.MINUTES);
        }catch (Exception e){
            _log.error("出错了：",e);
        }

    }


    /**
     * 将字节流转换成文件
     * @param filename
     * @param data
     * @throws Exception
     */
    public static void saveFile(String filename,byte [] data)throws Exception{
        if(data != null){
            File dir = new File("D:\\logs\\datas\\" );
            if(!dir.exists()){
                dir.mkdirs();
            }
            String filepath ="D:\\logs\\datas\\" + filename;
            File file  = new File(filepath);
            if(file.exists()){
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data,0,data.length);
            fos.flush();
            fos.close();
        }
    }


}
