package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.clinbrain.model.*;
import com.clinbrain.util.DateTimeUtil;
import com.clinbrain.util.EncodeUtil;
import com.intersys.cache.Dataholder;
import com.intersys.objects.CacheException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple4;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.intersys.objects.Database.RET_PRIM;

/**
 * @Description cache实时日志还原
 * @Author xhy
 * @Date 2020/4/8 10:39
 */
public class ParseCacheLog {

    private final Logger logger = LoggerFactory.getLogger(ParseCacheLog.class);

    /**
     * 缓存当前处理的日志记录
     */
    private JournalRecord currentJournalRecord;
    /**
     * 当前日志的操作类型: "I" "U" "D"
     */
    private String operatorType;
    /**
     * 程序上下文
     */
    private Context context;
    /**
     * 当前日志记录所属的namespace
     */
    private String namespace;
    /**
     * 当前还原记录所属的表名
     */
    private String tableName;
    /**
     * 当前日志记录解析所对应的配置
     */
    private GlobalManager globalManager;
    /**
     * 当前日志记录所属类的配置信息
     */
    private Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> classInfo;
    /**
     * 当前处理的global节点
     */
    private String currentDealRealGlobalNode;
    /**
     * cache表主键名
     */
    private String primaryKey;
    /**
     * 存放解析后的列数据: key: 列名 value: 列值
     */
    private Map<String, String> tableFieldMap;
    /**
     * 当前记录的before值
     */
    private Map<String, String> tableRecordBeforeMap;
    /**
     * 当前记录的after值
     */
    private Map<String, String> tableRecordAfterMap;

    private int topicPartitionNumber;

    public ParseCacheLog(Context context) {
        this.context = context;

        topicPartitionNumber = Integer.parseInt(context.getKafkaProps().getProperty("kafka.topic.partition"));
    }

