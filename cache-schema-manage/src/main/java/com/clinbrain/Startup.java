package com.clinbrain;

import com.alibaba.fastjson.JSONObject;
import com.clinbrain.util.CacheClient;
import com.clinbrain.util.Constans;
import com.clinbrain.util.PopLoadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 日志解析启动类
 */
public class Startup {

    private static final Logger logger = LoggerFactory.getLogger(Startup.class);
    private static Properties pop = new Properties();
    private static Statement cacheStat = null;

    static {
        try {
            // 加載properties文件
            pop = PopLoadUtils.loadProperties(null);
            // 加载cacheConne
            Connection cacheConn = CacheClient.getConne(Startup.pop.getProperty("cache.url"), Startup.pop.getProperty("cache.username"),
                    Startup.pop.getProperty("cache.password"));
            cacheStat = cacheConn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 模拟启动
     * @param args
     */
    public static void main(String[] args) {
        String nameSpace = pop.getProperty("cache.logs.namespace");
        StartParse(nameSpace);
    }


    /**
     * 启动接口
     */
    public static void StartParse(String dbName){
        try {
            //1.查取数据
            ResultSet rs = getResultLogs(dbName);
            //2.解析开始
            List<String> messages = ParseCacheLogs.startDealWithLogs(rs);
            //打印成表结构
            printToTable(messages);
        }catch (Exception e){
            logger.error(String.format("实时数据还原过程出错: %s", e.getMessage()));
            e.printStackTrace();
        }finally {
            try {
                cacheStat.close();
            } catch (SQLException e) {
                logger.error(String.format("关闭cache数据库连接错误: %s", e.getMessage()));
            }
        }
    }


    /**
     * get Logs
     * @return
     * @throws Exception
     */
    private static ResultSet getResultLogs(String dbName) throws SQLException{
        String startTime = pop.getProperty("cache.logs.startTime");
        String batchSize = pop.getProperty("cache.logs.batchSize");
        return cacheStat.executeQuery(String.format(Constans.query_logs_sql, dbName, startTime, batchSize));
    }


    /**
     * 将结果集按表形成打印
     */
    private static void printToTable(List<String> messages) throws Exception{
        List<Object> table = new ArrayList<>();
        for (String m : messages) {
            JSONObject jObj = JSONObject.parseObject(m);
            //表名
            String namespace = jObj.getJSONObject("shema").getString("namespace");
            String tableName = namespace.substring(namespace.lastIndexOf(".") + 1);
            table.add("表名:" + tableName);
            StringBuilder col = new StringBuilder("字段:");
            jObj.getJSONObject("shema").getJSONArray("fields").forEach(field -> {
                col.append(JSONObject.parseObject(field.toString()).getString("name")).append("||");
            });
            String colName = col.delete(col.lastIndexOf("||"), col.length()).toString();
            table.add(colName);

            StringBuilder vl = new StringBuilder("数据:");
            jObj.getJSONObject("payload").getJSONArray("after").forEach(value -> {
                vl.append(value).append("||");
            });
            String value = vl.delete(vl.lastIndexOf("||"), vl.length()).toString();
            table.add(value);
        }
        table.forEach(m -> System.out.println(m));
    }

}
