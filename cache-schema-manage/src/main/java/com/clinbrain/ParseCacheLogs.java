package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clinbrain.mapper.ParseLogDetailsMapper;
import com.clinbrain.mapper.ParseLogsMapper;
import com.clinbrain.model.*;
import com.clinbrain.rsultMessage.DataType;
import com.clinbrain.rsultMessage.Message;
import com.clinbrain.rsultMessage.MessageBuilder;
import com.clinbrain.util.ErrorInfo;
import com.clinbrain.util.PopLoadUtils;
import com.intersys.cache.Dataholder;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple4;

import java.io.InputStream;
import java.sql.ResultSet;
import java.util.*;

import static com.intersys.objects.Database.RET_PRIM;

/**
 * @Description cache实时日志还原
 * @Author xhy
 * @Date 2020/4/8 10:39
 */
public class ParseCacheLogs {

    private static final Logger logger = LoggerFactory.getLogger(ParseCacheLogs.class);

    private static Properties pop;
//    private static KafkaSink kafkaSink;
    private static Map<String, GlobalManager> allGlobalManager;
    private static Map<String, String> namespaceMap;
    private static Map<String, Database> cacheQueryConnMap = new HashMap<>();
    private static Map<String, String> localCacheDataMap = new HashMap<>(); //本地缓存日志
    private static Map<String, String> offsetMap = new HashMap<>(); //下标偏移量记录
    private static ParseLogsMapper parseLogsMapper;
    private static ParseLogDetailsMapper parseLogDetailsMapper;

    static{
        try{
            pop = PopLoadUtils.loadProperties(null);
//            kafkaSink = new KafkaSink(); //加载kafka对象
            allGlobalManager = GlobalManager.buildGlobalManager();
            namespaceMap = GlobalManager.buildGlobalDbMapNamespace(pop.getProperty("cache.glob.url"), pop.getProperty("cache.glob.username"),
                    pop.getProperty("cache.glob.password"));
            InputStream resourceAsStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
            SqlSessionFactory build = new SqlSessionFactoryBuilder().build(resourceAsStream);
            SqlSession sqlSession = build.openSession();
            parseLogsMapper = sqlSession.getMapper(ParseLogsMapper.class);
            parseLogDetailsMapper = sqlSession.getMapper(ParseLogDetailsMapper.class);
        }catch (Exception e){
            logger.error("实时还原,加载元数据发生错误: {}.", e.getMessage());
            //e.printStackTrace();
            throw new RuntimeException(String.format("实时还原,加载元数据发生错误: %s", e.getMessage()));
        }
    }


