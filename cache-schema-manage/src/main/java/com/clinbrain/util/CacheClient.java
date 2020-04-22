package com.clinbrain.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class CacheClient {
    private static final String driverName = "com.intersys.jdbc.CacheDriver";

    public static Connection getConne(String url, String username, String password) throws Exception {
        Class.forName(driverName);
        Connection connection = DriverManager.getConnection(url, username, password);
        return connection;
    }



}
