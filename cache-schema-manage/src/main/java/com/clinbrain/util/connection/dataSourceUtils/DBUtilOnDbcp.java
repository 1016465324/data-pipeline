package com.clinbrain.util.connection.dataSourceUtils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBUtilOnDbcp {

    private static final Map<String, DataSource> dataSourcePool = new ConcurrentHashMap<>();

    private DBUtilOnDbcp() {}

    private static DataSource init(String driverName, String url, String username, String password) throws ClassNotFoundException {
        BasicDataSource dataSource = new BasicDataSource();

        Class.forName(driverName);
        dataSource.setDriverClassName(driverName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setInitialSize(10);
        dataSource.setMaxActive(8);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);

        dataSource.setTestOnBorrow(false); //检查连接有效性

        String sql;
        if (StringUtils.contains(driverName, "Oracle"))
            sql = "select 1 from dual";
        else
            sql = "select 1";
        dataSource.setValidationQuery(sql);
        dataSource.setValidationQueryTimeout(1000);

        dataSource.setRemoveAbandoned(true); //超过时间限制回收连接
        dataSource.setRemoveAbandonedTimeout(86400);

        return dataSource;
    }


    public static DataSource getDataSouce(String driverName, String url, String username, String password) throws ClassNotFoundException {
        String dbType = getDbType(url, username, password);
        DataSource dataSource = dataSourcePool.get(dbType);
        if(dataSource == null){
            synchronized (DBUtilOnDbcp.class){
                dataSource = dataSourcePool.get(dbType);
                if (dataSource == null){
                    dataSource = init(driverName, url, username, password);
                    dataSourcePool.put(dbType, dataSource);
                }
            }
        }
        return dataSource;
    }


    public static void clearDataSourceInPool(String dbType){
        if(dataSourcePool.get(dbType) != null)
            dataSourcePool.remove(dbType);
    }


    public static String getDbType(String url, String username, String password) {
        String dbType = String.format("%s|%s", url, username);
        if(StringUtils.isNotEmpty(password))
            dbType += "|" + password;
        return dbType;
    }

    public static void closeQuietly(final ResultSet rs){
        closeQuietly((AutoCloseable) rs);
    }
    public static void closeQuietly(final Statement state){
        closeQuietly((AutoCloseable) state);
    }
    public static void closeQuietly(final Connection conn){
        closeQuietly((AutoCloseable) conn);
    }
    public static void closeQuietly(final AutoCloseable auto){
        try{
            if(auto != null)
                auto.close();
        }catch (final Exception e){
            e.printStackTrace();
        }
    }

}
