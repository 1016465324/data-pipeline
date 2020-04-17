package com.clinbrain.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedbMetadata implements Serializable {
    private static Logger logger = LoggerFactory.getLogger(CachedbMetadata.class);
    private Map<String, TableMeta> allDBTableMeta = new HashMap<>();

    public void init(IDatabaseClient databaseClient) throws Exception {
        logger.info("get cache database table metadata.");
        List<String> dbs = databaseClient.getDbNames();
        logger.info("cache database dbs size: {}.", dbs.size());
        int i = 0;
        for (String db : dbs) {
            List<String> tableNames = databaseClient.getTableNames(db);
            logger.info("cache database {} db {} has {} table", i++, db, tableNames.size());
            for (String tableName : tableNames) {
                // TODO: 2019/11/30 目前只存tableName
//                allDBTableMeta.put(db + "." + tableName, databaseClient.getTableMeta(db, tableName));
                allDBTableMeta.put(tableName, databaseClient.getTableMeta(db, tableName));
            }
        }

        logger.info("database table size: {}.", allDBTableMeta.size());
    }

    public Map<String, TableMeta> getAllDBTableMeta() {
        return allDBTableMeta;
    }

    public void setAllDBTableMeta(Map<String, TableMeta> allDBTableMeta) {
        this.allDBTableMeta = allDBTableMeta;
    }
}
