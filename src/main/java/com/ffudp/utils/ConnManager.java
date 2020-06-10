package com.ffudp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ConnManager {
    static Logger logger = LoggerFactory.getLogger(ConnManager.class);
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
                logger.error("释放数据库连接时发生异常!"+ e.getMessage());
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
                logger.error("释放数据库连接时发生异常!"+ e.getMessage());
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
                logger.error("释放数据库连接时发生异常!"+ e.getMessage());
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
