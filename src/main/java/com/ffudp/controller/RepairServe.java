package com.ffudp.controller;

import com.alibaba.fastjson.JSONObject;
import com.ffudp.cl.ICL;
import com.ffudp.dbo.RtnEntity;
import com.ffudp.serv.UDPDataRepairService;
import com.ffudp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class RepairServe {
    private static Logger _log = LoggerFactory.getLogger(RepairServe.class);



    @Autowired
    private UDPDataRepairService service;

    @RequestMapping(value = "/ffRepair",method = {RequestMethod.POST, RequestMethod.GET},produces = "application/json;charset=utf-8")
    public RtnEntity order(@RequestParam("tkid")String tkid) throws Exception {
        RtnEntity rtn = new RtnEntity();
        service.Repair(tkid);
        return rtn;
    }

    @RequestMapping(value = "/ffRenew",method = {RequestMethod.POST, RequestMethod.GET},produces = "application/json;charset=utf-8")
    public RtnEntity Renew(@RequestParam("stTime")String stTime,@RequestParam("edTime")String edTime,@RequestParam("sbid")String sbid,@RequestParam("sbid")String tkid) throws Exception {
        RtnEntity rtn = new RtnEntity();
        if(tkid == null){
            tkid = sbid+"_NOTASKID";
        }
        service.Renew(stTime,edTime,sbid,tkid);
        return rtn;
    }
}
