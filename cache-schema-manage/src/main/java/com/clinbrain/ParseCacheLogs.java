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
import com.clinbrain.util.DateTimeUtil;
import com.clinbrain.util.EncodeUtil;
import com.clinbrain.util.ErrorInfo;
import com.clinbrain.util.PopLoadUtils;
import com.clinbrain.util.exception.ConfigLoadException;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.intersys.objects.Database.RET_PRIM;

/**
 * @Description cache实时日志还原
 * @Author xhy
 * @Date 2020/4/8 10:39
 */
public class ParseCacheLogs {

    private static final Logger logger = LoggerFactory.getLogger(ParseCacheLogs.class);

    private static Properties pop;
    //private static KafkaSink kafkaSink;
    private static Map<String, GlobalManager> allGlobalManager;
    private static Map<String, List<String>> namespaceMap;
    private static Map<String, Database> cacheQueryConnMap = new HashMap<>();
    private static Map<String, String> localCacheDataMap = new HashMap<>(); //本地缓存日志
    private static Set<String> globalNames;
    private static SqlSession sqlSession;
    private static ParseLogsMapper parseLogsMapper;
    private static ParseLogDetailsMapper parseLogDetailsMapper;

    static{
        try{
            pop = PopLoadUtils.loadProperties(null);
            //kafkaSink = new KafkaSink(); //加载kafka对象
            allGlobalManager = GlobalManager.buildGlobalManager();
            namespaceMap = GlobalManager.buildGlobalDbMapNamespace(pop.getProperty("cache.glob.url"), pop.getProperty("cache.glob.username"),
                    pop.getProperty("cache.glob.password"));
            globalNames = GlobalManager.buildAllGlobalName();
            InputStream resourceAsStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
            SqlSessionFactory build = new SqlSessionFactoryBuilder().build(resourceAsStream);
            sqlSession = build.openSession();
            parseLogsMapper = sqlSession.getMapper(ParseLogsMapper.class);
            parseLogDetailsMapper = sqlSession.getMapper(ParseLogDetailsMapper.class);
        }catch (Exception e){
            logger.error("实时还原,加载元数据发生错误: {}.", e.getMessage());
            throw new RuntimeException(String.format("实时还原,加载元数据发生错误: %s", e.getMessage()));
        }
    }


