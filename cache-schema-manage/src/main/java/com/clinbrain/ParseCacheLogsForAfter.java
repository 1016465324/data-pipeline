package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clinbrain.model.*;
import com.clinbrain.rsultMessage.DataType;
import com.clinbrain.rsultMessage.Message;
import com.clinbrain.rsultMessage.MessageBuilder;
import com.clinbrain.util.Constans;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple4;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @Description cache实时日志还原
 * @Author xhy
 * @Date 2020/4/8 10:39
 */
public class ParseCacheLogsForAfter {

    private static final Logger logger = LoggerFactory.getLogger(ParseCacheLogsForAfter.class);

    private static GlobalManager globalManager = null;
    private static Properties pop = new Properties();
    private static Statement cacheStat = null;
    private static Map<String, String> localCacheDataMap = new HashMap<>(); //本地缓存日志
    private static Map<String, List<ClassPropertyDefine>> allGlobalNodeProperty = new HashMap<>();
    private static Map<String, Set<String>> allGlobalNodeOfClass =new HashMap<>();
    private static Map<String, List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>>> allGlobalNodeStorage = new HashMap<>();
    private static Map<String, Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable>> allClassInfo = new HashMap<>();
    private static Map<String, TableMeta> allTableMeta = new HashMap<>();

    static{
        try{
            // 加載properties文件
            FileInputStream fs = new FileInputStream("D:\\workspace\\data-pipeline\\cache-schema-manage\\src\\main\\resources\\application.properties");
            pop.load(fs);
            // 加载cacheConne
//            Connection cacheConn = CacheClient.getConne(pop.getProperty("cache.url"), pop.getProperty("cache.username"), pop.getProperty("cache.password"));
            Connection cacheConn = null;
            cacheStat = cacheConn.createStatement();
            // 加载cache配置
//            globalManager = new GlobalManager();
            globalManager.generateRule();
            allGlobalNodeOfClass = globalManager.getAllGlobalNodeOfClass();
            allGlobalNodeProperty = globalManager.getAllGlobalNodeProperty();
            allGlobalNodeStorage = globalManager.getAllGlobalNodeStorage();
            allClassInfo = globalManager.getAllClassInfo();
//            allTableMeta = globalManager.getAllTableMeta();
        }catch (Exception e){
            logger.error(String.format("实时还原,加载元数据发生错误: %s", e.getMessage()));
            e.printStackTrace();
        }
    }

    /**
     * 模拟启动
     * @param args
     */
    public static void main(String[] args) {
        List<Message> messages = dealWithGlobal();
        messages.forEach(m -> System.out.println(m.toString()));
    }

