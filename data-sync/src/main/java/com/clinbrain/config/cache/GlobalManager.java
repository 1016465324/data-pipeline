package com.clinbrain.config.cache;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clinbrain.mapper.ClassDefineMapper;
import com.clinbrain.mapper.ClassPropertyDefineMapper;
import com.clinbrain.mapper.ClassStorageDefineMapper;
import com.clinbrain.mapper.DataSourceMapper;
import com.clinbrain.mapper.DataTableMapper;
import com.clinbrain.mapper.StorageSubscriptDefineMapper;
import com.clinbrain.mapper.TableMetaMapper;
import com.clinbrain.model.ClassDefine;
import com.clinbrain.model.ClassPropertyDefine;
import com.clinbrain.model.ClassStorageDefine;
import com.clinbrain.model.DataSource;
import com.clinbrain.model.DataTable;
import com.clinbrain.model.StorageSubscriptDefine;
import com.clinbrain.model.TableMeta;
import com.clinbrain.util.CacheQueryUtil;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple4;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName GlobalManager
 * @Description TODO
 * @Author p
 * @Date 2020/4/8 10:39
 * @Version 1.0
 **/
public class GlobalManager {
    private static Logger logger = LoggerFactory.getLogger(GlobalManager.class);

    private DataSource dataSource;

    /**
     * 所有需要处理的表,不全是cache表
     */
    private static Map<Integer, List<DataTable>> allDataTableOfDsId;
    /**
     * 所有需要同步的表的data source id,不全是cache的数据源
     */
    private static Set<Integer> allDataSourceId;
    /**
     * cache所有需要同步的表的对应的类
     */
    private static Set<String> allClassName;

    /**
     * key: global_name + "_" + length
     */
    private Map<String, List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>>> allGlobalNodeStorage;
    /**
     * key: global whole define
     */
    private Map<String, List<ClassPropertyDefine>> allGlobalNodeProperty;
    /**
     * key: class name
     * value: all global whole define of class name
     */
    private Map<String, Set<String>> allGlobalNodeOfClass;
    /**
     * key: class name
     */
    private Map<String, Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable>> allClassInfo;
    /**
     * key: table name
     */
    private Map<String, Map<String, TableMeta>> allTableMeta;

    private GlobalManager(DataSource dataSource) {
        this.dataSource = dataSource;

        allGlobalNodeStorage = new HashMap<>(10000);
        allGlobalNodeProperty = new HashMap<>(10000);
        allGlobalNodeOfClass = new HashMap<>(10000);
        allClassInfo = new HashMap<>(10000);
        allTableMeta = new HashMap<>(10000);
    }

    public static Set<String> buildAllGlobalName(SqlSessionFactory datahubSessionFactory) {
        SqlSession sqlSession = datahubSessionFactory.openSession();
        Set<String> allGlobalName = sqlSession.getMapper(ClassStorageDefineMapper.class)
                .selectAllClassStorageDefine().stream()
                .filter(o -> !Strings.isNullOrEmpty(o.getGlobalName()))
                .filter(o -> allClassName.contains(o.getClassName()))
                .map(ClassStorageDefine::getGlobalName)
                .collect(Collectors.toSet());
        sqlSession.close();
        return allGlobalName;
    }

    public static Map<String, List<String>> buildGlobalDbMapNamespace() throws Exception {
        /**
         * key: global name + "_" + db name
         */
        Map<String, List<String>> globalDbMapNamespace = new HashMap<>(10000);

        String namespaceStr = CacheQueryUtil.getNameSpaces();
        JSONArray jsonArray = JSONArray.parseArray(namespaceStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String namespace = jsonObject.getString("Namespace");
            String globalStr = CacheQueryUtil.getGlobals(namespace);
            JSONArray globalArray = JSONArray.parseArray(globalStr);
            for (int j = 0; j < globalArray.size(); j++) {
                JSONObject globalObject = globalArray.getJSONObject(j);
                String key = String.format("^%s_%s", globalObject.get("GlobalName"), globalObject.get("DBName"));
                List<String> namespaces = globalDbMapNamespace.get(key);
                if (null == namespaces) {
                    namespaces = new LinkedList<>();
                    namespaces.add(namespace);

                    globalDbMapNamespace.put(key, namespaces);
                } else {
                    namespaces.add(namespace);
                }
            }
        }

        return globalDbMapNamespace;
    }