    /**
     * deal cachelogs
     * @return
     */
    public static String[] startDealWithLogs(String logData) throws Exception{

        String message = null;
        String[] resultArr = new String[2];
        //1.获取得到对应数据部分、global名称
        JSONObject parse = JSON.parseObject(logData);
        if(parse == null){
            throw new IllegalArgumentException(ErrorInfo.getErrDesc(ErrorInfo.PARSE_JSON_ERROR));
        }
        logger.info("正在处理数据: {}.", parse.toJSONString());

        //2.剃除非处理数据
        if (globalNames.contains(String.format("%s%s", "^", parse.getString("Glob")))) {

            //localCacheDataMap.put(globAlias, parse.toJSONString()); //TODO 本地缓存当前日志数据

            //3.处理log中global内容
            Object[] objects = getGlobalDefInfos(parse); //格式化globalName
            if (objects != null) {
                String globParseName = (String) objects[0];
                String[] logExpressArr = (String[]) objects[1]; //expression节点集

                //4.获取配置项
                String dbName = parse.getString("DBName");
                List<String> namespaces = namespaceMap.get(String.format("%s_%s", parse.getString("Glob"), dbName));//得到namespace

                if(namespaces != null) {
                    boolean isNamespaceFlag = false;
                    for (String nameSpace : namespaces) { //匹配日志对应namespace
                        if (isNamespaceFlag) //配置只允许配置一个对应namespace, 找到处理即跳出
                            break;

                        GlobalManager globManager = allGlobalManager.get(nameSpace); //根据dbName获取相应配置加载类
                        if (globManager == null)
                            throw new ConfigLoadException(ErrorInfo.getErrDesc(ErrorInfo.GLOBMANAGER_NOTFOUND));

                        Map<String, List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>>> allGlobalNodeStorage = globManager.getAllGlobalNodeStorage();
                        Map<String, Set<String>> allGlobalNodeOfClass = globManager.getAllGlobalNodeOfClass();
                        Map<String, Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable>> allClassInfo = globManager.getAllClassInfo();

                        //5.开始处理数据
                        List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>> tuple2s = allGlobalNodeStorage.get(globParseName); //根据globalParseName得到节点、属性二元组
                        for (Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>> tuple2 : tuple2s) { //定位当前global对应property
                            if (compareGlobalNodeInfo(logExpressArr, tuple2._1)) { //global匹配上了
                                isNamespaceFlag = true;
                                String globName = parse.getString("Glob");
                                String globalAlias = getGlobalAlias(globName, tuple2._1); //组装全称
                                List<ClassPropertyDefine> classPropDef = tuple2._2;
                                String className = classPropDef.get(0).getClassName(); //得到当前global对应类
                                Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> clsDefTabTup4 = allClassInfo.get(className);
                                if (clsDefTabTup4 == null) { //未匹配到类下属性，不作处理
                                    logger.warn("当前global:{}对应class:{}未匹配到属性信息，不作处理", parse.getString("Glob"), className);
                                    //saveErrInfo(new ParseLogs(offset_key, offset_value, 1), new ParseLogDetails(
                                    //          String.format("当前global: %s对应class: %s未匹配到属性信息，不作处理", parse.getString("Glob"), className)));
                                    continue;
                                }

                                String rowidName = clsDefTabTup4._2().get(0).getSqlRowidName(); //rowidName
                                Map<String, String> idsMap = buildRowId(logExpressArr, tuple2._1, clsDefTabTup4); //构建rowId、parentId、childsubId
                                List<String> allGlobDefs = new ArrayList<>(allGlobalNodeOfClass.get(className)); //得到类对应所有global定义
                                Map<String, String> globMap = buildGlobal(logExpressArr, tuple2._1, allGlobDefs); //替换变量为真实值
                                DataTable dataTable = clsDefTabTup4._4(); //得到数据表信息
                                if (dataTable == null) {
                                    logger.warn("当前global:{}未匹配到datatable信息,不作处理", parse.getString("Glob"));
                                    //saveErrInfo(new ParseLogs(offset_key, offset_value, 1), new ParseLogDetails(
                                    //          String.format("当前global: %s未匹配到datatable信息,不作处理", parse.getString("Glob"))));
                                    continue;
                                }

                                String schemaName = dataTable.getSchemaName(); // schemaName
                                String tableName = dataTable.getTableName(); // 表名
                                String[] paramArr = new String[]{schemaName, tableName, globalAlias, className, nameSpace, rowidName}; //参数

                                if (allGlobDefs.size() > 1) { //多个global批处理
                                    message = buildMsgByMultpGlob(globMap, parse, idsMap, globManager, paramArr);
                                    //kafkaSink.sendMessage(pop.getProperty("cache.topic"), 1, message, dbName); //发送kafka
                                } else { //单个global赋值此条数据
                                    message = buildMsgBySiglrGlob(allGlobDefs.get(0), parse, idsMap, globManager, paramArr);
                                    //kafkaSink.sendMessage(pop.getProperty("cache.topic"), 1, message, dbName); //发送kafka
                                }

                                //返回体
                                String offset_key = String.format("%s", dbName); //日志记录key值
                                resultArr[0] = offset_key;
                                resultArr[1] = message;
                                break;
                            }
                        }
                    }
                }else {
                    //logger.warn("为获取到namespace异常");
                }
            }else {
                //logger.warn("当前global格式异常:{}", parse.getString("GlobalReference"));
            }
        }
        return resultArr;
    }


