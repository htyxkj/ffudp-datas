package com.ffudp.serv;

import com.ffudp.msg.PublishService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-12
 */
@Component
@Slf4j
public class NettyServer {
    public void start(InetSocketAddress address, PublishService publishService){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
//            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup,workerGroup).channel(NioServerSocketChannel.class)
//                    .localAddress(address).childHandler(new ServerChannelInitializer(publishService)).option(ChannelOption.SO_BACKLOG, 128)
//                    .childOption(ChannelOption.SO_KEEPALIVE,true);
            ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup,workerGroup).channel(NioServerSocketChannel.class)
                    .localAddress(address).childHandler(new ServerChannelInitializer(publishService)).option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE,true);
//            bootstrap.option(ChannelOption.SO_SNDBUF, 16*1024)
//                    .option(ChannelOption.SO_RCVBUF, 16*1024);
            ChannelFuture future = bootstrap.bind(address).sync();
            log.info("Server start listen at " + address.getPort());
        }catch (Exception e){
            e.printStackTrace();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