    /**
     * 解析journal日志文件的记录
     * @param journalRecord
     * @throws Exception
     */
    public void parseJournalRecord(JournalRecord journalRecord) throws Exception {
        currentJournalRecord = journalRecord;
        logger.debug("current journal record: {}", currentJournalRecord.toString());

        //User.MRDiagnos
        try {
            //过滤不需要处理的global
            if (context.getAllGlobalName().contains(currentJournalRecord.getGlobalNode())) {
                currentJournalRecord.initInfo();
                //获取当前记录的操作类型
                setOperatorType();
                //获取当前记录所对应的namespace,如果为null则不处理
                List<String> namespaces = context.getAllGlobalDbMapNamespace().get(String.format("%s_%s",
                        currentJournalRecord.getGlobalNode(), currentJournalRecord.getDatabaseName()));//得到namespace
                if (namespaces != null) {
                    boolean isNamespaceFlag = false;
                    //匹配日志记录对应namespace
                    for (String namespace : namespaces) {
                        this.namespace = namespace;
                        //配置只允许配置一个对应namespace, 找到处理即跳出
                        if (isNamespaceFlag)
                            break;

                        //根据namespace获取解析当前的日子记录所需的配置信息
                        globalManager = context.getAllGlobalManager().get(namespace);
                        if (globalManager == null)
                            continue;

                        //开始处理日志记录数据
                        //根据global的名字和节点长度获取可能的配置信息
                        List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>> allGlobalNameLengthInfo =
                                globalManager.getAllGlobalNodeStorage().get(currentJournalRecord.getGlobalNameAndLength());
                        if (allGlobalNameLengthInfo != null) {
                            for (Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>> globalNameLengthInfo : allGlobalNameLengthInfo) {
                                //比对global的节点信息，判断出其真实的global节点
                                if (compareGlobalNodeInfo(currentJournalRecord.getAllGlobalNode(), globalNameLengthInfo._1)) {
                                    isNamespaceFlag = true;
                                    //通过真实值获取其在配置中对应global名字
                                    String globalConfigValue = currentJournalRecord.getGlobalConfigValue(globalNameLengthInfo._1);
                                    List<ClassPropertyDefine> classPropDef = globalNameLengthInfo._2;
                                    //得到当前global对应的配置类
                                    String className = classPropDef.get(0).getClassName();
                                    classInfo = globalManager.getAllClassInfo().get(className);
                                    //未匹配到配置类属性，不作处理
                                    if (classInfo == null) {
                                        continue;
                                    }

                                    //获取配置类所有的global节点
                                    Set<String> allGlobalNodeOfClass = globalManager.getAllGlobalNodeOfClass().get(className);
                                    //替换global配置节点变量为真实节点
                                    Map<String, String> allGlobalRealValue = buildGlobal(currentJournalRecord.getAllGlobalNode(),
                                            globalNameLengthInfo._1, allGlobalNodeOfClass);
                                    //得到配置类对应的sql表信息
                                    DataTable dataTable = classInfo._4();
                                    if (dataTable == null) {
                                        continue;
                                    }

                                    tableName = dataTable.getTableName();

                                    //还原数据,当childsubId有值时，如果值为0，则数据为计数节点，不需要处理
                                    if (buildTableField(allGlobalRealValue, globalConfigValue, globalNameLengthInfo._1)) {
                                        break;
                                    }

                                    //组装ogg消息格式往topic发送数据
                                    String topicName = dataTable.getOutputTopic();
                                    int partitionIndex = (currentJournalRecord.getGlobalNode().hashCode() & Integer.MAX_VALUE)
                                            % topicPartitionNumber;
                                    String oggMsg = parseOggMsg();
                                    if (!context.isLocalTest()) {
                                        context.getKafkaSink().sendMessage(topicName, partitionIndex, oggMsg,
                                                currentJournalRecord.getGlobalNode()); //发送kafka
                                    }

                                    logger.debug("build ogg message : {}", oggMsg);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(currentJournalRecord.toString());
            logger.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        } finally {
            clearStatus();
        }
    }

    /**
     * 清除处理当前记录处理的状态
     */
    private void clearStatus() {
        currentJournalRecord = null;
        operatorType = null;
        namespace = null;
        tableName = null;
        globalManager = null;
        classInfo = null;
        currentDealRealGlobalNode = null;
        primaryKey = null;
        tableFieldMap = null;
        tableRecordBeforeMap = null;
        tableRecordAfterMap = null;
    }

    /**
     * 还原sql表记录,当childsubId有值时，如果值为0，则数据为计数节点，不需要处理
     * @param allGlobalRealValue
     * @param globalConfigValue
     * @param storageSubscriptDefines
     * @return
     */
    private boolean buildTableField(Map<String, String> allGlobalRealValue,
                                 String globalConfigValue,
                                 List<StorageSubscriptDefine> storageSubscriptDefines) {
        tableFieldMap = new HashMap<>();

        //获取rowid,parrent,childsub值
        if (buildRowId(storageSubscriptDefines)) {
            return true;
        }

        //查看当前日志记录是否有其他的global节点
        List<String> otherGlobalNodeConfigValue = new ArrayList<>();
        List<String> otherGlobalNodeRealValue = new ArrayList<>();
        for (Map.Entry<String, String> entry : allGlobalRealValue.entrySet()) {
            if (!StringUtils.equalsIgnoreCase(currentJournalRecord.getGlobalReference(), entry.getValue())) {
                otherGlobalNodeConfigValue.add(entry.getKey());
                otherGlobalNodeRealValue.add(entry.getValue());
            }
        }

        //如果当前日志记录有其他的global节点,则从库中反查数据
        if (!otherGlobalNodeRealValue.isEmpty()) {
            JSONArray globalArray = queryAllGlobal(otherGlobalNodeRealValue, "Normal");
            for (int i = 0; i < globalArray.size(); i++) {
                List<ClassPropertyDefine> classPropertyDefines = globalManager.getAllGlobalNodeProperty().
                        get(otherGlobalNodeConfigValue.get(i));
                currentDealRealGlobalNode = otherGlobalNodeRealValue.get(i);
                JSONArray globalValue = globalArray.getJSONObject(i).getJSONArray("globalValue");
                buildGlobalField(globalValue, classPropertyDefines);
            }
        }

        //获取table meta中比property多的字段
        Map<String, TableMeta> allTableMeta = globalManager.getAllTableMeta().get(classInfo._4().getTableName());
        Set<String> allLeftColumnName = new HashSet<>(allTableMeta.keySet());

        //获取当前日志记录的字段
        currentDealRealGlobalNode = currentJournalRecord.getGlobalReference();
        List<ClassPropertyDefine> classPropertyDefines = globalManager.getAllGlobalNodeProperty().get(globalConfigValue);
        if (StringUtils.equalsIgnoreCase(operatorType, "I")) {
            buildGlobalField(currentJournalRecord.getAfterArray(), classPropertyDefines);

            tableRecordAfterMap.putAll(tableFieldMap);
            allLeftColumnName.removeAll(tableRecordAfterMap.keySet());
        } else if (StringUtils.equalsIgnoreCase(operatorType, "U")) {
            tableRecordBeforeMap.putAll(tableFieldMap);
            tableRecordAfterMap.putAll(tableFieldMap);

            tableFieldMap.clear();
            buildGlobalField(currentJournalRecord.getBeforeArray(), classPropertyDefines);
            tableRecordBeforeMap.putAll(tableFieldMap);
            tableFieldMap.clear();
            buildGlobalField(currentJournalRecord.getAfterArray(), classPropertyDefines);
            tableRecordAfterMap.putAll(tableFieldMap);

            allLeftColumnName.removeAll(tableRecordAfterMap.keySet());
        } else {
            buildGlobalField(currentJournalRecord.getBeforeArray(), classPropertyDefines);

            tableRecordBeforeMap.putAll(tableFieldMap);
            allLeftColumnName.removeAll(tableRecordBeforeMap.keySet());
        }

        //追加剩余的字段
        tableFieldMap.clear();
        appendLeftColumn(allLeftColumnName);

        return false;
    }

    /**
     * 追加剩余的字段 table meta字段大于等于property数，计算字段property无法处理
     * @param allLeftColumnName
     */
    private void appendLeftColumn(Set<String> allLeftColumnName) {
        if (!allLeftColumnName.isEmpty()) {
            for (String columnName : allLeftColumnName) {
                tableFieldMap.put(columnName, null);
            }

            if (StringUtils.equalsIgnoreCase(operatorType, "I")) {
                tableRecordAfterMap.putAll(tableFieldMap);
            } else if (StringUtils.equalsIgnoreCase(operatorType, "U")) {
                tableRecordBeforeMap.putAll(tableFieldMap);
                tableRecordAfterMap.putAll(tableFieldMap);
            } else {
                tableRecordBeforeMap.putAll(tableFieldMap);
            }
        }
    }

    /**
     * 还原global的字段
     * @param globalNodeValue
     * @param properties
     */
    private void buildGlobalField(JSONArray globalNodeValue, List<ClassPropertyDefine> properties) {
        for (ClassPropertyDefine classPropertyDefine : properties) {
//            System.out.println(classPropertyDefine.getPropertyName());
            extractPropertyValue(classPropertyDefine, classPropertyDefine.getSqlFieldName(), globalNodeValue);
        }
    }

    /**
     * 抽取property的值
     * @param classPropertyDefine
     * @param columnName
     * @param globalNodeValue
     */
    private void extractPropertyValue(ClassPropertyDefine classPropertyDefine, String columnName, JSONArray globalNodeValue) {
        String propertyValue;
        String storagePieceStr = classPropertyDefine.getStoragePiece();
        //property的storage piece有多个,需要获取分隔符获取数据
        // TODO: 2020/6/10 目前只处理15，1这种情况，更多的直接抛出异常
        if (storagePieceStr.contains(".")) {
            String[] indexArray = classPropertyDefine.getStoragePiece().split("\\.");
            if (2 != indexArray.length) {
                logger.error(classPropertyDefine.getStoragePiece());
                throw new RuntimeException(classPropertyDefine.getStoragePiece());
            }

            //默认分隔符为逗号，默认第一个为"^"，不然直接抛异常
            String[] delimiters = classPropertyDefine.getStorageDelimiter().split(",");
            if (StringUtils.equalsIgnoreCase(delimiters[0], "\"^\"")) {
                //下标需要减一，storagePiece不是真实数组下标
                int storagePiece = Integer.parseInt(indexArray[0]);
                storagePiece = storagePiece == 0 ? 0 : storagePiece - 1;
                if (storagePiece >= globalNodeValue.size()) {
                    propertyValue = null;
                } else {
                    String[] propertyValueArray = globalNodeValue.getString(storagePiece).split(delimiters[1].replaceAll("\"", ""));
                    storagePiece = Integer.parseInt(indexArray[1]);
                    storagePiece = storagePiece == 0 ? 0 : storagePiece - 1;
                    if (storagePiece >= propertyValueArray.length) {
                        propertyValue = null;
                    } else {
                        JSONArray array = new JSONArray();
                        array.addAll(Arrays.asList(propertyValueArray));
                        propertyValue = extractPropertyValueWithType(classPropertyDefine, columnName, array, storagePiece);
                    }
                }
            } else {
                logger.error(classPropertyDefine.getStorageDelimiter());
                throw new RuntimeException(classPropertyDefine.getStorageDelimiter());
            }
        } else {
            //下标需要减一，storagePiece不是真实数组下标
            int storagePiece = Integer.parseInt(storagePieceStr);
            storagePiece = storagePiece == 0 ? 0 : storagePiece - 1;
            if (storagePiece >= globalNodeValue.size()) {
                propertyValue = null;
            } else {
                propertyValue = extractPropertyValueWithType(classPropertyDefine, columnName, globalNodeValue, storagePiece);
            }
        }

        tableFieldMap.put(columnName, propertyValue);
    }

    /**
     * 根据property类型获取字段值
     * @param classPropertyDefine
     * @param columnName
     * @param globalNodeValue
     * @param storagePiece
     * @return
     */
    private String extractPropertyValueWithType(ClassPropertyDefine classPropertyDefine, String columnName,
                                                JSONArray globalNodeValue, int storagePiece) {
        String columnValue = null;
        //属性为对象类型则找子属性
        if (!StringUtils.startsWith(classPropertyDefine.getRuntimeType(), "%")) {
            Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable> objectClassInfo
                    = globalManager.getAllClassInfo().get(classPropertyDefine.getRuntimeType());
            //序列华类型, 需展开,字段拼接
            if (StringUtils.equalsIgnoreCase(objectClassInfo._1().getClassType(), "serial")) {
                JSONArray objectData = globalNodeValue.getJSONArray(storagePiece); //属性类对应数据
                List<ClassPropertyDefine> allSubClassPropDef = objectClassInfo._3(); //类对应属性
                allSubClassPropDef.forEach(subClassPropDef -> {
                    if (!(StringUtils.startsWith(subClassPropDef.getPropertyName(), "%") ||
                            StringUtils.isEmpty(subClassPropDef.getStoragePiece()))) {
                        String subColumnName = String.format("%s_%s", classPropertyDefine.getSqlFieldName(),
                                subClassPropDef.getSqlFieldName());
                        extractPropertyValue(subClassPropDef, subColumnName, objectData);
                    }
                });
            } else {
                //持久类型直接赋值
                columnValue = globalNodeValue.getString(storagePiece);
            }
        } else {
            switch (classPropertyDefine.getRuntimeType()) {
                case "%Collection.ListOfDT":
                case "%Collection.ListOfObj":
                    columnValue = extractListType(classPropertyDefine, globalNodeValue, storagePiece);
                    break;
                case "%Library.GlobalCharacterStream":
                case "%Library.GlobalBinaryStream":
                    columnValue = extractStreamType(classPropertyDefine, globalNodeValue, storagePiece);
                    break;
                case "%Library.Date":
                    columnValue = globalNodeValue.getString(storagePiece);
                    columnValue = StringUtils.isNotEmpty(columnValue) ?
                            DateTimeUtil.StringToShortDate(columnValue) : null;
                    break;
                case "%Library.Time":
                    columnValue = globalNodeValue.getString(storagePiece);
                    columnValue = StringUtils.isNotEmpty(columnValue) ?
                            DateTimeUtil.StringToTime(columnValue) : null;
                    break;
                default:
                    columnValue = globalNodeValue.getString(storagePiece);
                    break;
            }
        }

        return EncodeUtil.unicodeToString(columnValue);
    }

    /**
     * 抽取流数据类型的字段值
     * @param classPropertyDefine
     * @param globalNodeValue
     * @param storagePiece
     * @return
     */
    private String extractStreamType(ClassPropertyDefine classPropertyDefine, JSONArray globalNodeValue, int storagePiece) {
        String streamValue;
        String streamDataAddress = globalNodeValue.getString(storagePiece); //存储global地址
        // TODO: 2020/6/15 目前电子病历的表不去反查库
        // 反查可能会报 com.intersys.objects.CacheServerException: <MAXSTRING>zQueryGlobals+30^ClinBrain.CacheBasic.1
        // 目前只返回 流节点
        if (tableName.equalsIgnoreCase("instancedata")) {
            return streamDataAddress;
        }

        if (StringUtils.isNotEmpty(streamDataAddress)) {
            List<String> subNodeGlobals = new ArrayList<>();
            subNodeGlobals.add(streamDataAddress);
            JSONArray globalValue = queryAllGlobal(subNodeGlobals, classPropertyDefine.getRuntimeType());
            if (null == globalValue) {
                streamValue = null;
            } else {
                JSONArray dataNodes = globalValue.getJSONObject(0).getJSONArray("nodes");
                if (dataNodes.size() > 2) {
                    Integer dataCount = dataNodes.getJSONObject(0).getInteger("data") + 2;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < dataNodes.size() && i < dataCount; i++) {
                        sb.append(dataNodes.getJSONObject(i).getString("data"));
                    }
                    streamValue = sb.toString();
                } else {
                    streamValue = null;
                }
            }
        } else {
            streamValue = null;
        }

        return streamValue;
    }

    /**
     * 抽取列表类型的字段值
     * @param classPropertyDefine
     * @param globalNodeValue
     * @param storagePiece
     * @return
     */
    private String extractListType(ClassPropertyDefine classPropertyDefine, JSONArray globalNodeValue, int storagePiece) {
        String listValue = null;
        if (StringUtils.equalsIgnoreCase(classPropertyDefine.getSqlListType(), "SUBNODE")) {
            //subnode节点需要反查cache库
            List<String> subNodeGlobals = new ArrayList<>();
            subNodeGlobals.add(String.format("%s)",
                    currentDealRealGlobalNode.substring(0, currentDealRealGlobalNode.lastIndexOf(','))));
            JSONArray globalValue = queryAllGlobal(subNodeGlobals, "SubNode");
            if (null == globalValue) {
                listValue = null;
            } else {
                JSONArray dataNodes = globalValue.getJSONObject(0).getJSONArray("nodes");
                if (dataNodes.size() > 1) {
                    Integer dataCount = dataNodes.getJSONObject(0).getInteger("data") + 1;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < dataNodes.size() && i < dataCount; i++) {
                        sb.append(dataNodes.getJSONObject(i).getString("data")).append(",");
                    }
                    listValue = sb.deleteCharAt(sb.lastIndexOf(",")).toString();
                } else {
                    listValue = null;
                }
            }
        } else {
            if (globalNodeValue.size() == 0 || (globalNodeValue.get(storagePiece) instanceof String)) {
                listValue = null;
            } else {
                try {
                    JSONArray array = globalNodeValue.getJSONArray(storagePiece);
                    StringBuilder sb = new StringBuilder();
                    for (Object o : array) {
                        if (StringUtils.isEmpty(classPropertyDefine.getSqlListDelimiter()))
                            sb.append(o).append(","); //list类型对应数据拼接默认分隔符为","
                        else
                            sb.append(o).append(classPropertyDefine.getSqlListDelimiter());
                    }
                    listValue = sb.delete(sb.lastIndexOf(","), sb.length()).toString();
                } catch (Exception e) {
                    listValue = null;
                }
            }
        }

        return listValue;
    }

    /**
     * 反查cahce库接口
     * @param queryParam
     * @return
     * @throws CacheException
     */
    private String queryBatchGlobalValue(String queryParam) throws CacheException {
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(queryParam);
        Dataholder res = context.getCacheConnection().runClassMethod("ClinBrain.CacheBasic", "QueryGlobals", args, RET_PRIM);
        return res.toString();
    }

    /**
     * 构建查询参数
     * @param allGlobalRealValue
     * @param globalType
     * @return
     */
    private String buildQueryParam(List<String> allGlobalRealValue, String globalType) {
        JSONArray array = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("table", classInfo._4().getClassName()); //参数为class name
        JSONArray globalArray = new JSONArray();
        allGlobalRealValue.stream().forEach(o -> {
            JSONObject globalInfo = new JSONObject();
            globalInfo.put("globalNode", "^|\"" + namespace + "\"|" + o.substring(o.indexOf("^") + 1));
            globalInfo.put("globalType", globalType);
            globalArray.add(globalInfo);
        });
        jsonObject.put("item", globalArray);
        array.add(jsonObject);
        return array.toJSONString();
    }

    /**
     * 查询其余的global值
     * @param allGlobalRealValue
     * @param globalType
     * @return
     */
    private JSONArray queryAllGlobal(List<String> allGlobalRealValue, String globalType) {
        String queryParam = buildQueryParam(allGlobalRealValue, globalType);
        logger.debug("query param : {}", queryParam);
        String queryResult = null;
        try {
            queryResult = queryBatchGlobalValue(queryParam);
        } catch (CacheException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

        JSONArray result = null;
        switch (globalType) {
            case "Normal":
                result = JSONArray.parseArray(queryResult).getJSONObject(0).getJSONArray("data");
                break;
            case "SubNode":
            case "%Library.GlobalCharacterStream":
            case "%Library.GlobalBinaryStream":
                result = JSONArray.parseArray(queryResult).getJSONObject(0).getJSONArray("data").
                        getJSONObject(0).getJSONArray("globalValue");
                break;
            default:
                throw new RuntimeException(String.format("global type [%s] is error.", globalType));
        }

        return result;
    }

    /**
     * 初始化操作状态和存取字段的map
     */
    private void setOperatorType() {
        if (StringUtils.equalsIgnoreCase("SET", currentJournalRecord.getRecordType())) {
            if (currentJournalRecord.getBeforeArray().size() != 0){
                tableRecordBeforeMap = new HashMap<>();
                tableRecordAfterMap = new HashMap<>();
                operatorType = "U";
            } else {
                tableRecordAfterMap = new HashMap<>();
                operatorType = "I";
            }
        } else if (StringUtils.equalsIgnoreCase("KILL", currentJournalRecord.getRecordType())) {
            tableRecordBeforeMap = new HashMap<>();
            operatorType = "D";
        }
    }

    /**
     * 构建global全称(变量替换为真实值)
     * @param logExpressions
     * @param subscriptDef
     * @param allGlobalDef
     * @return
     * @throws Exception
     */
    private Map<String, String> buildGlobal(String[] logExpressions, List<StorageSubscriptDefine> subscriptDef,
                                            Set<String> allGlobalDef) throws Exception {
        Map<String, String> globMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        for (String globalWholeName : allGlobalDef) {
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
     * 构建OGG消息返回格式
     * @return
     * @throws Exception
     */
    private String parseOggMsg() throws Exception {
        JSONObject oggMessage = new JSONObject();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date date = sdf.parse(currentJournalRecord.getTimestamp());
        calendar.setTimeInMillis(date.getTime());
        oggMessage.put("op_ts", String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d%03d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND), Integer.parseInt(
                        currentJournalRecord.getAddress().substring(currentJournalRecord.getAddress().length() - 3))));
        calendar.setTimeInMillis(System.currentTimeMillis());
        oggMessage.put("current_ts", String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d%03d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                calendar.get(Calendar.MILLISECOND), Integer.parseInt(
                        currentJournalRecord.getAddress().substring(currentJournalRecord.getAddress().length() - 3))));

        String databaseName = String.format("hid0101_cache_his_%s_%s",
                namespace.replaceAll("-", "").toLowerCase(),
                classInfo._4().getSchemaName().replaceAll("-", "").toLowerCase());
        String tableName = classInfo._4().getTableName().toLowerCase();

        String table = String.format("%s.%s", databaseName, tableName);
        oggMessage.put("table", table);
        oggMessage.put("pos", currentJournalRecord.getAddress());
        JSONArray primaryKeyArray = new JSONArray();
        primaryKeyArray.add(primaryKey);
        oggMessage.put("primary_keys", primaryKeyArray);

        //查取、设置分区字段
        EtlPartitionConf etlPartitionConf = context.getAllEtlPartitionConf().get(table);

        JSONObject afterData = null;
        JSONObject beforeData = null;
        if (null != tableRecordBeforeMap) {
            beforeData = new JSONObject();
            for (Map.Entry<String, String> entry : tableRecordBeforeMap.entrySet()) {
                beforeData.put(entry.getKey().toLowerCase(), entry.getValue());
            }

            //分區表則追加分區
            if (etlPartitionConf != null) {
                String partitionColumn = etlPartitionConf.getHisTbPartitionColumnName().toLowerCase();
                String bfePartColumnValue = beforeData.getString(partitionColumn) == null ?
                        null : beforeData.getString(partitionColumn).replaceAll("/", "-");
                beforeData.put(String.format("p_%s", partitionColumn), bfePartColumnValue == null ?
                        null : bfePartColumnValue.substring(0, bfePartColumnValue.indexOf(" ")));
            }
        }

        if (null != tableRecordAfterMap) {
            afterData = new JSONObject();
            for (Map.Entry<String, String> entry : tableRecordAfterMap.entrySet()) {
                afterData.put(entry.getKey().toLowerCase(), entry.getValue());
            }

            //分區表則追加分區
            if (etlPartitionConf != null) {
                String partitionColumn = etlPartitionConf.getHisTbPartitionColumnName().toLowerCase();
                String aftPartColumnValue = afterData.getString(partitionColumn) == null ?
                        null : afterData.getString(partitionColumn).replaceAll("/", "-");
                afterData.put(String.format("p_%s", partitionColumn), aftPartColumnValue == null ?
                        null : aftPartColumnValue.substring(0, aftPartColumnValue.indexOf(" ")));
            }
        }

        oggMessage.put("op_type", operatorType);

        oggMessage.put("after", afterData);
        oggMessage.put("before", beforeData);
        String ret = JSON.toJSONString(oggMessage, SerializerFeature.WriteMapNullValue, SerializerFeature.MapSortField);
        return ret;
    }

    /**
     * 获取rowId,当childsubId有值时,如果值为0,则数据为计数节点,不需要处理
     *
     * @param storageSubscriptDefines
     * @return
     */
    private boolean buildRowId(List<StorageSubscriptDefine> storageSubscriptDefines) {
        ClassStorageDefine classStorageDef = classInfo._2().get(0);
        List<ClassPropertyDefine> classPopDef = classInfo._3();

        int rowIdCount = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentJournalRecord.getAllGlobalNode().length; i++) {
            if (storageSubscriptDefines.get(i).getIsRowid() == 1) {
                rowIdCount++;
                sb.append(currentJournalRecord.getAllGlobalNode()[i]).append("||");
            }
        }

        String rowId = sb.delete(sb.lastIndexOf("||"), sb.length()).toString(); //rowId
        tableFieldMap.put(classStorageDef.getSqlRowidName(), rowId);

        primaryKey = classStorageDef.getSqlRowidName().toLowerCase();
        // TODO: 2020/6/12 nuwa处理时会自动添加，暂时不需要添加
        //添加DL统一rowkey字段;值取rowId
//        tableFieldMap.put("rowkey", rowId);

        String parentId = null;
        String childsubId = null;
        if (StringUtils.equalsIgnoreCase("%Library.CacheStorage", classStorageDef.getStorageType())) { //默认存储
            for (ClassPropertyDefine clasPropDef : classPopDef) {
                if (StringUtils.isNotEmpty(clasPropDef.getPropertyCardinality()) &&
                        StringUtils.equalsIgnoreCase("parent", clasPropDef.getPropertyCardinality())) {
                    parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                    childsubId = rowId.substring(rowId.lastIndexOf("||") + 2);
                    tableFieldMap.put(clasPropDef.getSqlFieldName(), parentId); //parentId
                    tableFieldMap.put(classStorageDef.getSqlChildSub(), childsubId); //childsub
                    break;
                }
            }
        } else { //sql存储
            if (rowIdCount > 1) {
                boolean isGetParent = false;
                for (ClassPropertyDefine classPropDef : classPopDef) {
                    if (StringUtils.isNotEmpty(classPropDef.getPropertyCardinality()) &&
                            StringUtils.equalsIgnoreCase("parent", classPropDef.getPropertyCardinality())) {
                        parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                        tableFieldMap.put(classPropDef.getSqlFieldName(), parentId); //parentId
                        isGetParent = true;
                        break;
                    }
                }

                if (!isGetParent) {
                    for (StorageSubscriptDefine storageSubscriptDefine : storageSubscriptDefines) {
                        if (storageSubscriptDefine.getIsRowid() == 1) {
                            parentId = rowId.substring(0, rowId.lastIndexOf("||"));
                            tableFieldMap.put(storageSubscriptDefine.getExpression().replace("{", "").replace("}", ""), parentId); //parentId
                            break;
                        }
                    }
                }

                for (int i = storageSubscriptDefines.size() - 1; i >= 0; i--) {
                    StorageSubscriptDefine storageSubscriptDefine = storageSubscriptDefines.get(i);
                    if (storageSubscriptDefine.getIsRowid() == 1) {
                        childsubId = rowId.substring(rowId.lastIndexOf("||") + 2);
                        tableFieldMap.put(storageSubscriptDefine.getExpression().replace("{", "").replace("}", ""), childsubId); //parentId
                        break;
                    }
                }
            }
        }

        //当childsubId有值时,如果值为0,则数据为计数节点,不需要处理
        return StringUtils.isNotEmpty(childsubId) && StringUtils.equalsIgnoreCase(childsubId, "0");
    }

    /**
     * 比较配置与当前日志中global节点常量是否一致
     * @param logExpressions
     * @param subscriptDefines
     * @return
     */
    private boolean compareGlobalNodeInfo(String[] logExpressions, List<StorageSubscriptDefine> subscriptDefines) {
        boolean flag = true;
        for (int i = 0; i < logExpressions.length; i++) {
            if (subscriptDefines.get(i).getIsRowid() == 0 &&
                    !StringUtils.equals(subscriptDefines.get(i).getExpression(), logExpressions[i])) { //配置中的global节点常量
                //两边不匹配时
                flag = false;
                break;
            }
        }
        return flag;
    }
}
