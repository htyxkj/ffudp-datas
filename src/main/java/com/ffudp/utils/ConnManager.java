package com.ffudp.utils;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
public class ConnManager {

    /**
     * 释放数据库连接
     * @param connection
     * @throws Exception
     */
    public static void freeConnection(Connection connection) throws Exception {
        if (connection != null){
            try {
                connection.close();
            }catch(Exception e){
                log.error("释放数据库连接时发生异常!"+ e.getMessage());
            }
        }
    }
    /**
     * @param stat
     * @throws Exception
     */
    public static void freeCallableStatement(PreparedStatement stat) throws Exception {
        if (stat != null){
            try {
                stat.close();
            }catch(Exception e){
                log.error("释放数据库连接时发生异常!"+ e.getMessage());
            }
        }
    }

    /**
     * @param stat
     * @throws Exception
     */
    public static void freeResultSet(ResultSet stat) throws Exception {
        if (stat != null){
            try {
                stat.close();
            }catch(Exception e){
                log.error("释放数据库连接时发生异常!"+ e.getMessage());
            }
        }
    }

    // 关闭连接
    public static void close(Connection conn, PreparedStatement stat, ResultSet rs) throws Exception {
        freeResultSet(rs);
        freeCallableStatement(stat);
        freeConnection(conn);
    }
}
