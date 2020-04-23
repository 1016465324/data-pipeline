package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.clinbrain.util.Constans;
import com.clinbrain.util.PopLoadUtils;
import com.clinbrain.util.connection.CacheClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * 日志解析启动类
 */
public class Startup {

    private static final Logger logger = LoggerFactory.getLogger(Startup.class);
    private static Properties pop = new Properties();
    private static ParseCacheLogs parseCacheLogs;
    private static CacheClient cacheClient = null;

    static {
        try {
            // 加載properties文件
            pop = PopLoadUtils.loadProperties(null);
            // 加载cacheClient
            cacheClient = new CacheClient(Startup.pop.getProperty("cache.url"), Startup.pop.getProperty("cache.username"),
                    Startup.pop.getProperty("cache.password"));
            parseCacheLogs = new ParseCacheLogs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 模拟启动
     * @param args
     */
    public static void main(String[] args) {
        Startup startup = new Startup();
        startup.StartParse(startup);
    }


    /**
     * 启动接口
     */
    private void StartParse(Startup startup){
        String errMsg = "";
        try {
            //1.查取数据
            ResultSet rs = getResultLogs();
            //2.解析开始
            List<String> messages = parseCacheLogs.startDealWithLogs(rs);
            //打印成表结构
            //printToTable(messages);
            System.out.println(JSON.parseObject(messages.get(0)).toJSONString());
        }catch (Exception e){
            errMsg = e.getMessage();
            logger.error("实时数据还原过程出错: {}.", e.getMessage());
        }finally {
            startup.addShutdownHook(errMsg); //钩子函数
        }
    }


    /**
     * 收尾存入下标
     */
    private void addShutdownHook(String errMsg){
       Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
           @Override
           public void run() {
               if(parseCacheLogs != null)
                   parseCacheLogs.saveOffsetInfo(errMsg);
               else
                   System.out.println("Startup配置加载出错");
           }
       }));
    }


    /**
     * get Logs
     * @return
     * @throws Exception
     */
    private static ResultSet getResultLogs() throws SQLException{
        String dbName = pop.getProperty("cache.logs.database");
        String startTime = pop.getProperty("cache.logs.dealWithTime");
        String batchSize = pop.getProperty("cache.logs.batchSize");
        return cacheClient.executeQuery(String.format(Constans.query_logs_sql, dbName, startTime, batchSize));
    }


    /**
     * 将结果集按表形成打印
     */
//    private static void printToTable(List<String> messages) throws Exception{
//        List<Object> table = new ArrayList<>();
//        for (String m : messages) {
//            JSONObject jObj = JSONObject.parseObject(m);
//            //表名
//            String namespace = jObj.getJSONObject("schema").getString("namespace");
//            String tableName = namespace.substring(namespace.lastIndexOf(".") + 1);
//            table.add("表名:" + tableName);
//            StringBuilder col = new StringBuilder("字段:");
//            jObj.getJSONObject("schema").getJSONArray("fields").forEach(field -> {
//                col.append(JSONObject.parseObject(field.toString()).getString("name")).append("||");
//            });
//            String colName = col.delete(col.lastIndexOf("||"), col.length()).toString();
//            table.add(colName);
//
//            StringBuilder vl = new StringBuilder("数据:");
//            jObj.getJSONObject("payload").getJSONArray("after").forEach(value -> {
//                vl.append(value).append("||");
//            });
//            String value = vl.delete(vl.lastIndexOf("||"), vl.length()).toString();
//            table.add(value);
//        }
//        table.forEach(m -> System.out.println(m));
//    }

}
