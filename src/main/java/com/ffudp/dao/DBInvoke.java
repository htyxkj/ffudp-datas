package com.ffudp.dao;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.dao.resp.OBTaskBResp;
import com.ffudp.dbo.ObTaskB;
import com.ffudp.dbo.PkObTask;
import com.ffudp.dbo.UdpDataInfo;
import com.ffudp.utils.ConnManager;
import com.ffudp.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class DBInvoke {
    @Autowired
    @Qualifier("baseSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("pgSource")
    private DataSource pgSource;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OBTaskBResp rep;

    public ObTaskB getObTaskB(PkObTask pk){
        return rep.getOne(pk);
    }

    public void saveObTaskB(ObTaskB item){
        rep.save(item);
    }
    public void batchSaveObTaskB(List<ObTaskB> list){
        rep.saveAll(list);
    }

    public boolean exits(PkObTask pk){
        return rep.existsById(pk);
    }

    /**
     * 根据任务编码查询没有GPS数据的记录
     * @param tkid
     * @return
     */
    public List<ObTaskB> getNoGPS(String tkid){

        List<ObTaskB> listInfo= new ArrayList<ObTaskB>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = pgSource.getConnection();
            stmt = conn.prepareStatement("select speedtime,tkid,longitude,latitude,speed,height,direction,flow,sumfolw,pressure,temperature,effective,sbid,datetime from ob_taska where tkid =? and  to_number(latitude,'999999999')  =0  and  to_number(longitude,'999999999')  =0 and speed =0 and height =0 ORDER BY speedtime");
//                        stmt = conn.prepareStatement("select speedtime,tkid,longitude,latitude,speed,height,direction,flow,sumfolw,pressure,temperature,effective,sbid,datetime from ob_taska where tkid =?  ORDER BY speedtime");
            stmt.setString(1,tkid);
            rs = stmt.executeQuery();
            while (rs.next()){
                ObTaskB info = new ObTaskB();
                info.setSpeedtime(rs.getTimestamp(1));
                info.setTkid(rs.getString(2));
                info.setLongitude(rs.getFloat(3));
                info.setLatitude(rs.getFloat(4));
                info.setSpeed(rs.getFloat(5));
                info.setHeight(rs.getFloat(6));
                info.setDirection(rs.getString(7));
                info.setFlow(rs.getFloat(8));
                info.setSumflow(rs.getFloat(9));
                info.setPressure(rs.getFloat(10));
                info.setTemperature(rs.getFloat(11));
                info.setEffective(rs.getBoolean(12));
                info.setSbid(rs.getLong(13));
                info.setDatetime(rs.getTimestamp(14));
                listInfo.add(info);
            }
            log.info("GPS错误数据条数："+listInfo.size());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                ConnManager.close(conn,stmt,rs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listInfo;
        }
//        ObTaskB tskb = new ObTaskB();
//        tskb.setTkid(tkid);
//        tskb.setLongitude(0);
//        tskb.setLatitude(0);
//        //创建匹配器，即如何使用查询条件
//        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
//                .withMatcher("tkid",ExampleMatcher.GenericPropertyMatchers.exact())
//                .withMatcher("longitude",ExampleMatcher.GenericPropertyMatchers.exact())
//                .withMatcher("latitude",ExampleMatcher.GenericPropertyMatchers.exact())
//                .withIgnorePaths("speedtime").withIgnorePaths("sbid").withIgnorePaths("speed").withIgnorePaths("height").withIgnorePaths("flow")
//                .withIgnorePaths("sumfolw").withIgnorePaths("temperature").withIgnorePaths("ressure")
//                .withIgnorePaths("effective").withIgnorePaths("direction").withIgnorePaths("datetime");//isFace字段不参与匹配
//
//        Example<ObTaskB> example = Example.of(tskb,exampleMatcher);
//        return rep.findAll(example);
    }
    /**
     * 根据任务编码查询没有传感器数据的记录
     * @param tkid
     * @return
     */
    public List<ObTaskB> getNoData(String tkid){
        ObTaskB tskb = new ObTaskB();
        tskb.setTkid(tkid);
        tskb.setFlow(0);
        tskb.setSumflow(0);
        tskb.setTemperature(0);
        tskb.setPressure(0);
        //创建匹配器，即如何使用查询条件
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("tkid",ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("flow",ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("sumfolw",ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("temperature",ExampleMatcher.GenericPropertyMatchers.exact())
                .withMatcher("ressure",ExampleMatcher.GenericPropertyMatchers.exact())
                .withIgnorePaths("speedtime").withIgnorePaths("sbid").withIgnorePaths("longitude")
                .withIgnorePaths("latitude").withIgnorePaths("speed").withIgnorePaths("height")
                .withIgnorePaths("effective").withIgnorePaths("direction").withIgnorePaths("datetime");//isFace字段不参与匹配

        Example<ObTaskB> example = Example.of(tskb,exampleMatcher);
        return rep.findAll(example);
    }

    public ObTaskB getTaskbBytimeID(String tkid, Date time){
        PkObTask id = new PkObTask();
        id.setTkid(tkid);
        id.setSpeedtime(time);
        if(rep.existsById(id)) {
            return rep.getOne(id);
        }else{
            return null;
        }
    }

    /**
     * 根据时间，设备编码 查询源数据
     * @param time
     * @param sbid
     * @return
     */
    public List<UdpDataInfo> getUdpDataInfo(long time,int sbid,int type){
        List<UdpDataInfo> listInfo= new ArrayList<UdpDataInfo>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = pgSource.getConnection();
            stmt = conn.prepareStatement("select content,encode(bs,'hex')  bs from fflogs where tmid = ? and id =? and type = ?");
            stmt.setLong(1,time);
            stmt.setInt(2,sbid);
            stmt.setObject(3,type);
            rs = stmt.executeQuery();
            while (rs.next()){
                String content = rs.getString(1);
                String hexData = rs.getString(2);
                UdpDataInfo info = new UdpDataInfo();
                info.hexData = hexData;
                info.strInfo = content;
                listInfo.add(info);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                ConnManager.close(conn,stmt,rs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listInfo;
        }
    }

    /**
     * 根据时间，设备编码 查询源数据
     * @param stTime
     * @param stTime
     * @param edTime
     * @return
     */
    public List<UdpDataInfo> getUdpDataInfo(long stTime,long edTime,int sbid){
        List<UdpDataInfo> listInfo= new ArrayList<UdpDataInfo>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = pgSource.getConnection();
            stmt = conn.prepareStatement("select content,encode(bs,'hex'),tmid,type  bs from fflogs where tmid >= ? and tmid <=? and id =? order by tmid");
            stmt.setLong(1,stTime);
            stmt.setLong(2,edTime);
            stmt.setInt(3,sbid);
            rs = stmt.executeQuery();
            while (rs.next()){
                String content = rs.getString(1);
                String hexData = rs.getString(2);
                long tmid = rs.getLong(3);
                int type = rs.getInt(4);
                UdpDataInfo info = new UdpDataInfo();
                info.hexData = hexData;
                info.strInfo = content;
                info.tmid = tmid;
                info.type = type;
                listInfo.add(info);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                ConnManager.close(conn,stmt,rs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listInfo;
        }
    }
//    public OBItemInfo getOBItemInfoBytkId(long id,int sbid){
//        Connection conn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//        try {
//            conn = dataSource.getConnection();
//            String logsql="select speedtime,longitude,latitude,speed,height,drection,flow,sumflow,temperature,pressure from ob_taskb where id=? and sbid=?";
//            stmt = conn.prepareStatement(logsql);
//            stmt.setLong(1,id);
//            stmt.setInt(2,sbid);
//            rs = stmt.executeQuery();
//        }catch (Exception e){
//
//        }
//        return null;
//    }
    /**
     * 更具日志对象写日志库
     * @param info
     * @throws Exception
     */
    public void insertFFLogData(UdpDataInfo info)  {
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
//            log.info("写日志："+ JSONObject.toJSONString(info).toString());
            conn = pgSource.getConnection();
            String logsql="insert into fflogs (tmid,id,type,typestr,content,gettime,bs) values(?,?,?,?,?,?,?)";
            stmt = conn.prepareStatement(logsql);
            stmt.setLong(1,info.tmid);
            stmt.setLong(2,info.sbid);
            stmt.setInt(3,info.type);
            stmt.setString(4,info.typeStr);
            stmt.setString(5,info.strInfo);
            stmt.setString(6,info.dmt);
            stmt.setBytes(7,info.bs);
            stmt.execute();
        }catch (Exception e){
            e.printStackTrace();
            log.error("写库失败：：",e);
            log.error("写日志："+ JSONObject.toJSONString(info).toString());
        }finally {
            try {
                ConnManager.close(conn,stmt,null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void getPgInfo1() throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
//            log.info("写日志："+ JSONObject.toJSONString(info).toString());
            conn = pgSource.getConnection();
            String logsql="select user,password from user";
            stmt = conn.prepareStatement(logsql);
            rs = stmt.executeQuery();
            while (rs.next()){
                log.info(rs.getString(1));
                log.info(rs.getString(2));

            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("写库失败：：",e);
        }finally {
            ConnManager.close(conn,stmt,null);
        }
    }

    /**
     * 更具设备号查询任务编码
     * @param sbid
     * @return
     * @throws Exception
     */
    public String getTkidBYsbid(String sbid) throws Exception{
        String tkid = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            String logsql =  "select sid from ob_task where  bgtime<= GETDATE() and edtime>=GETDATE() and " +
                    "  asid = (select asid from v_air_equi where tlid = '"+sbid+"')";
            stmt = conn.prepareStatement(logsql);
            rs = stmt.executeQuery();
            if(rs.next()){
                tkid = rs.getString(1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            ConnManager.close(conn,stmt,rs);
            return tkid;
        }
    }

    /**
     * 根据设备号，时间查询任务编码
     * @param sbid
     * @param time
     * @return
     */
    public String getTkidBySbid(String sbid ,String time) {
        String tkid = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
            conn = dataSource.getConnection();
            String logsql =  "select sid from ob_task where  bgtime<= '"+time+"' and edtime>= '"+time+"' and " +
                    "  asid = (select top 1 asid from equi where  mkdate<= '"+time+"' and (edtime>= '"+time+"' or edtime is null)   and tlid = '"+sbid+"' order by mkdate desc )";
//            String logsql =  "select sid from ob_task where  bgtime<= '"+time+"' and edtime>= '"+time+"' and " +
//                    "  asid = (select   asid from equi where  mkdate<= '"+time+"' and (edtime>= '"+time+"' or edtime is null)   and tlid = '"+sbid+"' order by mkdate desc limit 0,1)";
            stmt = conn.prepareStatement(logsql);
            rs = stmt.executeQuery();
            if(rs.next()){
                tkid = rs.getString(1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                ConnManager.close(conn,stmt,rs);
            }catch (Exception e){
            }
            return tkid;
        }
    }
}
