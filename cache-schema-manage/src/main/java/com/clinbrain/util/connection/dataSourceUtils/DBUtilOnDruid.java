package com.clinbrain.util.connection.dataSourceUtils;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.clinbrain.util.UtilHelper;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DBUtilOnDruid {

    private static ConcurrentHashMap<String, DataSource> dataSourcePool = new ConcurrentHashMap<>();

    private DBUtilOnDruid() {
    }

    private static DataSource init(String driverName, String url, String username, String password){
        try {
            Properties prop = UtilHelper.loadProperties("druid.properties");
            DataSource dataSource = DruidDataSourceFactory.createDataSource(prop);
            return dataSource;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static DataSource getDataSource(String driverName, String url, String username, String password){
        String dbType = getDbType(url, username, password);
        DataSource dataSource = dataSourcePool.get(dbType);
        if(dataSource == null){
            synchronized (DataSource.class){
                dataSource = dataSourcePool.get(dbType);
                if(dataSource == null){
                    dataSource = init(driverName, url, username, password);
                    dataSourcePool.put(dbType, dataSource);
                }
            }
        }
        return dataSource;
    }

    public static void cleanDataSource(String dbType){
        if(dataSourcePool.containsKey(dbType))
            dataSourcePool.remove(dbType);
    }

    public static void closeQuietly(ResultSet rs){
        closeQuietly(rs);
    }

    public static void closeQuietly(Statement stat){
        closeQuietly(stat);
    }

    public static void closeQuietly(Connection conn){
        closeQuietly(conn);
    }

    public static void closeQuietly(AutoCloseable auto){
        if(auto != null) {
            try {
                auto.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDbType(String url, String username, String password) {
        String key = String.format("%s|%s", url, username);
        if(StringUtils.isNotEmpty(password))
            key += "|" + password;
        return key;
    }





}
