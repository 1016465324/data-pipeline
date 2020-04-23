package com.clinbrain.util;

import java.sql.ResultSet;
import java.util.List;

public class CacheClient extends BasicClient{

    private static final String driverName = "com.intersys.jdbc.CacheDriver";

    public CacheClient() {
    }

    public CacheClient(String url, String username , String password) {
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
        try {
            if(stat != null) {
                return stat.executeQuery(sql);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
