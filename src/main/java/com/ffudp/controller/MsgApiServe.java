package com.ffudp.controller;

import com.ffudp.dbo.RtnEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class MsgApiServe {

    @RequestMapping(value = "/ffthird",method = {RequestMethod.POST, RequestMethod.GET},produces = "application/json;charset=utf-8")
    public RtnEntity order(@RequestParam("info")String info) throws Exception {
        RtnEntity rtn = new RtnEntity();
        log.info("MsgApiServe"+info);
        rtn.setId(1);
        rtn.setMsg("接收成功！");
        return rtn;
    }
}
