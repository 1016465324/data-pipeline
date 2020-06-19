package com.clinbrain.util.connection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CacheClient extends BasicClient {

    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    private static final String driverName = "com.intersys.jdbc.CacheDriver";

    public CacheClient(String url, String username, String password) {
        super(driverName, url, username, password);
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
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            if (statement != null) {
                resultSet = statement.executeQuery(sql);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return resultSet;
    }

    @Override
    public void close() {
        try {
            if (null != resultSet) {
                resultSet.close();
                resultSet = null;
            }

            if (null != statement) {
                statement.close();
                statement = null;
            }

            if (null != connection) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
