package com.ffudp.serv;

import com.ffudp.cl.ICL;
import com.ffudp.listener.ParsingNewListener;
import com.ffudp.msg.PublishService;
import com.ffudp.utils.Tools;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.*;

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
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try{
            ByteBuf buf = (ByteBuf) msg;
            //创建目标大小的数组
            byte[] barray = new byte[buf.readableBytes()];
            //把数据从bytebuf转移到byte[]
            buf.getBytes(0,barray);
            ReferenceCountUtil.release(buf);
            //将byte[]转成字符串用于打印
            List<byte[]> l1 = new ArrayList<byte[]>();
            int i_temp = 0;
            for(int i=0;i<barray.length;i++){
                if(barray[i] == ICL.DIV_1E){
                    int len = 0;
                    if(i_temp == 0) {
                        len = i;
                    }else{
                        len = i-i_temp;
                    }
                    byte[] b1 = new byte[len];
                    System.arraycopy(barray,i_temp,b1,0,b1.length);
                    l1.add(b1);
                    i_temp = i+1;
                }
            }
            if(l1.size() == 0){
                log.info("DIV_1E解析失败，数据条数为0；barray长度："+barray.length);
                log.info(new String(barray));
            }
            for(int i=0;i<l1.size();i++){
                String str1 = Tools.getString(l1.get(i));
                str1 = str1.replaceAll("\n","");
                str1 = str1.replaceAll("\r","");
                if(str1.startsWith("G") || str1.startsWith("\u001eG")){
                    str1 = str1 + ICL.DIV_1E;
                    byte[] bs = Base64.getEncoder().encode(str1.getBytes());
                    String str = new String(bs);
                    log.info("GPS数据：{}",str1);
                    publishService.publish("Parsing2",str);
                }else if(str1.startsWith("C") || str1.startsWith("\u001eC")){
                    StringBuffer con = getCStrData(l1.get(i));
                    log.info("传感器数据：{}",con.toString());
                    byte[] bs = Base64.getEncoder().encode(con.toString().getBytes());
                    String str = new String(bs);
                    publishService.publish("Parsing2",str);
                }else{
                    log.info("数据格式错误："+str1);
                }
            }
            ctx.flush();
        }catch (Exception e){
            ctx.flush();
            e.printStackTrace();
            log.info("解析数据错误！！");
        }
    }

    /**
     * 解析传感器数据
     * @param b2
     * @return
     */
    private StringBuffer getCStrData(byte[] b2 ) {
        byte[] b3 = null;
        byte[] b4 = null;
        int num =0;
        for(int j=0;j<b2.length;j++){
            if(b2[j] == ICL.DIV_1F){
                num ++;
            }
            if(num == 3){
                b3 = new byte[j+1];
                System.arraycopy(b2,0,b3,0,b3.length);
                b4 = new byte[b2.length-j-1];
                System.arraycopy(b2,j+1,b4,0,b4.length);
                j = b2.length+10;
            }
        }
        String _b4 = Tools.bytesToHexString(b4);
        StringBuffer con = new StringBuffer();
        con.append(new String(b3));
        con.append(_b4);
        con.append(ICL.DIV_1E);
        return con;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("channelActive----->");
        ctx.close();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("出错了：",cause);
        ctx.close();
    }
}