    public static Map<String, GlobalManager> buildGlobalManager(SqlSessionFactory datahubSessionFactory) {
        // key: namespace
        Map<String, GlobalManager> allGlobalManager = new HashMap<>(16);
    
        SqlSession sqlSession = datahubSessionFactory.openSession();
        List<DataTable> allDataTable = sqlSession.getMapper(DataTableMapper.class).selectAllByStatus();
        allDataTableOfDsId = allDataTable.stream()
                .collect(Collectors.groupingBy(DataTable::getDsId));
        allDataSourceId = allDataTable.stream()
                .map(DataTable::getDsId)
                .collect(Collectors.toSet());
    
        allClassName = allDataTable.stream()
                .map(DataTable::getClassName)
                .filter(className -> !Strings.isNullOrEmpty(className))
                .collect(Collectors.toSet());

        List<DataSource> allCacheDataSource = sqlSession.getMapper(DataSourceMapper.class).selectAllCacheDataSource();
        for (DataSource cacheDataSource : allCacheDataSource) {
            if (allDataSourceId.contains(cacheDataSource.getId())) {
                GlobalManager globalManager = new GlobalManager(cacheDataSource);
                globalManager.generateRule(sqlSession);
                allGlobalManager.put(cacheDataSource.getInstanceName(), globalManager);
            }
        }

        sqlSession.close();
        return allGlobalManager;
    }


    public void generateRule(SqlSession sqlSession) {
        Map<String, List<StorageSubscriptDefine>> allStorageSubscriptDefine = sqlSession.getMapper(StorageSubscriptDefineMapper.class)
                .selectAllByDsId(dataSource.getId()).stream()
                .filter(o -> allClassName.contains(o.getClassName()))
                .collect(Collectors.groupingBy(StorageSubscriptDefine::getStorageName));
        List<ClassStorageDefine> classStorageDefineList = sqlSession.getMapper(ClassStorageDefineMapper.class)
                .selectAllByDsId(dataSource.getId());
        Map<String, ClassStorageDefine> allClassStorageDefine = classStorageDefineList.stream()
                .filter(o -> allClassName.contains(o.getClassName()))
                .collect(Collectors.toMap(ClassStorageDefine::getStorageName, o -> o));

        List<ClassPropertyDefine> classPropertyDefineList = sqlSession.getMapper(ClassPropertyDefineMapper.class)
                .selectAllByDsId(dataSource.getId());
        Map<String, List<ClassPropertyDefine>> allClassPropertyDefine = classPropertyDefineList.stream()
                .filter(o -> StringUtils.isNotEmpty(o.getStorageName()) && allClassName.contains(o.getClassName()))
                .collect(Collectors.groupingBy(ClassPropertyDefine::getStorageName));

        buildAllClassInfo(sqlSession, classStorageDefineList, classPropertyDefineList);

        for (Map.Entry<String, List<StorageSubscriptDefine>> entry : allStorageSubscriptDefine.entrySet()) {
            String storageName = entry.getKey();
            List<StorageSubscriptDefine> storageSubscriptDefines = entry.getValue();
            storageSubscriptDefines.sort(Comparator.comparingInt(StorageSubscriptDefine::getSortId));

            ClassStorageDefine classStorageDefine = allClassStorageDefine.get(storageName);
            if (null == classStorageDefine) {
                logger.warn("storage name {} can't find in table t_class_storage_define.", storageName);
                continue;
            }

            List<ClassPropertyDefine> classPropertyDefines = allClassPropertyDefine.get(storageName);
            if (null == classPropertyDefines) {
                logger.warn("class {} storage name {} can't find in table t_class_property_define.",
                        classStorageDefine.getClassName(), classStorageDefine.getStorageName());
                continue;
            }
            Map<String, List<ClassPropertyDefine>> allSubNodePropertyDefine = classPropertyDefines.stream().peek(o -> {
                if (null == o.getStorageSubscript()) {
                    o.setStorageSubscript("null");
                }
            }).collect(Collectors.groupingBy(ClassPropertyDefine::getStorageSubscript));

            String globalName = classStorageDefine.getGlobalName();
            for (Map.Entry<String, List<ClassPropertyDefine>> subNodeEntry : allSubNodePropertyDefine.entrySet()) {
                List<ClassPropertyDefine> subNodeClassPropertyDefines = subNodeEntry.getValue();
                subNodeClassPropertyDefines.forEach(o -> {
                    if (Strings.isNullOrEmpty(o.getStoragePiece())) {
                        o.setStoragePiece("0");
                    } else {
                        if (o.getStoragePiece().contains(",")) {
                            o.setStoragePiece(o.getStoragePiece().replaceAll(",", "."));
                        }
                    }
                });
                subNodeClassPropertyDefines.sort(Comparator.comparingDouble(o -> Double.parseDouble(o.getStoragePiece())));
                if (!"null".equalsIgnoreCase(subNodeEntry.getKey())) {
                    List<StorageSubscriptDefine> newStorageSubscriptDefines = new LinkedList<>(storageSubscriptDefines);
                    ClassPropertyDefine subNodeClassPropertyDefine = subNodeClassPropertyDefines.get(0);
                    String[] nodes = subNodeEntry.getKey().split(",");
                    for (String node : nodes) {
                        newStorageSubscriptDefines.add(generateGlobalNode(subNodeClassPropertyDefine,
                                node, newStorageSubscriptDefines.size() + 1));
                    }

                    if (1 == subNodeClassPropertyDefines.size() &&
                            !Strings.isNullOrEmpty(subNodeClassPropertyDefine.getSqlListType())) {
                        newStorageSubscriptDefines.add(generateGlobalNode(subNodeClassPropertyDefine,
                                "1", newStorageSubscriptDefines.size() + 1));
                    }

                    buildAllGlobalNodeStorage(globalName, newStorageSubscriptDefines, subNodeClassPropertyDefines);

                    String globalWholeName = buildWholeGlobal(globalName, newStorageSubscriptDefines);
                    allGlobalNodeProperty.put(globalWholeName, subNodeClassPropertyDefines);
                    buildAllGlobalNodeOfClass(classStorageDefine.getClassName(), globalWholeName);
                } else {
                    buildAllGlobalNodeStorage(globalName, storageSubscriptDefines, subNodeClassPropertyDefines);

                    String globalWholeName = buildWholeGlobal(globalName, storageSubscriptDefines);
                    allGlobalNodeProperty.put(globalWholeName, subNodeClassPropertyDefines);
                    buildAllGlobalNodeOfClass(classStorageDefine.getClassName(), globalWholeName);
                }
            }
        }
    }

