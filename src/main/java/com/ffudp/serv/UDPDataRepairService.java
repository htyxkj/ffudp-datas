package com.ffudp.serv;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.OBItemInfo;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.dbo.UdpDataInfo;
import com.ffudp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 监听数据服务
 */
@Service
public class UDPDataRepairService {
    private static Logger _log = LoggerFactory.getLogger(UDPDataRepairService.class);
//    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private DBInvoke invoke;

    /**
     * 重新读写数 修复错误数据
     * @param tkid 任务编码
     * @throws Exception
     */
    public Integer Repair(String tkid) throws Exception{
        int num = 0;
        _log.info("解析任务源数据："+tkid);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //修复错误GPS
        _log.info("解析GPS数据");
        List<ObTaskB> noGPSList = invoke.getNoGPS(tkid);
        _log.info("GPS错误数据条数："+noGPSList.size());
        if(noGPSList != null){
            for (int i=0;i<noGPSList.size();i++) {
                ObTaskB otb = noGPSList.get(i);
                long time = otb.getSpeedtime().getTime();//时间
                time = time/1000;
                time = time*1000;
                Long sbid = otb.getSbid();
                List<UdpDataInfo> listInfo = invoke.getUdpDataInfo(time,sbid,5);
                for(int z=0;z<listInfo.size();z++) {
                    String s1 = listInfo.get(z).strInfo;
                    int firstD = s1.indexOf(",");
                    if(firstD>0){
                        ObTaskB tb  = getGPSData(s1, firstD );
                        if(tb.getLatitude() !=0 && tb.getLongitude() !=0)
                            break;
                        noGPSList.get(i).setLatitude(tb.getLatitude());
                        noGPSList.get(i).setLongitude(tb.getLongitude());
                        noGPSList.get(i).setHeight(tb.getHeight());
                        noGPSList.get(i).setSpeedtime(tb.getSpeedtime());
                    }
                }
            }
            invoke.batchSaveObTaskB(noGPSList);
        }

        _log.info("解析传感器数据");
        List<ObTaskB> noDataList = invoke.getNoData(tkid);
        _log.info("传感器错误数据条数："+noDataList.size());
        if(noDataList != null){
            for (int i=0;i<noDataList.size();i++) {
                ObTaskB otb = noDataList.get(i);
                long time = otb.getSpeedtime().getTime();//时间
                time = time/1000;
                time = time*1000;
                Long sbid = otb.getSbid();
                List<UdpDataInfo> listInfo = invoke.getUdpDataInfo(time,sbid,8);
                for(int z=0;z<listInfo.size();z++) {
                    String hexData = listInfo.get(z).hexData;
                    byte[] bs = hexStr2Byte(hexData);
                    ObTaskB tb  = getFlowData(bs);
                    noDataList.get(i).setFlow(tb.getFlow());
                    noDataList.get(i).setSumflow(tb.getSumflow());
                    noDataList.get(i).setTemperature(tb.getTemperature());
                    noDataList.get(i).setPressure(tb.getPressure());
                }
            }
            invoke.batchSaveObTaskB(noDataList);
        }
        return num;
    }

    public ObTaskB getGPSData(String strInfo,int firstD){
        ObTaskB tskB = new ObTaskB();
        String typeStr = strInfo.substring(0, firstD);
        String s1 = strInfo;
        s1 = s1.substring(firstD + 1);
        try {
            if (typeStr.equals("$GPRMC") || typeStr.equals("$GNRMC")) {
                String[] vvs = s1.split(",");
                if ("A".equals(vvs[1])) {
                    float lat = Float.parseFloat(vvs[2]);
                    lat = (int) (lat / 100) + (lat % 100 / 60);
                    tskB.setLatitude(lat);
                    float lng = Float.parseFloat(vvs[4]);
                    lng = (int) (lng / 100) + (lng % 100 / 60);
                    tskB.setLongitude(lng);
                    tskB.setSpeed(Float.parseFloat(vvs[6]) * 1.852f);//速度
                    tskB.setDirection(vvs[7]);//方向
                }
            }
            if (typeStr.equals("$GPGGA") || typeStr.equals("$GNGGA")) {
                String[] vvs = s1.split(",");
                if(vvs.length >=14) {
                    if ("1".equals(vvs[5])) {
                        float lat = Float.parseFloat(vvs[1]);
                        lat = (int) (lat / 100) + (lat % 100 / 60);
                        tskB.setLatitude(lat);//纬度
                        float lng = Float.parseFloat(vvs[3]);
                        lng = (int) (lng / 100) + (lng % 100 / 60);
                        tskB.setLongitude(lng);//经度
                    }
                    //获取高度
                    String hi = vvs[8] == null ? "0" : vvs[8];
                    if (hi.length() > 0) {
                        tskB.setHeight(Float.parseFloat(vvs[8]));
                    }
                }
            }
        }catch (Exception e){
            _log.error("解析GPS数据错误",e,s1);
            _log.error(s1);
        }
        return tskB;
    }

