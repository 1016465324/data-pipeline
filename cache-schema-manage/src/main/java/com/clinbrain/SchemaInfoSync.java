package com.clinbrain;

import com.clinbrain.mapper.*;
import com.clinbrain.mapper.cache.*;
import com.clinbrain.model.*;
import com.clinbrain.model.cache.CacheStorageRowidSubscript;
import com.clinbrain.model.cache.CacheStorageSubSubscript;
import com.clinbrain.util.UtilHelper;
import com.google.common.base.Strings;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName SchemaInfoSync
 * @Description TODO
 * @Author p
 * @Date 2020/3/26 15:41
 * @Version 1.0
 **/
public class SchemaInfoSync {
    private static Logger logger = LoggerFactory.getLogger(SchemaInfoSync.class);

    private Properties props;
    private ClassDefineMapper classDefineMapper;
    private ClassPropertyDefineMapper classPropertyDefineMapper;
    private ClassStorageDefineMapper classStorageDefineMapper;
    private DataSchemaMapper dataSchemaMapper;
    private DataSourceMapper dataSourceMapper;
    private DataTableMapper dataTableMapper;
    private StorageSubscriptDefineMapper storageSubscriptDefineMapper;
    private TableMetaMapper tableMetaMapper;
    private SqlSession mysqlSession;
    private Map<String, SqlSession> cacheSessions;
    private SqlSession currentCacheSession;

    public SchemaInfoSync(String path) {
        this.props = UtilHelper.loadProperties(null);

        initMysql();
        initCache();
    }

    private void initMysql() {
        logger.info("init mysql session.");

        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 构建SqlSessionFactory
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 获取sqlSession
        mysqlSession = sqlSessionFactory.openSession();

        classDefineMapper = mysqlSession.getMapper(ClassDefineMapper.class);
        classPropertyDefineMapper = mysqlSession.getMapper(ClassPropertyDefineMapper.class);
        classStorageDefineMapper = mysqlSession.getMapper(ClassStorageDefineMapper.class);
        dataSchemaMapper = mysqlSession.getMapper(DataSchemaMapper.class);
        dataSourceMapper = mysqlSession.getMapper(DataSourceMapper.class);
        dataTableMapper = mysqlSession.getMapper(DataTableMapper.class);
        storageSubscriptDefineMapper = mysqlSession.getMapper(StorageSubscriptDefineMapper.class);
        tableMetaMapper = mysqlSession.getMapper(TableMetaMapper.class);
    }