    /**
     * 构建多global节点存储类型的返回消息体
     * @param parse
     * @param paramArr
     * @return
     */
    private static String buildMsgByMultpGlob(Map<String, String> globMap, JSONObject parse, Map<String, String> idsMap,
                                              GlobalManager globManager, String[] paramArr) throws Exception{
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
                            beforeBuilder, nameSpace, tableName, globManager); //构建数据部分
                }
            }

            List<JSONObject> QueryJsonObjList = getLogsByQuery(nameSpace, className, queryGlobs);

            if(QueryJsonObjList.size() > 0){ //后处理经查询的数据
                for (JSONObject jsonObject : QueryJsonObjList) {
                    globalnode = jsonObject.getString("globalnode");
                    globDef = getGlobDefByComparable(globMap, globalnode, nameSpace); //由返回数据匹配对应globDef
                    after = jsonObject.getJSONArray("globalValue");
                    //before = after; //before取after??
                    buildMSG(globDef, after, after, afterRowDataValues, beforeRowDataValues, afterBuilder,
                            beforeBuilder, nameSpace, tableName, globManager); //构建数据部分
                }
            }
        }catch (Exception e){
            logger.error("反查cache数据出错className{}. {}.", className, e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(String.format("反查cache数据出错className:%s. %s.", className, e.getMessage()));
        }

        //追加tableMeta差集字段, 追加rowId、parentId、childsubId
        appendOtherColInfo(afterBuilder, beforeBuilder, afterRowDataValues, beforeRowDataValues, globManager, idsMap, tableName);

        //return parseMsg(afterBuilder.message, beforeBuilder.message);
        return parseOggMsg(afterBuilder.message, beforeBuilder.message, op_type, pos, op_ts, primary_keys);
    }


    /**
     * 构建单global节点存储类型的返回消息体
     * @param parse
     * @return
     */
    private static String buildMsgBySiglrGlob(String global, JSONObject parse, Map<String, String> idsMap, GlobalManager globManager,
                                              String[] paramArr) throws Exception{
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
                beforeBuilder, nameSpace, tableName, globManager);//构建数据部分

        //追加tableMeta差集字段,追加rowId、parentId、childsubId
        appendOtherColInfo(afterBuilder, beforeBuilder, afterRowDataValues, beforeRowDataValues, globManager, idsMap, tableName);

        //return parseMsg(afterBuilder.message, beforeBuilder.message);
        return parseOggMsg(afterBuilder.message, beforeBuilder.message, op_type, pos, op_ts, primary_keys);
    }


    /**
     * 根据单个global构建消息体
     * @param globalDef
     */
    private static void buildMSG(String globalDef, JSONArray after, JSONArray before, List<Object> afterRowDataValues, List<Object> beforeRowDataValues,
                                 MessageBuilder afterBuilder, MessageBuilder beforeBuilder, String nameSpace, String tableName, GlobalManager globManager) throws Exception{
        //当前global对应属性
        List<ClassPropertyDefine> fields = globManager.getAllGlobalNodeProperty().get(globalDef);
        int columanLength = fields.size(); //整表字段个长
        int afterLength = after.size(); //after数据个长
        int beforeLength = before.size(); //before数据个长

        for (int i = 0; i < columanLength; i++) { //根据字段循环(value有可能少、漏)
            ClassPropertyDefine clsPropDef = fields.get(i); //当前属性信息
            //after处理
            if(i < afterLength) //有值则加载after对应数据
                buidMsgNotEmpty(clsPropDef, after, nameSpace, tableName, afterRowDataValues, afterBuilder, globManager);
            else  //否则默认给空值补上
                buidMsgEmpty(clsPropDef, tableName, afterRowDataValues, afterBuilder, globManager);

            //before处理
            if(i < beforeLength)  //有值则加载before对应数据
                buidMsgNotEmpty(clsPropDef, before, nameSpace, tableName, beforeRowDataValues, beforeBuilder, globManager);
            else  //否则默认给空值补上
                buidMsgEmpty(clsPropDef, tableName, beforeRowDataValues, beforeBuilder, globManager);
        }
    }


    /**
     * 有值情况构建消息体
     * @param clsPropDef
     * @param valueArr
     * @param rowDataValues
     * @param builder
     */
    private static void buidMsgNotEmpty(ClassPropertyDefine clsPropDef, JSONArray valueArr, String nameSpace, String tableName,
                                        List<Object> rowDataValues, MessageBuilder builder, GlobalManager globManager) throws Exception{

        int storePiece = Integer.parseInt(clsPropDef.getStoragePiece()) - 1; //属性下标, 按属性下标-1取数据值
        String colName = clsPropDef.getSqlFieldName(); //columnName
        String colType = "varchar"; //columnType 默认varchar
        Map<String, TableMeta> allTableMeta = globManager.getAllTableMeta().get(tableName); //allTableMeta

        if(StringUtils.equalsIgnoreCase(clsPropDef.getPropertyCollection(), "list")){ //属性为list类型则作list对应格式拼接
            colType = allTableMeta.get(colName).getDataType(); //columnType取自tableMeta
            if(valueArr.get(storePiece) instanceof String){
                rowDataValues.add("null");
                builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false); //columnName
            }else {
                JSONArray sub_ListArr = valueArr.getJSONArray(storePiece);
                StringBuilder sb = new StringBuilder();
                for (Object o : sub_ListArr) {
                    if(StringUtils.isEmpty(clsPropDef.getSqlListDelimiter()))
                        sb.append(o).append(","); //list类型对应数据拼接默认分隔符为","
                    else
                        sb.append(o).append(clsPropDef.getSqlListDelimiter());
                }
                String sub_ListValue = EncodeUtil.unicodeToString(sb.delete(sb.lastIndexOf(","), sb.length()).toString()); //转掉Unicode
                rowDataValues.add(sub_ListValue);
                builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false); //columnName
            }
        }else if(!StringUtils.startsWith(clsPropDef.getRuntimeType(), "%")){ //属性为对象类型则找子属性
            //根据对象属性找到其子属性
            Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefDataTabTup4
                    = globManager.getAllClassInfo().get(clsPropDef.getRuntimeType());
            if(StringUtils.equals(classDefDataTabTup4._1().getClassType(), "serial")){ //序列华类型, 需展开,字段拼接
                List<ClassPropertyDefine> subSeriClassPropDef = classDefDataTabTup4._3(); //类对应属性
                String sub_colName;
                JSONArray subSeriJsonArr = valueArr.getJSONArray(storePiece); //属性类对应数据
                for (int j = 0; j < subSeriClassPropDef.size(); j++) { //属性对象赋值
                    if(StringUtils.startsWith(subSeriClassPropDef.get(j).getPropertyName(), "%") &&
                            StringUtils.isEmpty(subSeriClassPropDef.get(j).getStoragePiece())){
                        continue;
                    }
                    ClassPropertyDefine subClsPropDef = subSeriClassPropDef.get(j);
                    sub_colName = String.format("%s_%s", colName, subClsPropDef.getSqlFieldName());
                    Map<String, String> sub_colInfo = evaluationValue(subClsPropDef, sub_colName, subSeriJsonArr, allTableMeta); //获字段取值、类型

                    rowDataValues.add(sub_colInfo.get("colValue"));
                    builder.appendSchema(sub_colName, DataType.convertTypeCacheToHive(sub_colInfo.get("colType")), false, false); //columnName
                }
            }else { //持久类型
                String colvalue = EncodeUtil.unicodeToString(valueArr.getString(storePiece)); //转掉unicode
                rowDataValues.add(colvalue);
                builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false); //columnName
            }
        }else if(StringUtils.equals(clsPropDef.getRuntimeType(), "%Library.GlobalCharacterStream") ||
            StringUtils.equals(clsPropDef.getRuntimeType(), "%Library.GlobalBinaryStream")){ //字段流存储格式
            String colValueAddr = valueArr.getString(storePiece); //存储global地址
            List<String> queryGlobs = new ArrayList<>();
            queryGlobs.add(colValueAddr);
            List<JSONObject> QueryJsonObjList = getLogsByQuery(nameSpace, clsPropDef.getClassName(), queryGlobs); //查找对应数据
            if(QueryJsonObjList.size() > 0){
                JSONObject jsonObject = QueryJsonObjList.get(0);
                String colvalue = EncodeUtil.unicodeToString(jsonObject.getJSONArray("globalValue").getString(0)); //流值
                rowDataValues.add(colvalue);
            }else {
                rowDataValues.add("null");
            }
            builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false);
        }else { //一般属性
            Map<String, String> colInfo = evaluationValue(clsPropDef, colName, valueArr, allTableMeta); //获字段取值、类型
            rowDataValues.add(colInfo.get("colValue")); //columnValue
            builder.appendSchema(colName, DataType.convertTypeCacheToHive(colInfo.get("colType")), false, false); //columnName
        }
    }


    /**
     * 空值情况构建消息体
     * @param classPropDef
     * @param rowDataValues
     * @param builder
     */
    private static void buidMsgEmpty(ClassPropertyDefine classPropDef, String tableName, List<Object> rowDataValues, MessageBuilder builder,
                                     GlobalManager globManager) throws Exception{
        String colName = classPropDef.getSqlFieldName(); //columnName
        String colType = "varchar"; //columnType
        String table_field = tableName + "_" + colName; //findTypeKey
        Map<String, Map<String, TableMeta>> allTableMetas = globManager.getAllTableMeta();//allTableMetas
        Map<String, TableMeta> allTableMeta = allTableMetas.get(tableName); //allTableMeta

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
                    builder.appendSchema(sub_colName, DataType.convertTypeCacheToHive(sub_colType), false, false); //columnName
                }
            }else { //持久类型
                rowDataValues.add("null");
                builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false); //columnName
            }
        }else { //一般属性
            rowDataValues.add("null");
            builder.appendSchema(colName, DataType.convertTypeCacheToHive(colType), false, false); //columnName
        }
    }


    /**
     * 构建global全称(变量替换为真实值)
     * @param allGlobalDefs
     * @return
     */
    private static Map<String, String> buildGlobal(String[] logExpressions, List<StorageSubscriptDefine> subscriptDef,
                                                   List<String> allGlobalDefs) throws Exception {

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
    private static List<JSONObject> getLogsByQuery(String namespace, String className, List<String> globals) throws CacheException {

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
     * 追加tableMeta差集字段,追加rowId、parentId、childsubId
     * @param afterBuilder
     * @param beforeBuilder
     * @param afterRowDataValues
     * @param beforeRowDataValues
     * @param globManager
     * @param idsMap
     * @param tableName
     */
    private static void appendOtherColInfo(MessageBuilder afterBuilder, MessageBuilder beforeBuilder, List<Object> afterRowDataValues,
                                           List<Object> beforeRowDataValues, GlobalManager globManager, Map<String, String> idsMap, String tableName) {
        //追加tableMeta多余字段
        Map<String, TableMeta> allTableMeta = globManager.getAllTableMeta().get(tableName);
        List<String> allSqlfield = afterBuilder.getMessage().getSchema().getFields().stream().map(f -> f.getName()).collect(Collectors.toList());
        for(Map.Entry<String, TableMeta> meta : allTableMeta.entrySet()) {
            if(!allSqlfield.contains(meta.getKey())){
                afterBuilder.appendSchema(meta.getKey(), DataType.convertTypeCacheToHive(meta.getValue().getDataType()), false, false);
                afterRowDataValues.add("null");
                beforeBuilder.appendSchema(meta.getKey(), DataType.convertTypeCacheToHive(meta.getValue().getDataType()), false, false);
                beforeRowDataValues.add("null");
            }
        }
        //追加rowId、parentId、childsubId
        for (Map.Entry<String, String> id : idsMap.entrySet()) {
            afterBuilder.appendSchema(id.getKey(), DataType.convertTypeCacheToHive("varchar"), false, false);
            afterRowDataValues.add(id.getValue());
            beforeBuilder.appendSchema(id.getKey(), DataType.convertTypeCacheToHive("varchar"), false, false);
            beforeRowDataValues.add(id.getValue());
        }

        afterBuilder.appendPayload(afterRowDataValues.toArray());
        beforeBuilder.appendPayload(beforeRowDataValues.toArray());
    }


    /**
     * 构建普通返回格式
     * @param afterMsg
     * @param beforeMsg
     * @return
     */
    private static String parseMsg(Message afterMsg, Message beforeMsg) throws Exception{
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
    private static String parseOggMsg(Message afterMsg, Message beforeMsg,String op_type, String pos, String op_ts,
                                      String primary_keys) throws Exception{
        JSONObject resObj = new JSONObject();
        JSONObject afterResObj = JSONObject.parseObject(afterMsg.toString());
        JSONObject beforeResObj = JSONObject.parseObject(beforeMsg.toString());

        resObj.put("table", afterResObj.getJSONObject("schema").getString("namespace"));
        resObj.put("op_type", op_type);
        resObj.put("op_ts", op_ts);
        resObj.put("current_ts", System.currentTimeMillis());
        resObj.put("pos", pos);
        JSONArray primarys = new JSONArray();
        primarys.add(primary_keys);
        resObj.put("primary_keys", primarys);

        JSONObject afterData = new JSONObject();
        JSONObject beforeData = new JSONObject();
        JSONArray afterFields = afterResObj.getJSONObject("schema").getJSONArray("fields");
        JSONArray afterValues = afterResObj.getJSONArray("payload").getJSONObject(0).getJSONArray("tuple");
        JSONArray beforeValues = beforeResObj.getJSONArray("payload").getJSONObject(0).getJSONArray("tuple");

        for (int i = 0; i < afterFields.size(); i++) {
            afterData.put(afterFields.getJSONObject(i).getString("name"), afterValues.getString(i).replaceAll("\\u0000", ""));
            beforeData.put(afterFields.getJSONObject(i).getString("name"), beforeValues.getString(i).replaceAll("\\u0000", "")); //before直接取after字段
        }

        resObj.put("after", afterData);
        resObj.put("before", beforeData);

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
                                                  Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classDefTabTuple4) throws Exception{

        ClassStorageDefine classStogeDef = classDefTabTuple4._2().get(0);
        List<ClassPropertyDefine> classPopDef = classDefTabTuple4._3();

        int rowIdCount = 0;
        StringBuilder sb = new StringBuilder();
        Map<String, String> idMap = new HashMap<>();
        for (int i = 0; i < logExpressArr.length; i++) {
            if(storageSubscriptDefines.get(i).getIsRowid() == 1){
                rowIdCount++;
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
            if (rowIdCount > 1) {
                boolean isGetParent = false;
                for (ClassPropertyDefine classPropDef : classPopDef) {
                    if(StringUtils.isNotEmpty(classPropDef.getPropertyCardinality()) &&
                            StringUtils.equalsIgnoreCase("parent", classPropDef.getPropertyCardinality())){
                        parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                        idMap.put(classPropDef.getSqlFieldName(), parentId); //parentId
                        isGetParent = true;
                        break;
                    }
                }

                if (!isGetParent) {
                    for (StorageSubscriptDefine storageSubscriptDefine : storageSubscriptDefines) {
                        if (storageSubscriptDefine.getIsRowid() == 1) {
                            parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                            idMap.put(storageSubscriptDefine.getExpression()
                                            .replace("{", "")
                                            .replace("}", ""),
                                    parentId); //parentId
                            break;
                        }
                    }
                }

                for (int i = storageSubscriptDefines.size() - 1; i >= 0; i--) {
                    StorageSubscriptDefine storageSubscriptDefine = storageSubscriptDefines.get(i);
                    if (storageSubscriptDefine.getIsRowid() == 1) {
                        childsubId = rowId.substring(rowId.lastIndexOf("||") + 2);
                        idMap.put(storageSubscriptDefine.getExpression()
                                        .replace("{", "")
                                        .replace("}", ""),
                                childsubId); //childsub
                        break;
                    }
                }
            }
        }
        return idMap;
    }


    /**
     * 字段取值
     * @param subClsPropDef
     * @param sub_colName
     * @param subSeriJsonArr
     * @param allTableMeta
     * @return
     */
    private static Map<String, String> evaluationValue(ClassPropertyDefine subClsPropDef, String sub_colName, JSONArray subSeriJsonArr,
                                                       Map<String, TableMeta> allTableMeta) {
        String sub_colType;
        String sub_colValue = "";
        String value;
        Map<String, String> colInfo = new HashMap<>();
        String sub_clsPopColType = subClsPropDef.getRuntimeType(); //取自classProperty
        int sub_storePiece = Integer.parseInt(subClsPropDef.getStoragePiece()) - 1; //属性类子属性下标
        sub_colType = allTableMeta.get(sub_colName).getDataType();
        value = EncodeUtil.unicodeToString(subSeriJsonArr.getString(sub_storePiece)); //转掉unicode

        if(StringUtils.equalsIgnoreCase(sub_clsPopColType, "%Library.Date")) {
            sub_colValue = converValueTimeOrDate(value, "date");
            sub_colType = "Date";
        }else if(StringUtils.equalsIgnoreCase(sub_clsPopColType, "%Library.Time")) {
            sub_colValue = converValueTimeOrDate(value, "time");
            sub_colType = "Time";
        }else {
            sub_colValue = value;
        }

        colInfo.put("colType", sub_colType);
        colInfo.put("colValue", sub_colValue);
        return colInfo;
    }


    /**
     * 将毫秒值转为日期格式
     * @param colValue
     * @param colType
     * @return
     */
    private static String converValueTimeOrDate(String colValue, String colType){
        String type = colType.toLowerCase();
        String value;
        switch (type){
            case "date":
                value = DateTimeUtil.IntToShortDate(Integer.parseInt(colValue));
                break;
            case "time":
                value = DateTimeUtil.IntToTime(Integer.parseInt(colValue));
                break;
            default:
                value = colValue;
        }
        return value;
    }


    /**
     * 启用钩子函数_下标记录
     */
    public void saveOffsetInfo(Map<String, String> offsetMap, String errMsg) {
        for (Map.Entry<String, String> offset : offsetMap.entrySet()) {
            String logId = UUID.randomUUID().toString();
            ParseLogs parseLogs = new ParseLogs();
            parseLogs.setLogId(logId);
            parseLogs.setOffsetNamespace(offset.getKey());
            parseLogs.setOffsetIndex(offset.getValue());
            parseLogs.setCreateTime(new Date());
            if(StringUtils.isEmpty(errMsg)) {
                parseLogs.setIdentification(1);
            }else {
                parseLogs.setIdentification(2);
                ParseLogDetails logDetails = new ParseLogDetails(errMsg);
                logDetails.setLogsId(logId);
                logDetails.setCreateTime(new Date());
                parseLogDetailsMapper.insertSelective(logDetails);
            }
            parseLogsMapper.insertSelective(parseLogs);
            sqlSession.commit();
        }
        System.out.println("钩子函数执行啦!!!!!!");
    }


    /**
     * 记录异常数据处理信息
     * @param parseLogs
     * @param logDetails
     */
    public static void saveErrInfo(ParseLogs parseLogs, ParseLogDetails logDetails) throws Exception{
        String log_id = UUID.randomUUID().toString();
        parseLogs.setLogId(log_id);
        parseLogs.setCreateTime(new Date());
        logDetails.setLogsId(log_id);
        logDetails.setCreateTime(new Date());
        parseLogsMapper.insertSelective(parseLogs);
        parseLogDetailsMapper.insertSelective(logDetails);
        sqlSession.commit();
    }


    /**
     * 格式化globalName
     * @return
     */
    private static Object[] getGlobalDefInfos(JSONObject parse) throws Exception{

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
    private static String getGlobalAlias(String globalName, List<StorageSubscriptDefine> subscriptDefines) throws Exception{
        StringBuilder sb = new StringBuilder(String.format("^%s(", globalName));
        for (StorageSubscriptDefine subscriptDefine : subscriptDefines) {
                sb.append(subscriptDefine.getExpression()).append(",");
        }
        //throw new IllegalAccessException("参数错误！");
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


    /**
     * 测试流字段反查逻辑
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        List<String> global = new ArrayList<>();
        //global.add("^DHCEMRI.InstanceDataS(11,1)");
        global.add("^DHCEMRI.InstanceDataS(3,1)"); //字段流存储格式
        getLogsByQuery("DHC-APP", "User.PACEmployeeType", global);
    }

}
