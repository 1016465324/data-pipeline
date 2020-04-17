package com.clinbrain.utils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JDBCUtils {
    private static final Map<String, DataSource> dataSourcePool = new ConcurrentHashMap<>();

    public static DataSource getDataSource(String driverName, String url, String username, String password) throws ClassNotFoundException {
        String key = getKey(url, username, password);
        DataSource dataSource = dataSourcePool.get(key);
        if (null == dataSource) {
            synchronized (JDBCUtils.class) {
                dataSource = dataSourcePool.get(key);
                if (null == dataSource) {
                    dataSource = init(driverName, url, username, password);
                    dataSourcePool.put(key, dataSource);
                }
            }
        }
        return dataSource;
    }

    private static DataSource init(String driverName, String url, String username, String password) throws ClassNotFoundException {
        BasicDataSource dataSource = new BasicDataSource();
        String sql;

        Class.forName(driverName);
        dataSource.setDriverClassName(driverName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        //dataSource.setMaxTotal(DipConfig.getConfigInstance().getPoolMaxTotal());
        dataSource.setInitialSize(10);
        dataSource.setMaxActive(8);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);
        // Default settings
        dataSource.setTestOnBorrow(true);
        if (driverName.contains("OracleDriver")) {
            sql = "select 1 from dual";
        }else {
            sql = "select 1";
        }
        dataSource.setValidationQuery(sql);
        dataSource.setRemoveAbandoned(true);
        //dataSource.setRemoveAbandonedOnBorrow(true);
        //dataSource.setRemoveAbandonedOnMaintenance(true);
        dataSource.setRemoveAbandonedTimeout(300);

        return dataSource;
    }

    private JDBCUtils() {
    }

    public static void clearDataSourcePool(String key) {
        if (dataSourcePool.get(key) != null) {
            dataSourcePool.remove(key);
        }
    }

    public static String getKey(String url, String username, String password) {
        String key = String.format("%s|%s", url, username);
        if (!StringUtils.isEmpty(password)) {
            key += "|" + password;
        }
        return key;
    }

    public static void closeQuietly(final ResultSet rs) {
        closeQuietly((AutoCloseable) rs);
    }

    public static void closeQuietly(final Statement stmt) {
        closeQuietly((AutoCloseable) stmt);
    }

    public static void closeQuietly(final Connection conn) {
        closeQuietly((AutoCloseable) conn);
    }

    public static void closeQuietly(final AutoCloseable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