    /**
     * deal cachelogs
     * @return
     */
    public static List<String> startDealWithLogs(ResultSet rs) throws Exception{
        //造数据
        //String str = "{\"Before\":\"[26,\\\"碘1\\\",\\\"Y\\\"]\",\"Address\":3735756,\"RecordType\":\"S\",\"GlobalReference\":\"DHCALLGYKEY(19)\",\"InTransaction\":1,\"After\":\"[26,\\\"碘\\\",\\\"Y\\\"]\",\"DBName\":\"DHC-DATA\",\"Glob\":\"DHCALLGYKEY\",\"Timestamp\":\"2020-04-11 15:51:03\"}";
        //ArrayList<String> dataSourceDist = new ArrayList<>();
        //for (int i = 0; i < 1000000; i++) {
        //    dataSourceDist.add(str);
        //}
        //for (String data : dataSourceDist) {
        //parse = JSON.parseObject(data);

        List<String> resMessages = new ArrayList<>(); //返回消息体
        long Tstart = System.currentTimeMillis();
        String offset_key = ""; //namespace + dbname
        String offset_value; //处理日期 + 批处理当前下标
        int batch_index = 0;
        while (rs.next()) {
            System.out.println(batch_index);
            offset_value = String.format("%s_%s", pop.getProperty("cache.logs.dealWithTime"), batch_index);
            batch_index++;
            JSONObject parse = null;
            try{
                //1.获取得到对应数据部分、global名称
                parse = JSON.parseObject(rs.getString(2));
                if(parse == null){
//                    saveErrInfo(new ParseLogs(offset_value, 2),
//                            new ParseLogDetails(String.format("当前数据解析JSON失败: %s", rs.getString(2))));
                    throw new RuntimeException(ErrorInfo.getErrDesc(ErrorInfo.PARSE_JSON_ERROR));
                }
                logger.info("正在处理数据: {}.", parse.toJSONString());

                //2.获取配置项
                String dbName = parse.getString("DBName");
                String nameSpace = namespaceMap.get(String.format("%s_%s", parse.getString("Glob"), dbName)); //得到namespace
                GlobalManager globManager = allGlobalManager.get(nameSpace); //根据dbName获取相应配置加载类
                if(globManager == null){
//                    saveErrInfo(new ParseLogs(offset_value, 2),
//                            new ParseLogDetails(String.format("未获取到加载类: %s", rs.getString(2))));
                    throw new RuntimeException(ErrorInfo.getErrDesc(ErrorInfo.GLOBMANAGER_NOTFOUND));
                }
                Map<String, Set<String>> allGlobalNodeOfClass = globManager.getAllGlobalNodeOfClass();
                Map<String, List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>>> allGlobalNodeStorage = globManager.getAllGlobalNodeStorage();
                Map<String, Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable>> allClassInfo = globManager.getAllClassInfo();

                //3.处理log中global内容
                Object[] objects = getGlobalDefInfos(parse); //格式化globalName
                if(objects == null) {
                    //logger.warn("当前global格式异常:{}", parse.getString("GlobalReference"));
                    continue;
                }
                String globParseName = (String) objects[0];
                String[] logExpressArr = (String[]) objects[1]; //expression节点集

                //TODO 本地缓存当前日志数据
                //localCacheDataMap.put(globAlias, parse.toJSONString());

                //4.剃除非处理数据
                if (!allGlobalNodeStorage.containsKey(globParseName)) {
                    continue;
                }

                //TODO 当前处理记录落档

                //5.开始处理数据
                List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>> tuple2s = allGlobalNodeStorage.get(globParseName); //根据globalParseName得到节点、属性二元组集合
                for (Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>> tuple2 : tuple2s) { //定位当前global对应property
                    if (compareGlobalNodeInfo(logExpressArr, tuple2._1)) { //global匹配上了
                        String globName = parse.getString("Glob");
                        offset_key = String.format("%s_%s", nameSpace, dbName);
                        String globalAlias = getGlobalAlias(globName, tuple2._1); //组装全称
                        List<ClassPropertyDefine> classPropDef = tuple2._2;
                        String className = classPropDef.get(0).getClassName(); //得到当前global对应类
                        Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefTabTuple4 = allClassInfo.get(className);
                        if(classDefTabTuple4 == null){ //未匹配到类下属性，不作处理
                            logger.warn("当前global:{}对应class:{}未匹配到属性信息，不作处理", parse.getString("Glob"), className);
                            saveErrInfo(new ParseLogs(offset_value, 1), new ParseLogDetails(
                                    String.format("当前global: %s对应class: %s未匹配到属性信息，不作处理", parse.getString("Glob"), className)));
                            continue;
                        }

                        String rowidName = classDefTabTuple4._2().get(0).getSqlRowidName(); //rowidName
                        Map<String, String> idsMap = buildRowId(logExpressArr, tuple2._1, classDefTabTuple4);//构建rowId、parentId、childsubId
                        List<String> allGlobDefs = new ArrayList<>(allGlobalNodeOfClass.get(className)); //得到类对应所有global定义
                        Map<String, String> globMap = buildGlobal(logExpressArr, tuple2._1, allGlobDefs); //替换变量为真实值
                        DataTable dataTable = classDefTabTuple4._4(); //得到数据表信息
                        if(dataTable == null){
                            logger.warn("当前global:{}未匹配到datatable信息,不作处理", parse.getString("Glob"));
                            saveErrInfo(new ParseLogs(offset_value, 1), new ParseLogDetails(
                                    String.format("当前global: %s未匹配到datatable信息,不作处理", parse.getString("Glob"))));
                            continue;
                        }

                        String schemaName = dataTable.getSchemaName(); // schemaName
                        String tableName = dataTable.getTableName(); // 表名
                        String[] paramArr = new String[]{schemaName, tableName, globalAlias, className, nameSpace, rowidName}; //参数
                        String message;
                        if (allGlobDefs.size() > 1) { //多个global批处理
                            message = buildMsgByMultpGlob(globMap, parse, idsMap, globManager, paramArr);
                            resMessages.add(message);
                            //kafkaSink.sendMessage(pop.getProperty("cache.topic"), 1, message, dbName); //发送kafka
                        } else { //单个global赋值此条数据
                            message = buildMsgBySiglrGlob(allGlobDefs.get(0), parse, idsMap, globManager, paramArr);
                            resMessages.add(message);
                            //kafkaSink.sendMessage(pop.getProperty("cache.topic"), 1, message, dbName); //发送kafka
                        }
                    }
                }
            }catch (Exception e){
                logger.error("{} {}.", e.getMessage(), rs.getString(2));
                saveErrInfo(new ParseLogs(offset_value, 2), new ParseLogDetails(
                        String.format("当前数据还原过程出错: %s : %s", rs.getString(2), e.getMessage())));
                //e.printStackTrace();
                continue;
            }
            offsetMap.put(offset_key, offset_value); //批处理下标记录
        }
        System.out.println(String.format(String.format("处理时长总计: %s", System.currentTimeMillis() - Tstart)));
        return resMessages;
    }