    private void buildAllTableMeta(SqlSession sqlSession, List<DataTable> dataTables) {
        Set<String> tableNames = dataTables.stream().map(DataTable::getTableName).collect(Collectors.toSet());
        List<TableMeta> tableMetas = sqlSession.getMapper(TableMetaMapper.class).selectAllByDsId(dataSource.getId());
        for (TableMeta tableMeta : tableMetas) {
            if (tableNames.contains(tableMeta.getTableName())) {
                Map<String, TableMeta> columns = allTableMeta.get(tableMeta.getTableName());
                if (null == columns) {
                    columns = new HashMap<>(16);
                    columns.put(tableMeta.getColumnName(), tableMeta);
                    allTableMeta.put(tableMeta.getTableName(), columns);
                } else {
                    columns.put(tableMeta.getColumnName(), tableMeta);
                }
            }
        }
    }

    private void buildAllClassInfo(SqlSession sqlSession, List<ClassStorageDefine> classStorageDefineList,
                                   List<ClassPropertyDefine> classPropertyDefineList) {
        Map<String, List<ClassStorageDefine>> allClassStorageDefineOfClass = classStorageDefineList.stream()
//                .filter(o -> allClassName.contains(o.getClassName()))
                .collect(Collectors.groupingBy(ClassStorageDefine::getClassName));
        List<DataTable> dataTables = allDataTableOfDsId.get(dataSource.getId());
        Map<String, DataTable> allDataTable = dataTables.stream()
                .collect(Collectors.toMap(DataTable::getClassName, o -> o));
        Map<String, ClassDefine> allClassDefine = sqlSession.getMapper(ClassDefineMapper.class)
                .selectAllByDsId(dataSource.getId()).stream()
//                .filter(o -> allClassName.contains(o.getClassName()))
                .collect(Collectors.toMap(ClassDefine::getClassName, o -> o));
        Map<String, List<ClassPropertyDefine>> allPropertyDefineOfClass = classPropertyDefineList.stream()
//                .filter(o -> allClassName.contains(o.getClassName()))
                .collect(Collectors.groupingBy(ClassPropertyDefine::getClassName));

        buildAllTableMeta(sqlSession, dataTables);
        for (Map.Entry<String, ClassDefine> entry : allClassDefine.entrySet()) {
            List<ClassPropertyDefine> classPropertyDefines = allPropertyDefineOfClass.get(entry.getKey());
            List<ClassStorageDefine> classStorageDefines = allClassStorageDefineOfClass.get(entry.getKey());
            DataTable dataTable = allDataTable.get(entry.getKey());
            if (null != classPropertyDefines) {
                this.allClassInfo.put(entry.getKey(), new Tuple4<>(entry.getValue(),
                        classStorageDefines, classPropertyDefines, dataTable));
            }
        }
    }