    /**
     * deal cachelogs
     * @return
     */
    public static List<Message> dealWithGlobal(){
        try{
            //接收日志
            ResultSet rs = getResultLogs();
            List<Message> resMessages = new ArrayList<>(); //返回消息体

            long Tstart = System.currentTimeMillis();
            while (rs.next()) {

                JSONObject parse = null;
                try{
                    //1.获取得到对应数据部分、global名称
                    parse = JSON.parseObject(rs.getString(2));
                    System.out.println(String.format("当前处理数据: %s", parse.toJSONString()));

                    //处理日志global内容
                    Object[] objects = dealGlobalName(parse); //格式化globalName
                    String globParseName = (String) objects[0];
                    String[] logExpressArr = (String[]) objects[1]; //expression节点集

                    //localCacheDataMap.put(globAlias, parse.toJSONString());//todo 本地缓存当前日志数据

                    //2.剃除非处理数据
                    if (!allGlobalNodeStorage.containsKey(globParseName)) {
                        continue;
                    }

                    //根据globalParseName得到节点、属性二元组集合
                    List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>> tuple2s = allGlobalNodeStorage.get(globParseName);
                    for (Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>> tuple2 : tuple2s) { //定位当前global对应property
                        if (compareGlobalNodeInfo(logExpressArr, tuple2._1)) { //global匹配上了
                            String globalAlias = getGlobalAlias(parse.getString("Glob"), tuple2._1); //组装全称
                            List<ClassPropertyDefine> classPropDef = tuple2._2;
                            String className = classPropDef.get(0).getClassName(); //得到当前global对应类
                            List<String> allGlobals = new ArrayList<>(allGlobalNodeOfClass.get(className)); //得到类对应所有global

                            DataTable dataTable = allClassInfo.get(className)._4(); //得到数据表信息
                            String schemaName = dataTable.getSchemaName(); // schemaName
                            String tableName = dataTable.getTableName(); // 表名

                            if (allGlobals.size() > 1) { //多个global批处理
                                resMessages.add(buildMessage(allGlobals, parse, globalAlias, schemaName, tableName));
                            } else { //单个global直接赋值此条数据
                                resMessages.add(resMessages(allGlobals.get(0), parse, schemaName, tableName));
                            }
                        }
                    }
                }catch (Exception e){
                    logger.error(String.format("处理当前数据%s出错: %s", parse.toJSONString(), e.getMessage()));
                }
            }
            long Tend = System.currentTimeMillis();
            System.out.println(String.format(String.format("处理时长总计: %s", Tend - Tstart)));
            return resMessages;
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
        return null;
    }


    /**
     * 构建多global节点存储类型的返回消息体（只取after）
     * @param globals
     * @param parse
     * @param globalAlias
     * @return
     */
    private static Message buildMessage(List<String> globals, JSONObject parse, String globalAlias, String schemaName, String tableName) {
        MessageBuilder builder = new MessageBuilder();

        String namespace = String.format("cache.his.%s.%s.%s", "NameSpace", schemaName, tableName); //拼接namespace

        builder.build(Message.ProtocolType.AFTER, namespace, 3);
        List<Object> rowDataValues = new ArrayList<>();

        globals.forEach(global -> {
            JSONArray after = null;
            if(StringUtils.equals(global, globalAlias)) { //当前数据
                after = parse.getJSONArray("After");
            }else { //非当前数据，默认先从本地缓存查询，否则接口调用查取
                //after = localCacheDataMap.get();
                if(after == null){
                    //after = getLog();//调用接口查询
                }
            }

            buildMessageByGlobal(global, after, rowDataValues, builder, tableName); //构建数据部分
        });
        builder.appendPayload(rowDataValues.toArray());
        return builder.message;
    }


    /**
     * 构建单global节点存储类型的返回消息体(只取after)
     * @param parse
     * @return
     */
    private static Message resMessages(String global, JSONObject parse, String schemaName, String tableName) {

        MessageBuilder builder = new MessageBuilder();

        String namespace = String.format("cache.his.%s.%s.%s", "nameSpace", schemaName, tableName); //拼接namespace

        builder.build(Message.ProtocolType.AFTER, namespace, 3);
        List<Object> rowDataValues = new ArrayList<>();
        JSONArray after = parse.getJSONArray("After");

        buildMessageByGlobal(global, after, rowDataValues, builder, tableName); //构建数据部分

        builder.appendPayload(rowDataValues.toArray());
        return builder.message;
    }


    /**
     * 构建消息体数据部分
     * @param global
     * @param after
     * @param rowDataValues
     * @param builder
     */
    public static void buildMessageByGlobal(String global, JSONArray after, List<Object> rowDataValues, MessageBuilder builder, String tableName) {
        //当前global对应属性
        List<ClassPropertyDefine> fields = allGlobalNodeProperty.get(global);

        for (int i = 0; i < after.size(); i++) { //!!根据数据的长度取数据

            ClassPropertyDefine classPropDef = fields.get(i);

            String sqlFieldName = classPropDef.getSqlFieldName(); //columnName
            String table_field = tableName + "_" + sqlFieldName; //key
            TableMeta tableMeta = allTableMeta.get(table_field);
            String dataType = tableMeta.getDataType(); //columnType

            if(StringUtils.equalsIgnoreCase(classPropDef.getPropertyCollection(), "list")){ //属性为list类型则作list对应格式拼接
                JSONArray subListJsonArray = after.getJSONArray(i);
                StringBuilder sb = new StringBuilder("[");
                for (Object o : subListJsonArray) {
                    sb.append(o + classPropDef.getSqlListDelimiter() == null ? ","
                            : classPropDef.getSqlListDelimiter()); //list类型对应数据拼接默认分隔符为","
                }
                rowDataValues.add(sb.toString().substring(0, sb.lastIndexOf(",")) + "]");
                builder.appendSchema(sqlFieldName, DataType.convertMysqlDataType(dataType), false, false); //columnName
            }else if(!StringUtils.startsWith(classPropDef.getRuntimeType(), "%")){ //属性为对象类型则找子属性
                //根据对象属性找到其子属性
                Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefDataTabTup4 = allClassInfo.get(classPropDef.getClassName());
                if(StringUtils.equals(classDefDataTabTup4._1().getClassType(), "serial")){ //序列华类型, 需展开,字段拼接
                    List<ClassPropertyDefine> subSeriClassPropDef = classDefDataTabTup4._3(); //类对应属性
                    JSONArray subSeriJsonArr = after.getJSONArray(i);
                    for (int j = 0; j < subSeriJsonArr.size(); j++) { //!!根据数据的长度取数据
                        String subColName = String.format("%s_%s", classPropDef.getSqlFieldName(),
                                subSeriClassPropDef.get(j).getSqlFieldName());
                        String subColValue = (String) subSeriJsonArr.get(j);

                        rowDataValues.add(subColValue);
                        builder.appendSchema(subColName, DataType.convertMysqlDataType(dataType), false, false); //columnName
                    }
                }else {
                    rowDataValues.add(after.getString(i));
                    builder.appendSchema(sqlFieldName, DataType.convertMysqlDataType(dataType), false, false); //columnName
                }
            }else { //一般属性
                rowDataValues.add(after.getString(i));
                builder.appendSchema(sqlFieldName, DataType.convertMysqlDataType(dataType), false, false); //columnName
            }
        }
    }


    /**
     * 格式化globalName
     * @return
     */
    private static Object[] dealGlobalName(JSONObject parse) {
        String globName = String.format("^%s", parse.getString("Glob")); //globalName
        String globRefNode = String.format("^%s", parse.getString("GlobalReference")); //提取global节点信息
        String[] logExpressions = globRefNode.substring(globRefNode.indexOf("(") + 1, globRefNode.indexOf(")")).split(",");
        int globalNodelength = logExpressions.length; //获取global节点数目
        String globalParseName = String.format("%s_%s", globName, globalNodelength);
        return new Object[]{globalParseName, logExpressions};
    }


    /**
     * 组装global全称
     * @param globalName
     * @param subscriptDefines
     * @return
     */
    private static String getGlobalAlias(String globalName, List<StorageSubscriptDefine> subscriptDefines) {
        StringBuilder sb = new StringBuilder(String.format("^%s(", globalName));
        for (StorageSubscriptDefine subscriptDefine : subscriptDefines) {
//            if(subscriptDefine.getIsRowid() == 1)//常量需加引号
//                sb.append("\"" + subscriptDefine.getExpression() + "\"").append(",");
//            else
                sb.append(subscriptDefine.getExpression()).append(",");
        }

        return  sb.toString().substring(0, sb.toString().lastIndexOf(",")) + ")";
    }


    /**
     * 比较配置与当前日志中global节点常量是否一致
     * @param logExpressions
     * @return
     */
    private static boolean compareGlobalNodeInfo(String[] logExpressions, List<StorageSubscriptDefine> subscriptDefines) {
        boolean flag = true;
        for (int i = 0; i < logExpressions.length; i++) {
            if(subscriptDefines.get(i).getIsRowid() == 0 && !StringUtils.equals(subscriptDefines.get(i).getExpression(), logExpressions[i])){ //配置中的global节点常量
                //两边不匹配时
                flag = false;
                break;
            }
        }
        return flag;
    }


    /**
     * get Logs
     * @return
     * @throws Exception
     */
    public static ResultSet getResultLogs() throws Exception{
        ResultSet rs = cacheStat.executeQuery(Constans.query_logs_sql);
        return rs;
    }

}
