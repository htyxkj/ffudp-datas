package com.ffudp.dbo;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @Auther: fengzejiang1987@163.com
 * @Date : 2020/1/10 9:15
 */
public class RtnEntity implements Serializable {
    public RtnEntity(){}
    private int id;
    private String msg;

    private ArrayList<JSONObject> data;


    public ArrayList<JSONObject> getData() {
        return data;
    }

    public void setData(ArrayList<JSONObject> data) {
        this.data = data;
    }

    public void addRow(JSONObject o1) {
        if(data==null){
            data = new ArrayList<>();
        }
        if(o1!=null)
            data.add(o1);
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
