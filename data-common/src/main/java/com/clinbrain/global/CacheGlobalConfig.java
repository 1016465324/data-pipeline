package com.clinbrain.global;

import com.clinbrain.database.CachedbRecord;
import com.clinbrain.utils.ExcelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class CacheGlobalConfig implements Serializable {
    private static Logger logger = LoggerFactory.getLogger(CacheGlobalConfig.class);

    private Map<TableInfo, List<GlobalInfo>> allTableMapGlobal = new HashMap<>();
    //key 需加上namespace
    private Map<String, List<GlobalInfo>> allGlobalInfo = new HashMap<>();

    public void init(String excelPath) {
        try {
            int[] range = new int[ConfigIndex.MAX.ordinal() - 1];
            for (int i = 0; i < range.length; i++) {
                range[i] = i;
            }

            ArrayList<ArrayList<String>> rows = ExcelUtils.excelReader(excelPath, range);
            if (null == rows) {
                logger.warn("{} has no data.", excelPath);
                return;
            }

            logger.info("{} has {} line.", excelPath, rows.size());
            for (ArrayList<String> row : rows) {
                if (null != row.get(ConfigIndex.GLOBAL.ordinal()) &&
                        row.get(ConfigIndex.GLOBAL.ordinal()).startsWith("^")) {
                    if (null == row.get(ConfigIndex.DATA_NODE.ordinal())) {
                        row.set(ConfigIndex.DATA_NODE.ordinal(), "1");
                    }

                    if (row.get(ConfigIndex.DATA_NODE.ordinal()).split(",").length > 1) {
                        logger.warn("DATA_NODE column data error: {}.", row.get(ConfigIndex.DATA_NODE.ordinal()));
                        continue;
                    }

                    TableInfo tableInfo = new TableInfo(row.get(ConfigIndex.NAMESPACE.ordinal()),
                            row.get(ConfigIndex.ENTITY_CLASS.ordinal()),
                            row.get(ConfigIndex.SQL_TABLE.ordinal()));
                    GlobalInfo globalInfo = new GlobalInfo(row.get(ConfigIndex.GLOBAL.ordinal()));
                    FieldInfo fieldInfo = new FieldInfo(row.get(ConfigIndex.SQL_FIELD.ordinal()),
                            Integer.parseInt(row.get(ConfigIndex.DATA_NODE.ordinal())), "",
                            row.get(ConfigIndex.DATA_TYPE.ordinal()));

                    List<GlobalInfo> globals = allTableMapGlobal.get(tableInfo);
                    if (null == globals) {
                        globalInfo.getAllFieldInfo().add(fieldInfo);
                        globalInfo.setTableInfo(tableInfo);

                        globals = new ArrayList<>();
                        globals.add(globalInfo);
                        allTableMapGlobal.put(tableInfo, globals);
                    } else {
                        int index = globals.indexOf(globalInfo);
                        if (-1 == index) {
                            globalInfo.getAllFieldInfo().add(fieldInfo);
                            globalInfo.setTableInfo(tableInfo);
                            globals.add(globalInfo);
                        } else {
                            globals.get(index).getAllFieldInfo().add(fieldInfo);
                        }
                    }
                }
            }

            for (Map.Entry<TableInfo, List<GlobalInfo>> entry : allTableMapGlobal.entrySet()) {
                for (GlobalInfo globalMetaData : entry.getValue()) {
                    globalMetaData.getAllFieldInfo().sort(Comparator.comparing(FieldInfo::getIndex));

                    String key = globalMetaData.getTableInfo().getNamespace() +
                            globalMetaData.getGlobalName() + globalMetaData.getLength();
                    List<GlobalInfo> globals = allGlobalInfo.get(key);
                    if (null == globals) {
                        globals = new ArrayList<>();
                        globals.add(globalMetaData);

                        allGlobalInfo.put(key, globals);
                    } else {
                        globals.add(globalMetaData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    public CachedbRecord findByGlobal(String globalStr, String namespace, Set<String> sqluserTables) {
        logger.info("find global: {} in namespace {}.", globalStr, namespace);
        GlobalInfo cdcGlobal = new GlobalInfo(globalStr, false);
        String key = namespace.toLowerCase() + cdcGlobal.getGlobalName() + cdcGlobal.getLength();
        List<GlobalInfo> globals = allGlobalInfo.get(key);
        if (null != globals) {
            TableInfo tableInfo = null;
            for (GlobalInfo global : globals) {
                boolean exist = true;
                for (Integer pos : global.getAllConstantIndex()) {
                    if (!global.getGlobalFields()[pos].equals(cdcGlobal.getGlobalFields()[pos])) {
                        exist = false;
                        break;
                    }
                }

                if (exist) {
                    tableInfo = global.getTableInfo();
                    break;
                }
            }

            if (null != tableInfo) {
                List<GlobalInfo> allGlobalPerTable = allTableMapGlobal.get(tableInfo);
                return new CachedbRecord(tableInfo.getSQLTableName(sqluserTables), tableInfo.getEntityName(),
                        allGlobalPerTable, cdcGlobal);
            }
        }

        logger.info("find nothing.");
        return null;
    }

    public Map<TableInfo, List<GlobalInfo>> getAllTableMapGlobal() {
        return allTableMapGlobal;
    }

    public void setAllTableMapGlobal(Map<TableInfo, List<GlobalInfo>> allTableMapGlobal) {
        this.allTableMapGlobal = allTableMapGlobal;
    }

    public Map<String, List<GlobalInfo>> getAllGlobalInfo() {
        return allGlobalInfo;
    }

    public void setAllGlobalInfo(Map<String, List<GlobalInfo>> allGlobalInfo) {
        this.allGlobalInfo = allGlobalInfo;
    }
}
