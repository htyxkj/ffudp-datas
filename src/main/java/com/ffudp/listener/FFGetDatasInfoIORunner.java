package com.ffudp.listener;

import com.ffudp.msg.PublishService;
import com.ffudp.serv.NettyServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
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
@Slf4j
public class FFGetDatasInfoIORunner implements CommandLineRunner {
    private ScheduledExecutorService es = Executors.newScheduledThreadPool(1);


    @Value("${server.ffport2}")
    private int port;//服务启动监听接口

    @Value("${server.ffip}")
    private String hostName;//服务启动监听接口

    @Value("${server.threadNum}")
    private int num;//线程数量

    private ExecutorService threadPool = null;
    @Autowired
    private PublishService publishService;
    @Autowired
    private NettyServer nettyServer;
    @Override
    public void run(String... args) throws Exception {
        log.info("Netty启动端口:"+port);
        InetSocketAddress address = new InetSocketAddress(hostName,port);
        nettyServer.start(address,publishService);
    }
}
