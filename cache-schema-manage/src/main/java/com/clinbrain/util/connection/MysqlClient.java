package com.clinbrain.util.connection;

import java.sql.ResultSet;
import java.util.List;

public class MysqlClient extends BasicClient{

    private static String driverName = "com.mysql.jdbc.Driver";

    public MysqlClient(String url, String username, String password) {
        super.init(driverName, url, username, password);
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
    public ResultSet executeQuery(String sql) {
        return super.executeQuery(sql);
    }
}
