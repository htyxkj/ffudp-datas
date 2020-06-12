package com.ffudp.serv;

import com.ffudp.msg.PublishService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-12
 */
@Slf4j
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private PublishService publishService;
    public  ServerHandler(){}
    public ServerHandler(PublishService publishService){
        this.publishService = publishService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive----->");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("server channelRead......");
        log.info(ctx.channel().remoteAddress()+"----->Server :"+ msg.toString());
        byte[] bs = Base64.getEncoder().encode(msg.toString().getBytes());
        String str = new String(bs);
        publishService.publish("Parsing2",str);
        //将客户端的信息直接返回写入ctx
//        ctx.write("server say :"+msg);
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("出错了：",cause);
        ctx.close();
    }
}
