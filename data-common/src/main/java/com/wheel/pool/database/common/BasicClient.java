package com.wheel.pool.database.common;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 基础客户端类
 */
public abstract class BasicClient {

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

    private void init() {
        try {
            BasicDataSource dataSource = new BasicDataSource();
    
            Class.forName(driverName);
            dataSource.setDriverClassName(driverName);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
    
            dataSource.setInitialSize(8);
            dataSource.setMaxActive(8);
            dataSource.setMaxIdle(5);
            dataSource.setMinIdle(1);
    
            dataSource.setTestOnBorrow(false); //检查连接有效性
    
            String sql;
            if (StringUtils.contains(driverName, "Oracle")) {
                sql = "select 1 from dual";
            } else {
                sql = "select 1";
            }
            
            dataSource.setValidationQuery(sql);
            dataSource.setValidationQueryTimeout(1000);
    
            dataSource.setRemoveAbandoned(true); //超过时间限制回收连接
            dataSource.setRemoveAbandonedTimeout(86400);
    
            this.dataSource = dataSource;
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

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
