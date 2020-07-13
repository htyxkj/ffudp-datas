package com.ffudp.serv;

import com.ffudp.dao.DBInvoke;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.utils.Tools;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class FileListenerService {
    @Autowired
    private DBInvoke invoke;
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    private static SimpleDateFormat sdfymdh = new SimpleDateFormat("yyyyMMddHH");

    @Value("${server.ffftpbk}")
    private String monitorBkDir;//ftp 解析后文件存放路径

    @Value("${server.ffftp}")
    private String fileDir;

    //3.添加定时任务
    @Scheduled(cron = "0 0/10 * * * ? ")
    //或直接指定时间间隔，例如：5秒
//    @Scheduled(fixedRate=5000)
    private void getFileList() {
        List<File> fileList = new ArrayList<File>();
        File file = new File(fileDir);
        File[] files = file.listFiles();// 获取目录下的所有文件或文件夹
        if (files == null) {// 如果目录为空，直接退出
            return;
        }
        for(int i=0;i<files.length;i++){
            this.parsingFile(files[i]);
        }
    }

    public void parsingFile(File file)  {
        String fileName = file.getName();
        log.info("解析文件:"+fileName);
        String sbid = fileName.split("_")[0];
        String s = null;
        String time1= "";
        Date speedTime = new Date();
        int readNum =0;
        String tkid = "";//任务编码
        ObTaskB taskB = null;
        try {
            if(file.length() <=1)
                return;
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件

            while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                try {
                    if(s != null && s.length()>14){
                        if(s.indexOf("$") <=14 && s.indexOf("$")!=-1)
                            break;
                        String time = s.substring(0,14);//时间
                        speedTime = sdf.parse(time);
                        String dmt = simpleDateFormat.format(speedTime);
                        speedTime = simpleDateFormat.parse(dmt);
                        time = dmt;
                        if(readNum ==0){//第一行 去获取任务编码
                            tkid = getTkid(sbid,time);
                            readNum++;
                        }
                        if(!time1.equals(time)){//时间发生变化
                            if(taskB != null){
                                invoke.saveObTaskB(taskB);
                            }
                            taskB = getTaskRow(tkid,speedTime);
                            taskB.setSbid(Long.parseLong(sbid));
                            time1 = time;
                        }
                        String dataStr = s.substring(14,s.length()).trim();
                        if(dataStr.indexOf("$") == 0){//GPS 消息
                            String[] vvs = dataStr.split(",");
                            String typeStr = vvs[0];
                            if (typeStr.equals("$GPRMC") || typeStr.equals("$GNRMC")) {
                                if ("A".equals(vvs[2]) && vvs.length>=9) {
                                    float lat = Float.parseFloat(vvs[3]);
                                    lat = (int) (lat / 100) + (lat % 100 / 60);
                                    taskB.setLatitude(lat);//纬度
                                    float lng = Float.parseFloat(vvs[5]);
                                    lng = (int) (lng / 100) + (lng % 100 / 60);
                                    taskB.setLongitude(lng);//经度
                                    taskB.setSpeed(Float.parseFloat(vvs[7]) * 1.852f);//速度
                                    taskB.setDirection(vvs[8]);//方向
                                }
                            }
                            if ((typeStr.equals("$GPGGA") || typeStr.equals("$GNGGA")) && vvs.length>=15) {
                                if("1".equals(vvs[6])){
                                    float lat = Float.parseFloat(vvs[2]);
                                    lat = (int) (lat / 100) + (lat % 100 / 60);
                                    taskB.setLatitude(lat);//纬度
                                    float lng = Float.parseFloat(vvs[4]);
                                    lng = (int) (lng / 100) + (lng % 100 / 60);
                                    taskB.setLongitude(lng);//经度
                                }
                                String hi = vvs[9] == null ? "0" : vvs[9];
                                if (hi.length() > 0) {
                                    taskB.setHeight(Float.parseFloat(vvs[9]));//高度
                                }
                            }
                        }else{
                            if(dataStr.indexOf("040312") !=-1){//字头
                                dataStr = dataStr.substring(dataStr.indexOf("040312"));
                                if(dataStr.length()>=46) {
                                    String fl = dataStr.substring(6, 14);//瞬时流量
                                    Integer flow = Integer.parseInt(fl, 16);
                                    taskB.setFlow(flow / 1000);
                                    String sfld = dataStr.substring(14, 22);//累积低位
                                    Integer sumFlowD = Integer.parseInt(sfld, 16);
                                    String sflg = dataStr.substring(22, 30);//累积高位
                                    Integer sumFlowG = Integer.parseInt(sflg, 16);
                                    taskB.setSumflow((sumFlowD + sumFlowG) / 1000);
                                    String sd = dataStr.substring(30, 34);//湿度
                                    Integer humidity = Integer.parseInt(sd, 16);
                                    taskB.setHumidity(humidity);
                                    String temp = dataStr.substring(34, 38);//温度
                                    Integer temper = Integer.parseInt(temp, 16);
                                    taskB.setTemperature(temper / 10);
                                    String per = dataStr.substring(38, 42);//压力
                                    Integer press = Integer.parseInt(per, 16);
                                    taskB.setPressure(press / 10);
                                }
                            }
//                            int idx0 = getCharacterPosition("0104",dataStr,1);
//                            int idx1 = getCharacterPosition("0104",dataStr,2);
//                            String str = dataStr.substring(idx0,idx1+1);
//                            if(str.length() !=66){//传感器数据一共66位
//                                str = dataStr.substring(idx0,idx0+1+66);
//                            }
//                            byte[] bs = hexString2Bytes(str);
//                            byte[] ssll = new byte[4];//瞬时流量
//                            System.arraycopy(bs,3,ssll,0,ssll.length);
//                            Float ll = Tools.bytes2Float(ssll);
//                            taskB.setFlow(ll);//瞬时流量
//                            byte[] temp = new byte[4];//温度
//                            System.arraycopy(bs,19,temp,0,temp.length);
//                            Float temper =Tools.bytes2Float(temp);
//                            taskB.setFlow(temper);//温度
//                            temp = new byte[4];//压力
//                            System.arraycopy(bs,23,temp,0,temp.length);
//                            Float press = Tools.bytes2Float(temp);
//                            taskB.setPressure(press);//压力
//                            temp = new byte[4];//总量
//                            System.arraycopy(bs,27,temp,0,temp.length);
//                            Float zl = Tools.bytes2Float(temp);
//                            taskB.setSumflow(zl);//总流量
                        }
                    }
                }catch (Exception e){
                    log.error("断点续传文件解析失败,文件名称："+fileName+"，数据："+s);
//                    log.error("错误信息：",e);
                }
            }
            br.close();
            if(taskB != null){
                invoke.saveObTaskB(taskB);
            }
            String toRoot = monitorBkDir+"/"+sdfymdh.format(new Date())+"/";
            File file1 = new File(toRoot);
            if(!file1.exists()){     //判断文件路径是否存在
                file1.mkdirs();              //不存在创建新的文件
            }
            File toFile = new File(toRoot+fileName);
            file.renameTo(toFile);
        }catch (Exception e){
            log.error("断点续传文件解析失败,文件名称："+fileName);
//            log.error("错误信息：",e);
        }
    }

    /**
     * 更具任务编码时间获取记录
     * @param tkid
     * @param speedtime
     * @return
     */
    public ObTaskB getTaskRow(String tkid,Date speedtime){
        ObTaskB tskb = new ObTaskB();
        tskb.setTkid(tkid);
        tskb.setSpeedtime(speedtime);
        try {
            String t = simpleDateFormat.format(new Date());
            tskb.setDatetime( simpleDateFormat.parse(t));
        }catch (Exception e){

        }
        PkObTask pk = new PkObTask();
        pk.setTkid(tkid);
        pk.setSpeedtime(speedtime);
        boolean exits = invoke.exits(pk);
        if(exits)
            tskb = invoke.getObTaskB(pk);
        return tskb;
    }
    /**
     * 获取设备对应的任务
     * @param sbid
     * @param speedtime
     * @return
     */
    public String getTkid(String sbid,String speedtime){
        String tkid = sbid+"_NOTASKID";
        try {
            tkid = invoke.getTkidBySbid(sbid,speedtime);
            if(tkid == null )
                tkid = sbid+"_NOTASKID";
        }catch (Exception e){
            e.printStackTrace();
            log.error("断点文件查询任务编码出错");
            tkid = sbid+"_NOTASKID";
        }finally {
            return tkid;
        }
    }
    //16进制文件转byte[]
    public static byte[] hexString2Bytes(String src) {
        int l = src.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = (byte) Integer.valueOf(src.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }
    //获取某个字符第几次出现的位置
    public static int getCharacterPosition(String str,String val,int index){
        //这里是获取"/"符号的位置
        Matcher slashMatcher = Pattern.compile(str).matcher(val);
        int mIdx = 0;
        while(slashMatcher.find()) {
            mIdx++;
            //当"/"符号第k次出现的位置
            if(mIdx == index){
                break;
            }
        }
        return slashMatcher.start();
    }
}
