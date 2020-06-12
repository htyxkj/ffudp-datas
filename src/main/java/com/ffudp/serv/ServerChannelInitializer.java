package com.ffudp.serv;

import com.ffudp.msg.PublishService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * @Author: 729002330@qq.com
 * @date :2020-06-12
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private PublishService publishService;

    public ServerChannelInitializer(PublishService publishService){
        this.publishService = publishService;
    }
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline().addLast("decoder",new StringDecoder(CharsetUtil.UTF_8));
        channel.pipeline().addLast("encoder",new StringEncoder(CharsetUtil.UTF_8));
        channel.pipeline().addLast(new ServerHandler(publishService));
    }
}
