package com.ffudp.serv;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class UDPServerChannelInitializer extends ChannelInitializer<NioDatagramChannel> {

    @Autowired
    ServerChannelInboundHandler serverChannelHandler;

    @Override
    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
        ChannelPipeline pipeline = nioDatagramChannel.pipeline();
//        pipeline.addLast(new StringDecoder());
        //自定义的InboundHandler输入处理者
        pipeline.addLast("serverChannelHandler", serverChannelHandler);
//        pipeline.addLast(new StringEncoder());
    }
}
