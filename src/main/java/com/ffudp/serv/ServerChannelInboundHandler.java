package com.ffudp.serv;

import com.ffudp.msg.PublishService;
import com.ffudp.utils.Tools;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
@ChannelHandler.Sharable
@Slf4j
public class ServerChannelInboundHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    @Autowired
    private PublishService publishService;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        ByteBuf buf = datagramPacket.content();
        int len = buf.readableBytes();
        byte[] data = new byte[len];
        buf.readBytes(data);
        String inf = Tools.bytesToHexString(data);
        log.info(inf);
        publishService.publish("Parsing",inf);
//        System.out.println("[UDP] server 收到的消息：" + datagramPacket.content().toString(CharsetUtil.UTF_8));
//        String response = "{" + datagramPacket.content().toString(CharsetUtil.UTF_8) + "}的响应，我是服务端啊！！！";
//        DatagramPacket datagramPacket1 = new DatagramPacket(Unpooled.copiedBuffer(response, CharsetUtil.UTF_8), datagramPacket.sender());
//        channelHandlerContext.channel().writeAndFlush(datagramPacket1);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

}