    public ObTaskB getFlowData(byte[] bs ){
        ObTaskB tkb = new ObTaskB();
        try {
            if(bs.length>= 30) {
                byte sbaddr = bs[0];//设备地址
                byte readcmd = bs[1];//读命令
                if (sbaddr == 1 && readcmd == 4) {
                    byte[] ssll = new byte[4];//瞬时流量
                    System.arraycopy(bs, 3, ssll, 0, ssll.length);
                    ByteBuffer bsf = ByteBuffer.wrap(ssll);
                    tkb.setFlow(Tools.bytes2Float(ssll));//瞬时流量
                    byte[] total = new byte[8];//总量
                    System.arraycopy(bs, 9, total, 0, total.length);
                    tkb.setSumflow(Tools.bytes2Float(total));//总量
                    byte[] temp = new byte[4];//温度
                    System.arraycopy(bs, 19, temp, 0, temp.length);
                    tkb.setTemperature(Tools.bytes2Float(temp));//温度
                    temp = new byte[4];//压力
                    System.arraycopy(bs, 23, temp, 0, temp.length);
                    tkb.setPressure(Tools.bytes2Float(temp));//压力
                    temp = new byte[4];//总量
                    System.arraycopy(bs, 27, temp, 0, temp.length);
                    tkb.setSumflow(Tools.bytes2Float(temp));//总量
                }
            }
        }catch (Exception e){
            _log.error("解析传感器数据错误",e);
            _log.error(bytesToHexString(bs));
        }finally {
            return tkb;
        }
    }
    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }
    public static byte[] hexStr2Byte(String hex) {
        ByteBuffer bf = ByteBuffer.allocate(hex.length() / 2);
        for (int i = 0; i < hex.length(); i++) {
            String hexStr = hex.charAt(i) + "";
            i++;
            hexStr += hex.charAt(i);
            byte b = (byte) Integer.parseInt(hexStr, 16);
            bf.put(b);
        }
        return bf.array();
    }


    public void Renew(String stTime,String edTime,String sbid,String tkid){
        try{
            Map<String,ObTaskB> mptb = new HashMap<String,ObTaskB>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date stDate = sdf.parse(stTime);
            Date edDate = sdf.parse(edTime);
            long stT = stDate.getTime();
            long edT = edDate.getTime();
            List<ObTaskB> obTaskBList = new ArrayList<ObTaskB>();
            List<UdpDataInfo> listInfo = invoke.getUdpDataInfo(stT,edT,Integer.parseInt(sbid));
            _log.info("数据条数："+listInfo.size());
            String kk = "";
            for(int z=0;z<listInfo.size();z++) {
                UdpDataInfo info = listInfo.get(z);
                String key = info.tmid+"_KEY";
                ObTaskB tb = mptb.get(key);

                if(tb == null){
                    tb = new ObTaskB();
                }
                tb.setSbid(Long.parseLong(sbid));
                tb.setTkid(tkid);
                tb.setSpeedtime(sdf.parse(sdf.format(new Date(info.tmid))));
                if(info.type == 5) {
                    String s1 = info.strInfo;
                    int firstD = s1.indexOf(",");
                    if (firstD > 0) {
                        ObTaskB tbgps = getGPSData(s1, firstD);
                        tb.setLatitude(tbgps.getLatitude());
                        tb.setLongitude(tbgps.getLongitude());
                        tb.setHeight(tbgps.getHeight());
                    }
                }
                if(info.type == 8 ) {
                    String hexData = info.hexData;
                    byte[] bs = hexStr2Byte(hexData);
                    ObTaskB tbdata = getFlowData(bs);
                    tb.setFlow(tbdata.getFlow());
                    tb.setSumflow(tbdata.getSumflow());
                    tb.setTemperature(tbdata.getTemperature());
                    tb.setPressure(tbdata.getPressure());
                }
                if(key.equals(kk)){
                    mptb.put(key,tb);
                }else{
                    _log.info("key != kk");
                    _log.info("数据时间:"+tb.getSpeedtime());
                    if(tb.getSpeedtime() != null) {
                        obTaskBList.add(tb);
                    }
                    kk = key;
                }
                if(z == listInfo.size()-1){
                    if(tb.getSpeedtime() != null) {
                        obTaskBList.add(tb);
                    }
                }
            }
            _log.info("需要保存数据："+obTaskBList.size());
            invoke.batchSaveObTaskB(obTaskBList);
        }catch (Exception e){
            e.printStackTrace();
            _log.error("重新解析数据错误",e);
        }
    }
}
