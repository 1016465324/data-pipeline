package com.clinbrain;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clinbrain.mapper.*;
import com.clinbrain.model.*;
import com.google.common.base.Strings;
import com.intersys.cache.Dataholder;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import scala.Tuple4;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.intersys.objects.Database.RET_PRIM;

/**
 * @ClassName GlobalManager
 * @Description TODO
 * @Author p
 * @Date 2020/4/8 10:39
 * @Version 1.0
 **/
public class GlobalManager {
    private Logger logger = LoggerFactory.getLogger(GlobalManager.class);

    private DataSource dataSource;

    private static DataSourceMapper dataSourceMapper;
    private static ClassDefineMapper classDefineMapper;
    private static ClassPropertyDefineMapper classPropertyDefineMapper;
    private static ClassStorageDefineMapper classStorageDefineMapper;
    private static StorageSubscriptDefineMapper storageSubscriptDefineMapper;
    private static DataTableMapper dataTableMapper;
    private static TableMetaMapper tableMetaMapper;

    static {
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 构建SqlSessionFactory
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 获取sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();

        dataSourceMapper = sqlSession.getMapper(DataSourceMapper.class);
        classDefineMapper = sqlSession.getMapper(ClassDefineMapper.class);
        classPropertyDefineMapper = sqlSession.getMapper(ClassPropertyDefineMapper.class);
        classStorageDefineMapper = sqlSession.getMapper(ClassStorageDefineMapper.class);
        storageSubscriptDefineMapper = sqlSession.getMapper(StorageSubscriptDefineMapper.class);
        dataTableMapper = sqlSession.getMapper(DataTableMapper.class);
        tableMetaMapper = sqlSession.getMapper(TableMetaMapper.class);
    }

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

    public static Set<String> buildAllGlobalName() {
        return classStorageDefineMapper.selectAllClassStorageDefine().stream()
                .filter(o -> !Strings.isNullOrEmpty(o.getGlobalName()))
                .map(ClassStorageDefine::getGlobalName)
                .collect(Collectors.toSet());
    }

    public static Map<String, List<String>> buildGlobalDbMapNamespace(String url, String username, String password) throws CacheException {
        /**
         * key: global name + "_" + db name
         */
        Map<String, List<String>> globalDbMapNamespace = new HashMap<>(10000);

        Database database = CacheDatabase.getDatabase (url, username, password);
//        JBindDatabase database = new JBindDatabase(url, username, password);
        String namespaceStr = getNameSpaces(database);
        JSONArray jsonArray = JSONArray.parseArray(namespaceStr);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String namespace = jsonObject.getString("Namespace");
            String globalStr = getGlobals(database, namespace);
            JSONArray globalArray = JSONArray.parseArray(globalStr);
            for (int j = 0; j < globalArray.size(); j++) {
                JSONObject globalObject = globalArray.getJSONObject(j);
                String key = String.format("%s_%s", globalObject.get("GlobalName"), globalObject.get("DBName"));
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

    private static String getGlobals(Database cacheDatabase, String request) throws CacheException {
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(request);
        Dataholder res = cacheDatabase.runClassMethod("ClinBrain.CacheBasic", "GetGlobals", args, RET_PRIM);
        return res.getString();
    }

    private static String getDbs(Database cacheDatabase) throws CacheException {
        Dataholder[] args = new Dataholder[0];
        Dataholder res = cacheDatabase.runClassMethod("ClinBrain.CacheBasic", "GetDBs", args, RET_PRIM);
        return res.getString();
    }

    private static String getNameSpaces(Database cacheDatabase) throws CacheException {
        Dataholder[] args = new Dataholder[0];
        Dataholder res = cacheDatabase.runClassMethod("ClinBrain.CacheBasic", "GetNameSpaces", args, RET_PRIM);
        return res.getString();
    }

    public static Map<String, GlobalManager> buildGlobalManager() {
        // key: namespace
        Map<String, GlobalManager> allGlobalManager = new HashMap<>(16);

        List<DataSource> dataSources = dataSourceMapper.selectAllCacheDataSource();
        Set<Integer> dsIds = dataTableMapper.selectAllByStatus().stream()
                .map(DataTable::getDsId)
                .collect(Collectors.toSet());
        for (DataSource dataSource : dataSources) {
            if (dsIds.contains(dataSource.getId())) {
                GlobalManager globalManager = new GlobalManager(dataSource);
                globalManager.generateRule();
                allGlobalManager.put(dataSource.getInstanceName(), globalManager);
            }
        }

        return allGlobalManager;
    }


    public void generateRule() {
        Map<String, List<StorageSubscriptDefine>> allStorageSubscriptDefine = storageSubscriptDefineMapper.selectAllByDsId(dataSource.getId()).stream()
                .collect(Collectors.groupingBy(StorageSubscriptDefine::getStorageName));
        List<ClassStorageDefine> classStorageDefineList = classStorageDefineMapper.selectAllByDsId(dataSource.getId());
        Map<String, ClassStorageDefine> allClassStorageDefine = classStorageDefineList
                .stream().collect(Collectors.toMap(ClassStorageDefine::getStorageName, o -> o));

        Map<String, List<ClassPropertyDefine>> allClassPropertyDefine = classPropertyDefineMapper
                .selectAllByDsIdAndStorageName(dataSource.getId()).stream()
                .collect(Collectors.groupingBy(ClassPropertyDefine::getStorageName));

        buildAllClassInfo(classStorageDefineList);

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

    private void buildAllTableMeta(List<DataTable> dataTables) {
        Set<String> tableNames = dataTables.stream().map(DataTable::getTableName).collect(Collectors.toSet());
        List<TableMeta> tableMetas = tableMetaMapper.selectAllByDsId(dataSource.getId());
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

    private void buildAllClassInfo(List<ClassStorageDefine> classStorageDefineList) {
        Map<String, List<ClassStorageDefine>> allClassStorageDefineOfClass = classStorageDefineList
                .stream().collect(Collectors.groupingBy(ClassStorageDefine::getClassName));
        List<DataTable> dataTables = dataTableMapper.selectAllByDsId(dataSource.getId());
        Map<String, DataTable> allDataTable = dataTables.stream()
                .collect(Collectors.toMap(DataTable::getClassName, o -> o));
        Map<String, ClassDefine> allClassDefine = classDefineMapper.selectAllByDsId(dataSource.getId()).stream()
                .collect(Collectors.toMap(ClassDefine::getClassName, o -> o));
        Map<String, List<ClassPropertyDefine>> allPropertyDefineOfClass = classPropertyDefineMapper
                .selectAllByDsId(dataSource.getId()).stream()
                .collect(Collectors.groupingBy(ClassPropertyDefine::getClassName));

        buildAllTableMeta(dataTables);
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
