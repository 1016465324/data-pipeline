package com.clinbrain.database;

import org.apache.commons.dbutils.BeanProcessor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public interface IDatabaseClient extends AutoCloseable{

    List<Map<String, Object>> queryWithResult(String sql) throws Exception;

    ResultSet queryWithResultSet(String sql) throws Exception;

    <T> List<T> queryWithBean(String sql, Class<T> cls, BeanProcessor processor) throws Exception;

    Map<String, Object> queryScalar(String sql) throws Exception;

    void executeSQL(String sql) throws Exception;

    int executeUpdate(String sql, String... database) throws Exception;

    void executeSQL(List<String> sqls) throws Exception;

    long getTableRows(String database, String tableName) throws Exception;

    List<String> getDbNames() throws Exception;

    List<String> getTableNames(String database) throws Exception;

    TableMeta getTableMeta(String database, String tableName) throws Exception;

    List<TableMeta> getAllTableMeta(String database) throws Exception;

    void close();

    Connection getConnection();
}
