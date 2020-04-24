package com.clinbrain.util.connection;

import com.clinbrain.util.connection.dataSourceUtils.DBUtilOnDbcp;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * 基础客户端类
 */
public class BasicClient {

    protected static Connection conn;
    protected static Statement stat;

    protected void init(String driverName,String url,String username,String password){
        try{
            DataSource dataSource = DBUtilOnDbcp.getDataSouce(driverName, url, username, password); //dbcp
//            DataSource dataSource = DBUtilOnDruid.getDataSource(driverName, url, username, password); //druid
            conn = dataSource.getConnection();
            stat = conn.createStatement();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected List<String> getDBNames() {
        return null;
    }

    protected List<String> getTableNames(String database) {
        return null;
    }

    protected List<Object> getTableMetas(String database, String tableName) {
        return null;
    }

    protected ResultSet executeQuery(String sql) {
        return null;
    }

}