    private void initCache() {
        String[] namespaces = props.getProperty("cache_namespace").split(",");
        cacheSessions = new HashMap<>(namespaces.length);
        for (String namespace : namespaces) {
            logger.info("init cache namespace {} session.", namespace);
            InputStream inputStream = null;
            try {
                String path = String.format("mybatis-config-cache-%s.xml", namespace.toLowerCase());
                inputStream = Resources.getResourceAsStream(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 构建SqlSessionFactory
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 获取sqlSession
            cacheSessions.put(namespace, sqlSessionFactory.openSession());
        }
    }

    public void schemaInfoSync() {
        try {
            for (Map.Entry<String, SqlSession> sessionEntry : cacheSessions.entrySet()) {
                String namespace = sessionEntry.getKey();
                DataSource dataSource = dataSourceMapper.selectByInstanceAndDbType(namespace);


                currentCacheSession = sessionEntry.getValue();

                logger.info("begin to sync storage sub script define.");
                Map<String, List<StorageSubscriptDefine>> allStorageSubscriptDefine = getAllStorageSubscriptDefine(dataSource);
                logger.info("begin to sync class define.");
                Map<String, ClassDefine> allClassDefine = getAllClassDefine(dataSource);
                for (Map.Entry<String, ClassDefine> entry : allClassDefine.entrySet()) {
                    classDefineMapper.insert(entry.getValue());
                    mysqlSession.commit();
                }

                logger.info("begin to sync class storage define.");
                Map<String, List<ClassStorageDefine>> allClassStorageDefine = getAllClassStorageDefine(dataSource);
                dealCacheStorage(allClassDefine, allClassStorageDefine, allStorageSubscriptDefine, dataSource);
                for (Map.Entry<String, List<ClassStorageDefine>> entry : allClassStorageDefine.entrySet()) {
                    entry.getValue().forEach(o -> classStorageDefineMapper.insert(o));
                    mysqlSession.commit();
                }

                for (Map.Entry<String, List<StorageSubscriptDefine>> entry : allStorageSubscriptDefine.entrySet()) {
                    entry.getValue().forEach(o -> storageSubscriptDefineMapper.insert(o));
                    mysqlSession.commit();
                }

                Map<String, ClassDefine> allClassDefineOfDsId = classDefineMapper.selectAllByDsId(dataSource.getId()).stream()
                        .collect(Collectors.toMap(ClassDefine::getClassName, classDefine -> classDefine));

                logger.info("begin to sync class property define.");
                Map<String, List<ClassPropertyDefine>> allClassPropertyDefine = getAllClassPropertyDefine(dataSource,
                        allClassDefineOfDsId);
                for (Map.Entry<String, List<ClassPropertyDefine>> entry : allClassPropertyDefine.entrySet()) {
                    entry.getValue().forEach(o -> classPropertyDefineMapper.insert(o));
                    mysqlSession.commit();
                }

                logger.info("begin to sync data schema.");
                List<DataSchema> allDataSchema = getAllDataSchema(dataSource);
                allDataSchema.forEach(o -> dataSchemaMapper.insert(o));
                mysqlSession.commit();

                for (DataSchema dataSchema : allDataSchema) {
                    logger.info("begin to sync data table with {}.{}", namespace, dataSchema.getSchemaName());
                    List<DataTable> allDataTableBySchema = getAllDataTableBySchema(dataSource,
                            dataSchema.getSchemaName(), allClassDefineOfDsId);
                    allDataTableBySchema.forEach(o -> dataTableMapper.insert(o));
                    mysqlSession.commit();

                    logger.info("begin to sync table meta with {}.", dataSchema.getSchemaName());
                    List<TableMeta> allTableMetaBySchema = getAllTableMetaBySchema(dataSource, dataSchema.getSchemaName());
                    allTableMetaBySchema.forEach(o -> tableMetaMapper.insert(o));
                    mysqlSession.commit();
                }

                currentCacheSession.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void dealCacheStorage(Map<String, ClassDefine> allClassDefine,
                                  Map<String, List<ClassStorageDefine>> allClassStorageDefine,
                                  Map<String, List<StorageSubscriptDefine>> allStorageSubscriptDefine,
                                  DataSource dataSource) {
        for (Map.Entry<String, List<ClassStorageDefine>> entry : allClassStorageDefine.entrySet()) {
            String className = entry.getKey();
            List<ClassStorageDefine> classStorageDefines = entry.getValue();
            ClassDefine classDefine = allClassDefine.get(className);
            if (null == classDefine) {
                logger.warn("class {} define don't exist.", className);
                continue;
            }

            for (ClassStorageDefine classStorageDefine : classStorageDefines) {
                //persistent serial
                if ("persistent".equalsIgnoreCase(classDefine.getClassType())) {
                    String globalName = findGlobalName(classStorageDefine, allClassDefine, allClassStorageDefine);
                    classStorageDefine.setGlobalName(globalName);

                    if ("%Library.CacheStorage".equalsIgnoreCase(classStorageDefine.getStorageType())) {
                        List<String> expressions = new LinkedList<>();
                        saveCacheStorage(classStorageDefine, allClassDefine, allClassStorageDefine, expressions);

                        List<StorageSubscriptDefine> storageSubscriptDefines = allStorageSubscriptDefine.get(classStorageDefine.getStorageName());
                        if (null == storageSubscriptDefines) {
                            storageSubscriptDefines = new LinkedList<>();
                            Collections.reverse(expressions);
                            for (int j = 0; j < expressions.size(); j++) {
                                StorageSubscriptDefine storageSubscriptDefine = new StorageSubscriptDefine();
                                storageSubscriptDefine.setDsId(dataSource.getId());
                                storageSubscriptDefine.setClassName(className);
                                storageSubscriptDefine.setStorageName(classStorageDefine.getStorageName());
                                storageSubscriptDefine.setAccessType("");
                                storageSubscriptDefine.setExpression(expressions.get(j));
                                if (storageSubscriptDefine.getExpression().startsWith("{")) {
                                    storageSubscriptDefine.setIsRowid(1);
                                } else {
                                    if (expressions.size() - 1 == j) {
                                        storageSubscriptDefine.setIsRowid(1);
                                    } else {
                                        storageSubscriptDefine.setIsRowid(0);
                                    }
                                }

                                storageSubscriptDefine.setSortId(j + 1);
                                storageSubscriptDefine.setCreateTime(new Date());

                                storageSubscriptDefines.add(storageSubscriptDefine);
                            }

                            allStorageSubscriptDefine.put(classStorageDefine.getStorageName(), storageSubscriptDefines);
                        }
                    }
                }
            }
        }
    }

    private void saveCacheStorage(ClassStorageDefine classStorageDefine,
                                  Map<String, ClassDefine> allClassDefine,
                                  Map<String, List<ClassStorageDefine>> allClassStorageDefine,
                                  List<String> expressions) {
        String dataLocation = classStorageDefine.getDataLocation();
        int beginIndex = dataLocation.indexOf('(');
        int endIndex = dataLocation.indexOf(')');
        expressions.add("{" + classStorageDefine.getClassName() + "." +
                (null == classStorageDefine.getSqlRowidName() ? "ID" : classStorageDefine.getSqlRowidName()) + "}");
        if (-1 != beginIndex) {
            String[] fields = dataLocation.substring(beginIndex + 1, endIndex).split(",");
            for (int i = fields.length - 1; i >= 0; i--) {
                expressions.add(fields[i]);
            }
        }

        if (dataLocation.startsWith("{%%PARENT}")) {
            ClassDefine classDefine = allClassDefine.get(classStorageDefine.getClassName());
            String parentClass = classDefine.getRuntimeType();
            List<ClassStorageDefine> parentClassStorageDefines = allClassStorageDefine.get(parentClass);
            if (null == parentClassStorageDefines) {
                logger.warn("can't find class {} in ClassStorageDefine", parentClass);
                return;
            }

            saveCacheStorage(parentClassStorageDefines.get(0), allClassDefine, allClassStorageDefine, expressions);
        }
    }

    private String findGlobalName(ClassStorageDefine classStorageDefine,
                                  Map<String, ClassDefine> allClassDefine,
                                  Map<String, List<ClassStorageDefine>> allClassStorageDefine) {
        String dataLocation = classStorageDefine.getDataLocation();
        if (dataLocation.startsWith("{%%PARENT}")) {
            ClassDefine classDefine = allClassDefine.get(classStorageDefine.getClassName());
            String parentClass = classDefine.getRuntimeType();
            List<ClassStorageDefine> parentClassStorageDefines = allClassStorageDefine.get(parentClass);
            if (null == parentClassStorageDefines) {
                logger.warn("class {} can't find in ClassStorageDefine", parentClass);
                return null;
            } else {
                return findGlobalName(parentClassStorageDefines.get(0), allClassDefine, allClassStorageDefine);
            }
        } else {
            int index = dataLocation.indexOf('(');
            return -1 == index ? dataLocation : dataLocation.substring(0, index);
        }
    }

    private Map<String, List<StorageSubscriptDefine>> getAllStorageSubscriptDefine(DataSource dataSource) {
        List<StorageSubscriptDefine> allStorageSubscriptDefine = new LinkedList<>();
        Map<String, List<CacheStorageSubSubscript>> allCacheStorageSubSubscript = getAllStorageSubSubSubscript();
        Map<String, List<CacheStorageRowidSubscript>> allCacheStorageRowidSubscript = getAllStorageRowidSubSubscript();

        for (Map.Entry<String, List<CacheStorageSubSubscript>> entry : allCacheStorageSubSubscript.entrySet()) {
            String storageName = entry.getKey();
            List<CacheStorageSubSubscript> allCacheStorageSubSubscriptObject = entry.getValue();
            allCacheStorageSubSubscriptObject.sort(Comparator.comparingInt(o -> Integer.parseInt(o.getName())));


            List<CacheStorageRowidSubscript> cacheStorageRowidSubscripts = allCacheStorageRowidSubscript.get(storageName);
            if (null == cacheStorageRowidSubscripts) {
                for (int i = 0; i < allCacheStorageSubSubscriptObject.size(); i++) {
                    StorageSubscriptDefine storageSubscriptDefine = new StorageSubscriptDefine();
                    storageSubscriptDefine.setDsId(dataSource.getId());
                    storageSubscriptDefine.setClassName(allCacheStorageSubSubscriptObject.get(i).getClassName());
                    storageSubscriptDefine.setStorageName(allCacheStorageSubSubscriptObject.get(i).getStorageName());
                    storageSubscriptDefine.setAccessType(null == allCacheStorageSubSubscriptObject.get(i).getAccessType() ?
                            "" : allCacheStorageSubSubscriptObject.get(i).getAccessType());
                    storageSubscriptDefine.setExpression(allCacheStorageSubSubscriptObject.get(i).getExpression());
                    if (storageSubscriptDefine.getExpression().startsWith("{")) {
                        storageSubscriptDefine.setIsRowid(1);
                    } else {
                        if (allCacheStorageSubSubscriptObject.size() - 1 == i) {
                            storageSubscriptDefine.setIsRowid(1);
                        } else {
                            storageSubscriptDefine.setIsRowid(0);
                        }
                    }

                    storageSubscriptDefine.setSortId(Integer.valueOf(allCacheStorageSubSubscriptObject.get(i).getName()));
                    storageSubscriptDefine.setCreateTime(new Date());

                    allStorageSubscriptDefine.add(storageSubscriptDefine);
                }

            } else {
                for (int i = 0; i < allCacheStorageSubSubscriptObject.size(); i++) {
                    StorageSubscriptDefine storageSubscriptDefine = new StorageSubscriptDefine();
                    storageSubscriptDefine.setDsId(dataSource.getId());
                    storageSubscriptDefine.setClassName(allCacheStorageSubSubscriptObject.get(i).getClassName());
                    storageSubscriptDefine.setStorageName(allCacheStorageSubSubscriptObject.get(i).getStorageName());
                    storageSubscriptDefine.setAccessType(null == allCacheStorageSubSubscriptObject.get(i).getAccessType() ?
                            "" : allCacheStorageSubSubscriptObject.get(i).getAccessType());
                    storageSubscriptDefine.setExpression(allCacheStorageSubSubscriptObject.get(i).getExpression());
                    if (storageSubscriptDefine.getExpression().startsWith("{")) {
                        storageSubscriptDefine.setIsRowid(1);
                    } else {
                        boolean hasRowIdInfo = false;
                        String searchKey = String.format("{L%s}", allCacheStorageSubSubscriptObject.get(i).getName());
                        for (CacheStorageRowidSubscript cacheStorageRowidSubscript : cacheStorageRowidSubscripts) {
                            if (searchKey.equalsIgnoreCase(cacheStorageRowidSubscript.getExpression())) {
                                hasRowIdInfo = true;
                                break;
                            }
                        }

                        if (hasRowIdInfo) {
                            storageSubscriptDefine.setIsRowid(1);
                        } else {
                            storageSubscriptDefine.setIsRowid(0);
                        }
                    }

                    storageSubscriptDefine.setSortId(Integer.valueOf(allCacheStorageSubSubscriptObject.get(i).getName()));
                    storageSubscriptDefine.setCreateTime(new Date());

                    allStorageSubscriptDefine.add(storageSubscriptDefine);
                }
            }
        }

        return allStorageSubscriptDefine.stream().collect(Collectors.groupingBy(StorageSubscriptDefine::getStorageName));
    }

    private List<TableMeta> getAllTableMetaBySchema(DataSource dataSource, String schemaName) {
        CacheTableMetaMapper cacheTableMetaMapper = currentCacheSession.getMapper(CacheTableMetaMapper.class);
        return cacheTableMetaMapper.selectAllTableMeta(schemaName).stream().map(cacheTableMeta -> {
            TableMeta tableMeta = new TableMeta();
            tableMeta.setVerId(1);
            tableMeta.setDsId(dataSource.getId());
            tableMeta.setTableName(cacheTableMeta.getTableName());
            tableMeta.setColumnName(cacheTableMeta.getColumnName());
            tableMeta.setOriginalColumnName(cacheTableMeta.getColumnName());
            tableMeta.setColumnId(cacheTableMeta.getColumnId());
            tableMeta.setInternalColumnId(tableMeta.getColumnId());
            tableMeta.setHiddenColumn("N");
            tableMeta.setVirtualColumn(Strings.isNullOrEmpty(cacheTableMeta.getVirtualColumn()) ? "N" : cacheTableMeta.getVirtualColumn().substring(0, 1));
            tableMeta.setOriginalSer(0);
            tableMeta.setDataType(cacheTableMeta.getDataType());
            long dataLength = 0;
            if ("integer".equalsIgnoreCase(tableMeta.getDataType())) {
                dataLength = Integer.MAX_VALUE;
            } else if ("long".equalsIgnoreCase(tableMeta.getDataType())) {
                dataLength = Long.MAX_VALUE;
            }
            tableMeta.setDataLength(dataLength);
            tableMeta.setDataPrecision(cacheTableMeta.getDataPrecision());
            tableMeta.setDataScale(cacheTableMeta.getDataScale());
            tableMeta.setCharLength(cacheTableMeta.getCharLength());
            tableMeta.setCharUsed(null);
            tableMeta.setNullable(Strings.isNullOrEmpty(cacheTableMeta.getNullable()) ? "N" : cacheTableMeta.getNullable().substring(0, 1));
            tableMeta.setIsPk(Strings.isNullOrEmpty(cacheTableMeta.getIsPk()) ? "N" : cacheTableMeta.getIsPk().substring(0, 1));
            tableMeta.setPkPosition(null);
            Date date = new Date();
            tableMeta.setAlterTime(date);
            tableMeta.setCreateTime(date);
            tableMeta.setComments(cacheTableMeta.getComments());
            tableMeta.setDefaultValue(cacheTableMeta.getDefaultValue());

            return tableMeta;
        }).collect(Collectors.toList());
    }

    private List<DataTable> getAllDataTableBySchema(DataSource dataSource, String schemaName,
                                                    Map<String, ClassDefine> allClassDefineOfDsId) {
        CacheDataTableMapper cacheDataTableMapper = currentCacheSession.getMapper(CacheDataTableMapper.class);
        DataSchema dataSchema = dataSchemaMapper.selectBySchemaNameAndDsId(schemaName, dataSource.getId());
        return cacheDataTableMapper.selectAllDataTable(schemaName).stream().map(cacheDataTable -> {
            DataTable dataTable = new DataTable();
            dataTable.setDsId(dataSource.getId());
            dataTable.setSchemaId(dataSchema.getId());
            dataTable.setSchemaName(cacheDataTable.getSchemaName());
            dataTable.setTableName(cacheDataTable.getTableName());
            dataTable.setTableNameAlias("");
            dataTable.setPhysicalTableRegex("");
            dataTable.setOutputTopic("");
            dataTable.setVerId(1);
            dataTable.setStatus("ok");
            dataTable.setMetaChangeFlg(0);
            dataTable.setBatchId(0);
            dataTable.setVerChangeHistory("");
            dataTable.setVerChangeNoticeFlg(0);
            dataTable.setOutputBeforeUpdateFlg(0);
            dataTable.setDescription(null == cacheDataTable.getDescription() ? "" : cacheDataTable.getDescription());
            dataTable.setFullpullCol("");
            dataTable.setFullpullSplitShardSize("");
            dataTable.setFullpullSplitStyle("");
            dataTable.setFullpullCondition("");
            dataTable.setIsOpen(0);
            dataTable.setIsAutoComplete(Byte.parseByte("0"));
            dataTable.setClassId(allClassDefineOfDsId.get(cacheDataTable.getClassName()).getId());
            dataTable.setClassName(cacheDataTable.getClassName());
            dataTable.setCreateTime(new Date());

            return dataTable;
        }).collect(Collectors.toList());
    }

    private List<DataSchema> getAllDataSchema(DataSource dataSource) {
        CacheDataSchemaMapper cacheDataSchemaMapper = currentCacheSession.getMapper(CacheDataSchemaMapper.class);
        return cacheDataSchemaMapper.selectAllDataSchema().stream().map(cacheDataSchema -> {
            DataSchema dataSchema = new DataSchema();
            dataSchema.setDsId(dataSource.getId());
            dataSchema.setSchemaName(cacheDataSchema.getSchemaName());
            dataSchema.setStatus("active");
            dataSchema.setSrcTopic("");
            dataSchema.setTargetTopic("");
            dataSchema.setDbVip("");
            dataSchema.setCreateTime(new Date());
            dataSchema.setDescription("");

            return dataSchema;
        }).collect(Collectors.toList());
    }

    private Map<String, List<CacheStorageSubSubscript>> getAllStorageSubSubSubscript() {
        CacheStorageSubSubscriptMapper cacheStorageSubSubscriptMapper = currentCacheSession.getMapper(CacheStorageSubSubscriptMapper.class);
        return cacheStorageSubSubscriptMapper.selectAllStorageSubSubscript().stream().collect(
                Collectors.groupingBy(CacheStorageSubSubscript::getStorageName));
    }

    private Map<String, List<CacheStorageRowidSubscript>> getAllStorageRowidSubSubscript() {
        CacheStorageRowidSubscriptMapper cacheStorageRowidSubscriptMapper = currentCacheSession.getMapper(CacheStorageRowidSubscriptMapper.class);
        return cacheStorageRowidSubscriptMapper.selectAllStorageRowidSubscript().stream().collect(
                Collectors.groupingBy(CacheStorageRowidSubscript::getParent)
        );
    }

    private Map<String, List<ClassPropertyDefine>> getAllClassPropertyDefine(DataSource dataSource,
                                                                             Map<String, ClassDefine> allClassDefineOfDsId) {
        CacheClassPropertyDefineMapper cacheClassPropertyDefineMapper = currentCacheSession.getMapper(CacheClassPropertyDefineMapper.class);
        return cacheClassPropertyDefineMapper.selectAllClassPropertyDefine().stream().map(cacheClassPropertyDefine -> {
            ClassPropertyDefine classPropertyDefine = new ClassPropertyDefine();

            System.out.println(cacheClassPropertyDefine.getClassName());
            ClassDefine classDefine = allClassDefineOfDsId.get(cacheClassPropertyDefine.getClassName());
            classPropertyDefine.setDsId(dataSource.getId());
            classPropertyDefine.setClassId(null == classDefine ? null : classDefine.getId());
            classPropertyDefine.setClassName(cacheClassPropertyDefine.getClassName());
            classPropertyDefine.setPropertyName(cacheClassPropertyDefine.getPropertyName());
            classPropertyDefine.setPropertyCollection(cacheClassPropertyDefine.getPropertyCollection());
            classPropertyDefine.setPropertyAliases(null == cacheClassPropertyDefine.getPropertyAliases() ? "" : cacheClassPropertyDefine.getPropertyAliases());
            classPropertyDefine.setPropertyCalculated(cacheClassPropertyDefine.getPropertyCalculated() ? "Y" : "N");
            classPropertyDefine.setPropertyCardinality(cacheClassPropertyDefine.getPropertyCardinality());
            classPropertyDefine.setRuntimeType(cacheClassPropertyDefine.getRuntimeType());
            classPropertyDefine.setSqlFieldName(cacheClassPropertyDefine.getSqlFieldName());
            classPropertyDefine.setSqlListType(null == cacheClassPropertyDefine.getSqlListType() ? "" : cacheClassPropertyDefine.getSqlListType());
            classPropertyDefine.setSqlListDelimiter(cacheClassPropertyDefine.getSqlListDelimiter());
            classPropertyDefine.setStorable(cacheClassPropertyDefine.getStorable() ? "Y" : "N");
            classPropertyDefine.setStorageName(cacheClassPropertyDefine.getStorageName());
            classPropertyDefine.setStorageDelimiter(cacheClassPropertyDefine.getStorageDelimiter());
            classPropertyDefine.setStorageSubscript(cacheClassPropertyDefine.getStorageSubscript());
            classPropertyDefine.setStoragePiece(cacheClassPropertyDefine.getStoragePiece());
            classPropertyDefine.setCreateTime(new Date());

            return classPropertyDefine;
        }).filter(o -> o.getClassId() != null).collect(Collectors.groupingBy(ClassPropertyDefine::getClassName));
    }

    private Map<String, List<ClassStorageDefine>> getAllClassStorageDefine(DataSource dataSource) {
        CacheClassStorageDefineMapper cacheClassStorageDefineMapper = currentCacheSession.getMapper(CacheClassStorageDefineMapper.class);
        return cacheClassStorageDefineMapper.selectAllClassStorageDefine().stream().map(cacheClassStorageDefine -> {
            ClassStorageDefine classStorageDefine = new ClassStorageDefine();
            classStorageDefine.setDsId(dataSource.getId());
            classStorageDefine.setClassName(cacheClassStorageDefine.getClassName());
            classStorageDefine.setStorageId(cacheClassStorageDefine.getStorageId());
            classStorageDefine.setStorageName(cacheClassStorageDefine.getStorageName());
            classStorageDefine.setSqlRowidName(cacheClassStorageDefine.getSqlRowidName());
            classStorageDefine.setSqlChildSub(null == cacheClassStorageDefine.getSqlChildSub() ? "" : cacheClassStorageDefine.getSqlChildSub());
            classStorageDefine.setDataLocation(cacheClassStorageDefine.getDataLocation());
            classStorageDefine.setStreamLocation(cacheClassStorageDefine.getStreamLocation());
            classStorageDefine.setStorageType(cacheClassStorageDefine.getStorageType());
            classStorageDefine.setCreateTime(new Date());

            return classStorageDefine;
        }).collect(Collectors.groupingBy(ClassStorageDefine::getClassName));
    }

    private Map<String, ClassDefine> getAllClassDefine(DataSource dataSource) {
        CacheClassDefineMapper cacheClassDefineMapper = currentCacheSession.getMapper(CacheClassDefineMapper.class);
        return cacheClassDefineMapper.selectAllClassDefine().stream().map(cacheClassDefine -> {
            ClassDefine classDefine = new ClassDefine();
            classDefine.setVerId(1);
            classDefine.setDsId(dataSource.getId());
            classDefine.setClassName(cacheClassDefine.getClassName());
            classDefine.setClassType(cacheClassDefine.getClassType());
            classDefine.setClassSuper(null == cacheClassDefine.getClassSuper() ? "" : cacheClassDefine.getClassSuper());
            classDefine.setSqlRowidPrivate(cacheClassDefine.getSqlRowidPrivate() ? "Y" : "N");
            classDefine.setCompileNamespace(dataSource.getInstanceName().toLowerCase());
            classDefine.setRuntimeType(cacheClassDefine.getRuntimeType());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime parse = LocalDateTime.parse(cacheClassDefine.getTimeChanged(), formatter);
            int timeChanged = (int) (LocalDateTime.from(parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000);
            classDefine.setTimeChanged(timeChanged);
            parse = LocalDateTime.parse(cacheClassDefine.getTimeCreated(), formatter);
            int timeCreated = (int) (LocalDateTime.from(parse).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000);
            classDefine.setTimeCreated(timeCreated);
            classDefine.setCreateTime(new Date());

            return classDefine;
        }).collect(Collectors.toMap(ClassDefine::getClassName, classDefine -> classDefine));
    }
}
