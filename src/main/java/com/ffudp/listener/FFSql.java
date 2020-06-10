package com.ffudp.listener;

import com.ffudp.utils.ConnManager;
import com.ffudp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *  飞防硬件数据监听接口，在服务启动的时候，该监听自动启动
 *  实时数据按照每5秒作为一个数据单元存储，10分钟后自动过期清理
 *  同一台设备的$GPRMC数据可以一次接收到，其他的数据可能会分多次接收，
 *  但是数据格式还没有见到，！！！！！需要测试的时候注意一下！！！
 * @author fengzejiang
 *
 */
//@Component
//@Order(value=8)
public class FFSql implements CommandLineRunner {
    private static Logger _log = LoggerFactory.getLogger(FFSql.class);

    @Autowired
    private DataSource dataSource;

    //通过这种方式也能获取注册服务的信息
    @Override
    public void run(String... args) throws Exception {
        _log.info("FFSql start2");
        onDataChange2();
    }

    public void onDataChange2() throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("select bs from fflogs where tmid in(?,?)");
            stmt.setLong(1,1586179160953l);
            stmt.setLong(2,1586189290615l);
            rs = stmt.executeQuery();
            int k = 1;
            while (rs.next()){
                byte[] bs = rs.getBytes(1);
                byte sbaddr = bs[0];//设备地址
                byte readcmd = bs[1];//读命令
                byte rtnb = bs[2];//返回字节数
                if(sbaddr==1&&readcmd==4){
                    byte[] ssll = new byte[4];//瞬时流量
                    System.arraycopy(bs,3,ssll,0,ssll.length);
                    ByteBuffer bsf = ByteBuffer.wrap(ssll);
                    _log.info("瞬时流量："+Tools.bytes2Float(ssll));
                    byte[] sslldw = new byte[2];//瞬时流量单位
                    System.arraycopy(bs,7,sslldw,0,2);
                    _log.info("瞬时流量单位："+Tools.bytes2Integer(sslldw));
                    byte[] total = new byte[8];//总量
                    System.arraycopy(bs,9,total,0,total.length);
                    _log.info("总量："+Tools.bytes2Double(total));
                    byte[] zlsw = new byte[2];//总单位
                    System.arraycopy(bs,17,zlsw,0,2);
                    _log.info("总量单位："+Tools.bytes2Integer(zlsw));
                    byte[] temp = new byte[4];//温度
                    System.arraycopy(bs,19,temp,0,temp.length);
                    _log.info("温度："+Tools.bytes2Float(temp));

                    temp = new byte[4];//压力
                    System.arraycopy(bs,23,temp,0,temp.length);
                    _log.info("压力："+Tools.bytes2Float(temp));
                    temp = new byte[4];//总量
                    System.arraycopy(bs,27,temp,0,temp.length);
                    _log.info("总量："+Tools.bytes2Float(temp));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            ConnManager.close(conn,stmt,rs);
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

    /**
     * 将byte数组转成十六进制形式的字符串
     *
     * @param src
     * @return
     */




}
