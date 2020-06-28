package com.clinbrain.util.connection;

import com.clinbrain.util.connection.dataSourceUtils.DBUtilOnDbcp;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.List;

/**
 * 基础客户端类
 */
public class BasicClient {

    protected DataSource dataSource;

    protected String driverName;
    protected String url;
    protected String username;
    protected String password;


    protected BasicClient(String driverName, String url, String username, String password) {
        this.driverName = driverName;
        this.url = url;
        this.username = username;
        this.password = password;

        init();
    }

    protected void init() {
        try {
            dataSource = DBUtilOnDbcp.getDataSouce(driverName, url, username, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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

    public ResultSet executeQuery(String sql) {
        return null;
    }

    public void close() {

    }

}
