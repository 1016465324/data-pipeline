package com.clinbrain.database;

import com.clinbrain.utils.CommonUtils;
import com.clinbrain.utils.JDBCUtils;
import com.google.common.collect.Lists;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * cache官方文档
 * https://docs.intersystems.com/latest/csp/docbook/DocBook.UI.Page.cls?KEY=ROBJ_classdef_property
 */
public class CachedbClient implements IDatabaseClient {
    private Connection conn;
    private Statement stmt;
    private DatabaseMetaData metaData;

    private Pattern columnNamePattern = Pattern.compile("(^[%|_]+)(\\w+)");

    public CachedbClient(String url, String username, String password) {
        init(url, username, password);
    }

    private void init(String url, String username, String password) {
        try {
            String driverName = "com.intersys.jdbc.CacheDriver";
            DataSource dataSource = JDBCUtils.getDataSource(driverName, url, username, password);
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            metaData = conn.getMetaData();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int executeUpdate(String sql,String ...database) throws Exception{
        return stmt.executeUpdate(sql);
    }

    @Override
    public Map<String, Object> queryScalar(String sql) throws Exception {
        List<Map<String, Object>> list = queryWithResult(sql);
        Map<String, Object> result = null;
        if (list.size() > 0) {
            result = list.get(0);
        }
        return result;
    }

    /**
     * 直接查询sql 转成 java bean ,
     * @param sql sql语句
     * @param cls java bean
     * @param processor 转换方式，默认：bean property名称跟sql select 一样，2.new GenerousBeanProcessor() 含下划线转java bean
     * @param <T>
     * @return
     * @throws Exception
     */
    @Override
    public <T> List<T> queryWithBean(String sql, Class<T> cls, BeanProcessor processor) throws Exception {
        QueryRunner runner = new QueryRunner();
        if (processor == null) {
            processor = new BeanProcessor();
        }
        ResultSetHandler<List<T>> h =
                new BeanListHandler<>(cls, new BasicRowProcessor(processor));
        return runner.query(conn, sql, h);
    }

    @Override
    public List<Map<String, Object>> queryWithResult(String sql) throws Exception {
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, Object>> result = CommonUtils.buildList(rs);
        JDBCUtils.closeQuietly(rs);
        return result;
    }

    @Override
    public ResultSet queryWithResultSet(String sql) throws Exception {
        return stmt.executeQuery(sql);
    }

    @Override
    public void executeSQL(String sql) throws Exception {
        stmt.execute(sql);
    }

    @Override
    public void executeSQL(List<String> sqls) throws Exception {
        for (String sql : sqls) {
            executeSQL(sql);
        }
    }

    @Override
    public long getTableRows(String database, String table) throws Exception {
        ResultSet rs = null;
        long count = 0;
        try {
            rs = stmt.executeQuery("select count(*) from " + database + "." + table);
            while (rs.next()) {
                count = rs.getLong(1);
            }
        } finally {
            JDBCUtils.closeQuietly(rs);
        }
        return count;
    }

    @Override
    public TableMeta getTableMeta(String database, String tableName) throws Exception {
        List<TableMeta.ColumnMeta> allColumns = Lists.newArrayList();
        List<TableMeta.ColumnMeta> pkColumns = Lists.newArrayList();
        List<TableMeta.ColumnMeta> generatedColumns = Lists.newArrayList();
        TableMeta.Builder builder = TableMeta.builder();

        builder.setDatabase(database);
        builder.setTableName(tableName);

        ResultSet resultSet = stmt.executeQuery("select * from information_schema.columns " +
                "where table_schema = '"+database+"' and table_name = '"+tableName+"'");

        if(!resultSet.next()) {
            resultSet = stmt.executeQuery("SELECT * FROM %TSQL_sys.columns where \"schema\"= '"+database+"' and parent_obj_name  = '"+tableName+"'");
            while (resultSet.next()) {
                String dataType = resultSet.getString(6);
                dataType = dataType.indexOf(".") > 0 ? StringUtils.substringAfter(dataType, ".") : dataType;

                String columnName = Optional.ofNullable(resultSet.getString(8)).orElse("").toLowerCase();
                String dataLength = Optional.ofNullable(resultSet.getString(5)).orElse(null);

                Matcher matcher = columnNamePattern.matcher(columnName);
                if(matcher.find()) {
                    columnName = matcher.group(2);
                }
                TableMeta.ColumnMeta columnMeta = new TableMeta.ColumnMeta(
                        columnName, StringUtils.lowerCase(dataType), "", dataLength);
                allColumns.add(columnMeta);
                // cache的主键是根据列的排序来的，默认是ID，如果你已经创建了一个ID, 那就是ID1,很怪
                if (resultSet.getInt("colid") == 1) {
                    pkColumns.add(columnMeta);
                }
                //目前这种查询无法获取那个字段为计算字段
            }
        }else {
            // 查询表结构从information_schema 表来查

            do {
                String dataType = resultSet.getString("DATA_TYPE");
                dataType = dataType.indexOf(".") > 0 ? StringUtils.substringAfter(dataType, ".") : dataType;

                String columnName = Optional.ofNullable(resultSet.getString("COLUMN_NAME")).orElse("").toLowerCase();
                String dataLength = Optional.ofNullable(resultSet.getString("CHARACTER_MAXIMUM_LENGTH")).orElse(null);

                Matcher matcher = columnNamePattern.matcher(columnName);
                if(matcher.find()) {
                    columnName = matcher.group(2);
                }

                TableMeta.ColumnMeta columnMeta = new TableMeta.ColumnMeta(
                        columnName, StringUtils.lowerCase(dataType), "", dataLength);
                allColumns.add(columnMeta);
                if ("YES".equalsIgnoreCase(resultSet.getString("PRIMARY_KEY"))) {
                    pkColumns.add(columnMeta);
                }

                if ("YES".equalsIgnoreCase(resultSet.getString("IS_GENERATED"))) {
                    generatedColumns.add(columnMeta);
                }
            } while (resultSet.next());
        }
        builder.setAllColumns(allColumns);
        builder.setPrimaryKeyColumns(pkColumns);
        builder.setGeneratedColumns(generatedColumns);
        JDBCUtils.closeQuietly(resultSet);

        return builder.get();
    }

    @Override
    public List<String> getDbNames() throws Exception {
        List<String> ret = Lists.newArrayList();
        ResultSet schemas = metaData.getSchemas();
        while (schemas.next()) { // 过滤掉系统表 ： 以”%“ 开头的库
            String name = Optional.ofNullable(schemas.getString(1)).orElse("").toLowerCase();
            if(!StringUtils.startsWith(name,"%")) {
                ret.add(name);
            }
        }
        JDBCUtils.closeQuietly(schemas);
        return ret;
    }

    @Override
    public List<String> getTableNames(String database) throws Exception {
        List<String> ret = Lists.newArrayList();
        ResultSet tables = stmt.executeQuery("SELECT table_name FROM information_schema.tables where table_schema='"+database+"'");

        if(tables.next()) {
            do{
                ret.add(Optional.ofNullable(tables.getString(1)).orElse("").toLowerCase());
            }while (tables.next());
        }else {
            tables = stmt.executeQuery("SELECT DISTINCT parent_obj_name FROM %TSQL_sys.columns where \"SCHEMA\"='" + database + "'");
            while (tables.next()) {
                ret.add(Optional.ofNullable(tables.getString(1)).orElse("").toLowerCase());
            }
        }
        JDBCUtils.closeQuietly(tables);
        return ret;
    }

    @Override
    public List<TableMeta> getAllTableMeta(String database) throws Exception {
        List<TableMeta> ret = Lists.newArrayList();
        for (String table : getTableNames(database)) {
            ret.add(getTableMeta(database, table));
        }
        return ret;
    }

    @Override
    public Connection getConnection() {
        return this.conn;
    }

    @Override
    public void close() {
        JDBCUtils.closeQuietly(stmt);
        JDBCUtils.closeQuietly(conn);
    }
}
