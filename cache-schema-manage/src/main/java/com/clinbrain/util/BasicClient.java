package com.clinbrain.util;

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

    public void init(String driverName,String url,String username,String password){
        try{
            DataSource dataSouce = DBUtils.getDataSouce(driverName, url, username, password);
            conn = dataSouce.getConnection();
            stat = conn.createStatement();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<String> getDBNames() {
        return null;
    }

    public List<String> getTableNames(String database) {
        return null;
    }

    public List<Object> getTableMetas(String database, String tableName) {
        return null;
    }

    public ResultSet executeQuery(String sql) {
        return null;
    }

}