    private void buildAllGlobalNodeOfClass(String className, String globalWholeName) {
        Set<String> allGlobalNode = allGlobalNodeOfClass.get(className);
        if (null == allGlobalNode) {
            allGlobalNode = new HashSet<>();
            allGlobalNode.add(globalWholeName);

            allGlobalNodeOfClass.put(className, allGlobalNode);
        } else {
            allGlobalNode.add(globalWholeName);
        }
    }

    private String buildWholeGlobal(String globalName,
                                    List<StorageSubscriptDefine> storageSubscriptDefines) {
        StringBuilder sb = new StringBuilder();
        sb.append(globalName);
        sb.append("(");
        for (StorageSubscriptDefine storageSubscriptDefine : storageSubscriptDefines) {
            sb.append(storageSubscriptDefine.getExpression());
            sb.append(",");
        }
        sb.replace(sb.lastIndexOf(","), sb.length(), ")");

        return sb.toString();
    }

    private void buildAllGlobalNodeStorage(String globalName,
                                           List<StorageSubscriptDefine> storageSubscriptDefines,
                                           List<ClassPropertyDefine> subNodeClassPropertyDefines) {
        String key = String.format("%s_%d", globalName, storageSubscriptDefines.size());
        List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>> allGlobalStorageSubscriptDefine
                = allGlobalNodeStorage.get(key);
        if (null == allGlobalStorageSubscriptDefine) {
            allGlobalStorageSubscriptDefine = new LinkedList<>();
            allGlobalStorageSubscriptDefine.add(new Tuple2<>(storageSubscriptDefines, subNodeClassPropertyDefines));
            allGlobalNodeStorage.put(key, allGlobalStorageSubscriptDefine);
        } else {
            allGlobalStorageSubscriptDefine.add(new Tuple2<>(storageSubscriptDefines, subNodeClassPropertyDefines));
        }
    }

    private StorageSubscriptDefine generateGlobalNode(ClassPropertyDefine subNodeClassPropertyDefine,
                                                      String expression,
                                                      int sortId) {
        StorageSubscriptDefine storageSubscriptDefine = new StorageSubscriptDefine();
        storageSubscriptDefine.setClassName(subNodeClassPropertyDefine.getClassName());
        storageSubscriptDefine.setStorageName(subNodeClassPropertyDefine.getStorageName());
        storageSubscriptDefine.setAccessType("");
        storageSubscriptDefine.setExpression(expression);
        storageSubscriptDefine.setIsRowid(0);
        storageSubscriptDefine.setSortId(sortId);
        return storageSubscriptDefine;
    }

    public Map<String, List<Tuple2<List<StorageSubscriptDefine>, List<ClassPropertyDefine>>>> getAllGlobalNodeStorage() {
        return allGlobalNodeStorage;
    }

    public Map<String, List<ClassPropertyDefine>> getAllGlobalNodeProperty() {
        return allGlobalNodeProperty;
    }

    public Map<String, Set<String>> getAllGlobalNodeOfClass() {
        return allGlobalNodeOfClass;
    }

    public Map<String, Tuple4<ClassDefine, List<ClassStorageDefine>, List<ClassPropertyDefine>, DataTable>> getAllClassInfo() {
        return allClassInfo;
    }

    public Map<String, Map<String, TableMeta>> getAllTableMeta() {
        return allTableMeta;
    }
}
