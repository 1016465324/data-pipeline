package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.clinbrain.util.Constans;
import com.clinbrain.util.PopLoadUtils;
import com.clinbrain.util.connection.CacheClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 日志解析启动类
 */
public class Startup {

    private static final Logger logger = LoggerFactory.getLogger(Startup.class);
    private static Properties pop = new Properties();
    private static ParseCacheLogs parseCacheLogs;
    private static CacheClient cacheClient = null;
    private static CacheClient cacheDbNameClient = null;
    private static Map<String, String> offsetMap = new HashMap<>(); //下标偏移量记录

    //TODO 加载所有dbName连接源
    //TODO 下标存入zookeeper
    //TODO 字段流存储测试
    static {
        try {
            // 加載properties文件
            pop = PopLoadUtils.loadProperties(null);
            // 加载cacheClient
            cacheClient = new CacheClient(Startup.pop.getProperty("cache.url"), Startup.pop.getProperty("cache.username"),
                    Startup.pop.getProperty("cache.password"));
            // 加载dbName连接源
            //cacheDbNameClient = new CacheClient(Startup.pop.getProperty("cache.glob.url"), Startup.pop.getProperty("cache.glob.username"),
            //        Startup.pop.getProperty("cache.glob.password"));
            parseCacheLogs = new ParseCacheLogs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 模拟启动
     * @param args
     */
    public static void main(String[] args) throws Exception{
        Startup startup = new Startup();
        Map<String, String> params = queryInitParams();
        String initDate = params.get("initDate");
        String initRange = params.get("initRange");
        int step = Integer.parseInt(pop.getProperty("cache.log.readStep"));
        ResultSet rs = null;
        while (true){
            //1.查取数据
            rs = getResultLogs(initDate, initRange);
            if(rs == null){ //未获取到日志
                if(isLessThanNow(initDate)){ //小于当前日期
                    initDate = addOneDate(initDate); //时间加一天
                    initRange = params.get("initRange"); //范围重置
                    continue;
                }else {
                    Thread.sleep(3000); //当前日期查询不到,等待5秒重试
                }
            }else { //获取到日志
                //执行
                startup.run(rs);
                initRange = addOneStep(initRange, step); //范围右偏移
            }
        }
    }


    /**
     * 启动接口
     */
    private void run(ResultSet rs){

        String logData = "";
        String messages = "";
        String offset_key = ""; //namespace + dbname
        String offset_value = ""; //处理日期 + 批处理当前下标
        String errMsg = "";

        int batch_index = 0;
        long Tstart = System.currentTimeMillis();
        List<String> resMessages = new ArrayList<>(); //返回消息体
        try {
            //2.解析开始
            while (rs.next()){
                System.out.println(batch_index);
                offset_value = String.format("%s_%s", pop.getProperty("cache.logs.dealWithTime"), batch_index);
                batch_index++;
                logData = rs.getString(2);
                String[] resultArr = parseCacheLogs.startDealWithLogs(logData);
                offset_key = resultArr[0];
                messages = resultArr[1];
                if(StringUtils.isNotEmpty(offset_key)) {
                    offsetMap.put(offset_key, offset_value); //批处理下标记录
                    resMessages.add(messages);
                }
            }
            System.out.println(String.format(String.format("处理时长总计: %s", System.currentTimeMillis() - Tstart)));

            //printToTable(messages); //打印成表结构
            resMessages.forEach(res -> {
                String s = JSON.parseObject(res).toJSONString();
                System.out.println(s);
            });
        }catch (Exception e){
            errMsg = e.getMessage();
            logger.error("实时数据还原过程出错 {}: {}.", e.getMessage(), logData);
            //offsetMap.put(offset_key, offset_value); //批处理下标记录
            //throw new RuntimeException(String.format("%s: %s", e.getMessage(), logData));
        } finally {
            //startup.addShutdownHook(offsetMap, errMsg); //钩子函数
        }
    }


    /**
     * 判断是否为当前日期
     * @param initDate
     * @return
     */
    private static boolean isLessThanNow(String initDate) throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyMMdd");
        Date now = sdf.parse(sdf.format(new Date()));
        Date bfDate = sdf.parse(initDate);
        return bfDate.before(now);
    }


    /**
     * TODO 读取初始值查询初始化参数
     * @return
     */
    private static Map<String, String> queryInitParams() {
        Map<String, String> param = new HashMap<>();
        String initDate = "20200428";
        String initRange = "1:10000";
        param.put("initDate", initDate);
        param.put("initRange", initRange);
        return param;
    }


    /**
     * 日志下标偏移
     * @param initRange
     * @param readStep
     * @return
     */
    private static String addOneStep(String initRange, int readStep) {
        String[] rangeArr = StringUtils.split(initRange, ":");
        int startRg = Integer.parseInt(rangeArr[0]) + readStep;
        int endRg = Integer.parseInt(rangeArr[1]) + readStep;
        return String.format("%s:%s", startRg, endRg);
    }


    /**
     * 时间偏移
     * @param initDate
     * @return
     */
    private static String addOneDate(String initDate) throws Exception{
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = new GregorianCalendar();
        Date date = sdf.parse(initDate);
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 1);
        String now = sdf.format(calendar.getTime());
        return now;
    }


    /**
     * 收尾存入下标
     */
    private void addShutdownHook(Map<String, String> offsetMap, String errMsg){
       Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
           @Override
           public void run() {
               if(parseCacheLogs != null)
                   parseCacheLogs.saveOffsetInfo(offsetMap, errMsg);
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
    private static ResultSet getResultLogs(String initDate, String initRange) throws SQLException{
        String dbName = pop.getProperty("cache.logs.database");
        //String startTime = pop.getProperty("cache.logs.dealWithTime");
        //String batchSize = pop.getProperty("cache.logs.batchSize");
        return cacheClient.executeQuery(String.format(Constans.query_logs_sql, dbName, initDate, initRange));
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