    /**
     * 构建多global节点存储类型的返回消息体
     * @param parse
     * @param paramArr
     * @return
     */
    private static String buildMsgByMultpGlob(Map<String, String> globMap, JSONObject parse, Map<String, String> idsMap,
                                              GlobalManager globManager, String[] paramArr){
        /**
         * 类定义参数
         */
        String schemaName = paramArr[0];
        String tableName = paramArr[1];
        String globalAlias = paramArr[2];
        String className = paramArr[3];
        String nameSpace = paramArr[4];

        /**
         * ogg消息格式参数
         */
        String op_type = parse.getString("RecordType");
        String pos = parse.getString("Address");
        String op_ts = parse.getString("Timestamp");
        String primary_keys = paramArr[5];

        String namespaceFullName = String.format("cache.his.%s.%s.%s", nameSpace, schemaName, tableName); //拼接namespace

        MessageBuilder afterBuilder = new MessageBuilder(); //处理after
        afterBuilder.build(Message.ProtocolType.DATA_INCREMENT_DATA, namespaceFullName, 3);
        List<Object> afterRowDataValues = new ArrayList<>();

        MessageBuilder beforeBuilder = new MessageBuilder(); //处理before
        beforeBuilder.build(Message.ProtocolType.DATA_INCREMENT_DATA, namespaceFullName, 3);
        List<Object> beforeRowDataValues = new ArrayList<>();

        List<String> queryGlobs = new ArrayList<>();
        Map<String, JSONObject> localGlobAndDataMap = new HashMap<>();
        for (Map.Entry<String, String> glob : globMap.entrySet()) {//迭代glob定义、global全称
            JSONObject parseObj = null;
            if(StringUtils.equals(glob.getKey(), globalAlias)) { //当前数据(global定义相同)
                parseObj = parse;
                localGlobAndDataMap.put(glob.getKey(), parseObj);
            }else { //TODO 非当前数据，默认先从本地缓存查询，否则接口调用查取
                //String parseObjStr = localCacheDataMap.get(glob.getKey());
                //if(parseObjStr != null)
                //    localGlobAndDataMap.put(glob.getKey(), JSONObject.parseObject(parseObjStr));
                //else //本地未查到
                    queryGlobs.add(glob.getValue()); //添加进待查集合
            }
        }

        //反查数据、批处理
        String globalnode;
        JSONArray after;
        JSONArray before;
        String globDef = "";
        try{
            if(localGlobAndDataMap.size() > 0){ //先处理当前或本地获取的数据
                for (Map.Entry<String, JSONObject> globAndJSobj : localGlobAndDataMap.entrySet()) {
                    globDef = globAndJSobj.getKey();
                    after = globAndJSobj.getValue().getJSONArray("After") == null ? new JSONArray() : globAndJSobj.getValue().getJSONArray("After");
                    before = globAndJSobj.getValue().getJSONArray("Before") == null ? new JSONArray() : globAndJSobj.getValue().getJSONArray("Before");
                    buildMSG(globDef, after, before, afterRowDataValues, beforeRowDataValues, afterBuilder,
                            beforeBuilder, tableName, globManager); //构建数据部分
                }
            }

            List<JSONObject> QueryJsonObjList = queryLog(nameSpace, className, queryGlobs);

            if(QueryJsonObjList.size() > 0){ //后处理经查询的数据
                for (JSONObject jsonObject : QueryJsonObjList) {
                    globalnode = jsonObject.getString("globalnode");
                    globDef = getGlobDefByComparable(globMap, globalnode, nameSpace); //由返回数据匹配对应globDef
                    after = jsonObject.getJSONArray("globalValue");
                    //before = after; //before取after??
                    buildMSG(globDef, after, after, afterRowDataValues, beforeRowDataValues, afterBuilder,
                            beforeBuilder, tableName, globManager); //构建数据部分
                }
            }
        }catch (Exception e){
            logger.error("反查cache数据出错className{}. {}.", className, e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(String.format("反查cache数据出错className:%s. %s.", className, e.getMessage()));
        }

        //追加rowId、parentId、childsubId
        for (Map.Entry<String, String> id : idsMap.entrySet()) {
            afterBuilder.appendSchema(id.getKey(), DataType.VARCHAR, false, false);
            afterRowDataValues.add(id.getValue());
            beforeBuilder.appendSchema(id.getKey(), DataType.VARCHAR, false, false);
            beforeRowDataValues.add(id.getValue());
        }

        afterBuilder.appendPayload(afterRowDataValues.toArray());
        beforeBuilder.appendPayload(beforeRowDataValues.toArray());

        //return parseMsg(afterBuilder.message, beforeBuilder.message);
        return parseOggMsg(afterBuilder.message, beforeBuilder.message, op_type, pos, op_ts, primary_keys);
    }


    /**
     * 构建单global节点存储类型的返回消息体
     * @param parse
     * @return
     */
    private static String buildMsgBySiglrGlob(String global, JSONObject parse, Map<String, String> idsMap, GlobalManager globManager, String[] paramArr) {

        /**
         * 类定义参数
         */
        String schemaName = paramArr[0];
        String tableName = paramArr[1];
        String nameSpace = paramArr[4];

        /**
         * ogg消息格式参数
         */
        String op_type = parse.getString("RecordType");
        String pos = parse.getString("Address");
        String op_ts = parse.getString("Timestamp");
        String primary_keys = paramArr[5];

        String namespaceFullName = String.format("cache.his.%s.%s.%s", nameSpace, schemaName, tableName); //拼接namespace

        MessageBuilder afterBuilder = new MessageBuilder();
        afterBuilder.build(Message.ProtocolType.DATA_INCREMENT_DATA, namespaceFullName, 3);
        List<Object> afterRowDataValues = new ArrayList<>();

        MessageBuilder beforeBuilder = new MessageBuilder();
        beforeBuilder.build(Message.ProtocolType.DATA_INCREMENT_DATA, namespaceFullName, 3);
        List<Object> beforeRowDataValues = new ArrayList<>();

        JSONArray after = parse.getJSONArray("After") == null ? new JSONArray() : parse.getJSONArray("After");
        JSONArray before = parse.getJSONArray("Before") == null ? new JSONArray() : parse.getJSONArray("Before");

        buildMSG(global, after, before, afterRowDataValues, beforeRowDataValues, afterBuilder,
                beforeBuilder, tableName, globManager);//构建数据部分

        //追加rowId、parentId、childsubId
        for (Map.Entry<String, String> id : idsMap.entrySet()) {
            afterBuilder.appendSchema(id.getKey(), DataType.VARCHAR, false, false);
            afterRowDataValues.add(id.getValue());
            beforeBuilder.appendSchema(id.getKey(), DataType.VARCHAR, false, false);
            beforeRowDataValues.add(id.getValue());
        }

        afterBuilder.appendPayload(afterRowDataValues.toArray());
        beforeBuilder.appendPayload(beforeRowDataValues.toArray());

        //return parseMsg(afterBuilder.message, beforeBuilder.message);
        return parseOggMsg(afterBuilder.message, beforeBuilder.message, op_type, pos, op_ts, primary_keys);
    }


    /**
     * 根据单个global构建消息体
     * @param globalDef
     */
    private static void buildMSG(String globalDef, JSONArray after, JSONArray before, List<Object> afterRowDataValues, List<Object> beforeRowDataValues,
                                 MessageBuilder afterBuilder, MessageBuilder beforeBuilder, String tableName, GlobalManager globManager) {

        //当前global对应属性
        List<ClassPropertyDefine> fields = globManager.getAllGlobalNodeProperty().get(globalDef);
        int columanLength = fields.size(); //整表字段个长
        int afterLength = after.size(); //after数据个长
        int beforeLength = before.size(); //before数据个长

        for (int i = 0; i < columanLength; i++) { //根据字段循环(value有可能少、漏)

            ClassPropertyDefine classPropDef = fields.get(i); //当前属性信息

            //after处理
            if(i < afterLength){ //有值则加载after对应数据
                buidMsgNotEmpty(classPropDef, after, tableName, afterRowDataValues, afterBuilder, globManager);
            }else { //否则默认给空值补上
                buidMsgEmpty(classPropDef, tableName, afterRowDataValues, afterBuilder, globManager);
            }

            //before处理
            if(i < beforeLength) { //有值则加载before对应数据
                buidMsgNotEmpty(classPropDef, before, tableName, beforeRowDataValues, beforeBuilder, globManager);
            }else { //否则默认给空值补上
                buidMsgEmpty(classPropDef, tableName, beforeRowDataValues, beforeBuilder, globManager);
            }
        }
    }


    /**
     * 有值情况构建消息体
     * @param classPropDef
     * @param valueArr
     * @param rowDataValues
     * @param builder
     */
    private static void buidMsgNotEmpty(ClassPropertyDefine classPropDef, JSONArray valueArr, String tableName,
                                        List<Object> rowDataValues, MessageBuilder builder, GlobalManager globManager){

        int storePiece = Integer.parseInt(classPropDef.getStoragePiece()) - 1; //属性下标, 按属性下标-1取数据值
        String colName = classPropDef.getSqlFieldName(); //columnName
        String colType = "varchar"; //columnType
        String table_field = tableName + "_" + colName; //findTypeKey
        Map<String, TableMeta> allTableMeta = globManager.getAllTableMeta(); //allTableMeta

        if(StringUtils.equalsIgnoreCase(classPropDef.getPropertyCollection(), "list")){ //属性为list类型则作list对应格式拼接
            colType = allTableMeta.get(table_field).getDataType(); //columnType
            if(valueArr.get(storePiece) instanceof String){
                rowDataValues.add("null");
                builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
            }else {
                JSONArray sub_ListArr = valueArr.getJSONArray(storePiece);
                StringBuilder sb = new StringBuilder();
                for (Object o : sub_ListArr) {
                    if(StringUtils.isEmpty(classPropDef.getSqlListDelimiter()))
                        sb.append(o).append(","); //list类型对应数据拼接默认分隔符为","
                    else
                        sb.append(o).append(classPropDef.getSqlListDelimiter());
                }
                String sub_ListValue = sb.delete(sb.lastIndexOf(","), sb.length()).toString();
                rowDataValues.add(sub_ListValue);
                builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
            }
        }else if(!StringUtils.startsWith(classPropDef.getRuntimeType(), "%")){ //属性为对象类型则找子属性
            //根据对象属性找到其子属性
            Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefDataTabTup4
                    = globManager.getAllClassInfo().get(classPropDef.getRuntimeType());
            if(StringUtils.equals(classDefDataTabTup4._1().getClassType(), "serial")){ //序列华类型, 需展开,字段拼接
                List<ClassPropertyDefine> subSeriClassPropDef = classDefDataTabTup4._3(); //类对应属性
                String sub_colName;
                String sub_colType;
                String sub_colValue;
                JSONArray subSeriJsonArr = valueArr.getJSONArray(storePiece); //属性类对应数据
                for (int j = 0; j < subSeriClassPropDef.size(); j++) { //属性对象赋值
                    if(StringUtils.startsWith(subSeriClassPropDef.get(j).getPropertyName(), "%") &&
                            StringUtils.isEmpty(subSeriClassPropDef.get(j).getStoragePiece())){
                        continue;
                    }
                    int sub_storePiece = Integer.parseInt(subSeriClassPropDef.get(j).getStoragePiece()) - 1; //属性类子属性下标
                    sub_colName = String.format("%s_%s", colName, subSeriClassPropDef.get(j).getSqlFieldName());
                    sub_colType = allTableMeta.get(String.format("%s_%s", table_field, subSeriClassPropDef.get(j).getSqlFieldName())).getDataType();
                    sub_colValue = subSeriJsonArr.getString(sub_storePiece);

                    rowDataValues.add(sub_colValue);
                    builder.appendSchema(sub_colName, DataType.convertCacheToHiveDataType(sub_colType), false, false); //columnName
                }
            }else { //持久类型
                rowDataValues.add(valueArr.getString(storePiece));
                builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
            }
        }else { //一般属性
            colType = allTableMeta.get(table_field).getDataType(); //columnType
            rowDataValues.add(valueArr.getString(storePiece)); //columnValue
            builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
        }
    }


    /**
     * 空值情况构建消息体
     * @param classPropDef
     * @param rowDataValues
     * @param builder
     */
    private static void buidMsgEmpty(ClassPropertyDefine classPropDef, String tableName, List<Object> rowDataValues, MessageBuilder builder, GlobalManager globManager){
        String colName = classPropDef.getSqlFieldName(); //columnName
        String colType = "varchar"; //columnType
        String table_field = tableName + "_" + colName; //findTypeKey
        Map<String, TableMeta> allTableMeta = globManager.getAllTableMeta(); //allTableMeta

        if(!StringUtils.startsWith(classPropDef.getRuntimeType(), "%")){ //属性为对象类型则找子属性
            //根据对象属性找到其子属性
            Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefDataTabTup4 =
                    globManager.getAllClassInfo().get(classPropDef.getRuntimeType());
            if(StringUtils.equals(classDefDataTabTup4._1().getClassType(), "serial")){ //序列华类型, 需展开,字段拼接
                List<ClassPropertyDefine> subSeriClassPropDef = classDefDataTabTup4._3(); //类对应属性
                String sub_colName;
                String sub_colType;
                for (int j = 0; j < subSeriClassPropDef.size(); j++) {
                    if(StringUtils.startsWith(subSeriClassPropDef.get(j).getPropertyName(), "%") &&
                            StringUtils.isEmpty(subSeriClassPropDef.get(j).getStoragePiece())){
                        continue;
                    }

                    sub_colName = String.format("%s_%s", colName, subSeriClassPropDef.get(j).getSqlFieldName());
                    sub_colType = allTableMeta.get(String.format("%s_%s", table_field, subSeriClassPropDef.get(j).getSqlFieldName())).getDataType();

                    rowDataValues.add("null");
                    builder.appendSchema(sub_colName, DataType.convertCacheToHiveDataType(sub_colType), false, false); //columnName
                }
            }else { //持久类型
                rowDataValues.add("null");
                builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
            }
        }else { //一般属性
            rowDataValues.add("null");
            builder.appendSchema(colName, DataType.convertCacheToHiveDataType(colType), false, false); //columnName
        }
    }


    /**
     * 构建global全称(变量替换为真实值)
     * @param allGlobalDefs
     * @return
     */
    private static Map<String, String> buildGlobal(String[] logExpressions, List<StorageSubscriptDefine> subscriptDef, List<String> allGlobalDefs) {

        Map<String, String> globMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        for (String globalWholeName : allGlobalDefs) {
            int index = globalWholeName.indexOf("(");
            sb.append(globalWholeName.substring(0, index));
            String[] fields = globalWholeName.substring(index + 1, globalWholeName.indexOf(")")).split(",");
            for (int i = 0; i < subscriptDef.size(); i++) {
                if (1 == subscriptDef.get(i).getIsRowid()) {
                    fields[i] = logExpressions[i];
                }
            }

            sb.append("(");
            sb.append(StringUtils.join(fields, ","));
            sb.append(")");
            globMap.put(globalWholeName, sb.toString());
            sb.delete(0, sb.length());
        }

        return globMap;
    }


    /**
     * 反查cache数据
     * @param namespace
     * @param className
     * @param globals
     * @return
     * @throws Exception
     */
    private static List<JSONObject> queryLog(String namespace, String className, List<String> globals) throws CacheException {

        Database database = null;
        if(cacheQueryConnMap.containsKey(namespace)) { //已存入直接取
            database = cacheQueryConnMap.get(namespace);
        }else {
            String url = pop.getProperty("cache.url");
            String username = pop.getProperty("cache.base.username");
            String password = pop.getProperty("cache.base.password");
            database = CacheDatabase.getDatabase(url, username, password);
            cacheQueryConnMap.put(namespace, database);
        }

        JSONArray array = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("table", className);
        JSONArray globalArray = new JSONArray();
        globals.stream().forEach(g -> {globalArray.add("^|\"" + namespace + "\"|"
                + g.substring(g.indexOf("^") + 1));});
        jsonObject.put("item", globalArray);
        array.add(jsonObject);

        String queryParam = array.toJSONString();
        //logger.info("反查queryParam: {}.", queryParam);

        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(queryParam);
        Dataholder res = database.runClassMethod("ClinBrain.CDCUtil", "QueryGlobals", args, RET_PRIM);
        List<JSONObject> dataParses = new ArrayList<>();
            //获取数据部分
            JSONArray dataArr = JSONArray.parseArray(res.getString()).getJSONObject(0).getJSONArray("data");
            for (int i = 0; i < dataArr.size() ; i++) {
                if(dataArr.getJSONObject(i).getJSONArray("globalValue").size() != 0){
                    dataParses.add(dataArr.getJSONObject(i));
                }
            }
        return dataParses;
    }


    /**
     * 构建普通返回格式
     * @param afterMsg
     * @param beforeMsg
     * @return
     */
    private static String parseMsg(Message afterMsg, Message beforeMsg) {
        JSONObject resObj = new JSONObject();
        JSONObject afterObj = JSONObject.parseObject(afterMsg.toString());
        JSONObject beforeObj = JSONObject.parseObject(beforeMsg.toString());

        resObj.put("protocol", afterObj.get("protocol"));
        resObj.put("schema", afterObj.get("schema"));

        JSONObject PlayObj = new JSONObject();
        PlayObj.put("after", afterObj.getJSONArray("payload").getJSONObject(0).get("tuple"));
        PlayObj.put("before", beforeObj.getJSONArray("payload").getJSONObject(0).get("tuple"));

        resObj.put("payload", PlayObj);

        return resObj.toJSONString();
    }


    /**
     * 构建OGG消息返回格式
     * @param afterMsg
     * @param beforeMsg
     * @return
     */
    private static String parseOggMsg(Message afterMsg, Message beforeMsg,String op_type, String pos, String op_ts, String primary_keys) {
        JSONObject resObj = new JSONObject();
        JSONObject afterResObj = JSONObject.parseObject(afterMsg.toString());
        JSONObject beforeResObj = JSONObject.parseObject(beforeMsg.toString());

        resObj.put("table", afterResObj.getJSONObject("schema").getString("namespace"));
        resObj.put("op_type", op_type);
        resObj.put("op_ts", op_ts);
        resObj.put("current_ts", System.currentTimeMillis());
        resObj.put("pos", pos);
        resObj.put("primary_keys", new JSONArray().add(primary_keys));

        JSONObject afterData = new JSONObject();
        JSONObject beforeData = new JSONObject();
        JSONArray afterFields = afterResObj.getJSONObject("schema").getJSONArray("fields");
        JSONArray afterValues = afterResObj.getJSONArray("payload").getJSONObject(0).getJSONArray("tuple");
        JSONArray beforeValues = beforeResObj.getJSONArray("payload").getJSONObject(0).getJSONArray("tuple");

        for (int i = 0; i < afterFields.size(); i++) {
            afterData.put(afterFields.getJSONObject(i).getString("name"), afterValues.getString(i));
            beforeData.put(afterFields.getJSONObject(i).getString("name"), beforeValues.getString(i)); //before直接取after字段
        }

        resObj.put("after", afterData.toJSONString());
        resObj.put("before", beforeData.toJSONString());

        return resObj.toJSONString();
    }


    /**
     * 根据返回内容匹配globDef
     * @param globMap
     * @param globalnode
     * @param namespace
     * @return
     */
    private static String getGlobDefByComparable(Map<String, String> globMap, String globalnode, String namespace) {
        for (Map.Entry<String, String> glob : globMap.entrySet()) {
            String globNodeStr = "^|\"" + namespace + "\"|" + glob.getValue().substring(glob.getValue().indexOf("^") + 1);
            if(StringUtils.equals(globalnode, globNodeStr)){
                return glob.getKey();
            }
        }
        return null;
    }


    /**
     * 获取rowId
     * @param logExpressArr
     * @param storageSubscriptDefines
     * @return
     */
    private static Map<String, String> buildRowId(String[] logExpressArr, List<StorageSubscriptDefine> storageSubscriptDefines,
                                                  Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefTabTuple4) {

        ClassStorageDefine classStogeDef = classDefTabTuple4._2().get(0);
        List<ClassPropertyDefine> classPopDef = classDefTabTuple4._3();

        StringBuilder sb = new StringBuilder();
        Map<String, String> idMap = new HashMap<>();
        for (int i = 0; i < logExpressArr.length; i++) {
            if(storageSubscriptDefines.get(i).getIsRowid() == 1){
                sb.append(logExpressArr[ i]).append("||");
            }
        }

        String rowId = sb.delete(sb.lastIndexOf("||"), sb.length()).toString(); //rowId
        idMap.put(classStogeDef.getSqlRowidName(), rowId);

        String parentId = "";
        String childsubId = "";
        if(StringUtils.equalsIgnoreCase("%Library.CacheStorage", classStogeDef.getStorageType())){ //默认存储
            for (ClassPropertyDefine clasPropDef : classPopDef) {
                if(StringUtils.isNotEmpty(clasPropDef.getPropertyCardinality()) &&
                        StringUtils.equalsIgnoreCase("parent", clasPropDef.getPropertyCardinality())){
                    parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                    childsubId = rowId.substring(rowId.lastIndexOf("||") + 2);
                    idMap.put(clasPropDef.getSqlFieldName(), parentId); //parentId
                    idMap.put(classStogeDef.getSqlChildSub(), childsubId); //childsub
                    break;
                }
            }
        }else { //sql存储
            for (ClassPropertyDefine classPropDef : classPopDef) {
                if(StringUtils.isEmpty(classPropDef.getPropertyCardinality())){
                    if(StringUtils.isEmpty(classPropDef.getStorageName()) &&
                            StringUtils.equalsIgnoreCase("N", classPropDef.getPropertyCalculated()) &&
                            !StringUtils.startsWith(classPropDef.getPropertyName(),"%") &&
                            classPropDef.getPropertyName().toLowerCase().contains("childsub")){
                        childsubId = rowId.substring(rowId.lastIndexOf("||") + 2);
                        idMap.put(classPropDef.getSqlFieldName(), childsubId); //childsub
                    }else {
                        if(StringUtils.equalsIgnoreCase("parent", classPropDef.getPropertyCardinality())){
                            parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                            idMap.put(classPropDef.getSqlFieldName(), parentId); //parentId
                        }
                    }
                }
            }
        }
        return idMap;
    }


    /**
     * 下标记录
     */
    public void saveOffset(){
        System.out.println("钩子函数执行啦!!!!!!");
    }


    /**
     * 记录异常数据处理信息
     * @param parseLogs
     * @param logDetails
     */
    public static void saveErrInfo(ParseLogs parseLogs, ParseLogDetails logDetails) {
        String log_id = UUID.randomUUID().toString();
        parseLogs.setLogId(log_id);
        logDetails.setLogsId(log_id);
        parseLogsMapper.insertSelective(parseLogs);
        parseLogDetailsMapper.insertSelective(logDetails);
    }


    /**
     * 格式化globalName
     * @return
     */
    private static Object[] getGlobalDefInfos(JSONObject parse) {

        String globName = String.format("^%s", parse.getString("Glob")); //globalName
        String globRefNode = String.format("^%s", parse.getString("GlobalReference")); //提取global节点信息

        if(!StringUtils.startsWith(globRefNode,"(") && !StringUtils.endsWith(globRefNode, ")")){//global未含括号
            return null;
        }
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
            if(subscriptDefines.get(i).getIsRowid() == 0 &&
                    !StringUtils.equals(subscriptDefines.get(i).getExpression(), logExpressions[i])){ //配置中的global节点常量
                //两边不匹配时
                flag = false;
                break;
            }
        }
        return flag;
    }

}
