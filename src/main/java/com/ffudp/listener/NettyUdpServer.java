package com.ffudp.listener;

import com.ffudp.serv.UDPServerChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;


@Component
@Slf4j
public class NettyUdpServer {

    private static final EventLoopGroup group = new NioEventLoopGroup(1);


    @Autowired
    UDPServerChannelInitializer udpServerChannelInitializer;

    @Value("${server.ffport}")
    private int port;

    //监听端口的通道，即server的处理通道
    private Channel channel;

    /**
     * 开启udp server服务
     *
     * @return
     */
    public ChannelFuture start() {
        //启动类
        Bootstrap serverBootstrap = new Bootstrap();
        serverBootstrap.group(group)//组配置，初始化ServerBootstrap的线程组
                .channel(NioDatagramChannel.class)//数据包通道，udp通道类型
                .option(ChannelOption.SO_BROADCAST, true)//支持广播
                .handler(udpServerChannelInitializer);//通道处理者
        //Future：异步任务的生命周期，可用来获取任务结果
        ChannelFuture channelFuture1 = serverBootstrap.bind(port).syncUninterruptibly();//绑定端口，开启监听,同步等待
        if (channelFuture1 != null && channelFuture1.isSuccess()) {
            log.info("[UDP] server start success, port = {}", port);
            channel = channelFuture1.channel();//获取通道
        } else {
            log.error("udp server start failed!!");
            channelFuture1.cause().printStackTrace();
        }
        return channelFuture1;
    }

    /**
     * 停止udp server服务
     * 销毁前的拦截
     */
    @PreDestroy
    public void destroy() {
        try {
            if (channel != null) {
                ChannelFuture await = channel.close().await();
                if (!await.isSuccess()) {
                    log.error("udp channel close fail, {}", await.cause());
                }
            }
            Future<?> future1 = group.shutdownGracefully().await();
            if (!future1.isSuccess()) {
                log.error("udp group shutdown fail, {}", future1.cause());
            }
            log.info("udp shutdown success");
        } catch (InterruptedException e) {
            log.info("udp shutdown fail");
            e.printStackTrace();
        }
    }

}
