package com.clinbrain.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

public class CacheClient extends BasicClient{
    private static final String driverName = "com.intersys.jdbc.CacheDriver";

    public static Connection getConne(String url, String username, String password) throws Exception {
        //Class.forName(driverName);
        //Connection connection = DriverManager.getConnection(url, username, password);
        DataSource connection = DBUtils.getDataSouce(driverName, url, username, password);
        return connection.getConnection();
    }

    @Override
    public List<String> getDBNames() {
        return super.getDBNames();
    }

    @Override
    public List<String> getTableNames(String database) {
        return super.getTableNames(database);
    }

    @Override
    public List<Object> getTableMetas(String database, String tableName) {
        return super.getTableMetas(database, tableName);
    }

    @Override
    public List<String> qury(String database) {
        return super.qury(database);
    }
}
