package com.clinbrain.source.cache;

import com.clinbrain.database.CachedbClient;
import com.clinbrain.database.IDatabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DatabaseReader implements Runnable {
    private final Logger log = LoggerFactory.getLogger(DatabaseReader.class);
    private final String url;
    private final String username;
    private final String password;
    private final String db;
    private final Integer batchSize;
    private Long start;

    private IDatabaseClient client;

    private ConcurrentLinkedQueue<CDCGlobalLog> messages;

    public DatabaseReader(String url, String username, String password, String db, String start,
                          Integer batchSize, ConcurrentLinkedQueue<CDCGlobalLog> messages) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.db = db;
        this.batchSize = batchSize;
        this.start = Long.valueOf(start);
        this.messages = messages;

        init();
    }

    private void init() {
        client = new CachedbClient(url, username, password);
    }

    @Override
    public void run() {
        while (true) {
            if (messages.isEmpty()) {
                log.debug("Query starting in offset {}.", start);
                String sql = getGlobalQuerySQL();
                log.info("query sql: {}", sql);
                List<CDCGlobalLog> cdcGlobalLogs = new LinkedList<>();
                try {
                    ResultSet rs = client.queryWithResultSet(sql);
                    while (rs.next()) {
                        CDCGlobalLog cdcGlobalLog = new CDCGlobalLog();
                        cdcGlobalLog.setID(rs.getString("ID"));
                        cdcGlobalLog.setDBName(rs.getString("DBName"));
                        cdcGlobalLog.setGlobalName(rs.getString("GlobalName"));
                        cdcGlobalLog.setRecordType(rs.getString("RecordType"));
                        cdcGlobalLog.setTS(rs.getString("TS"));
                        cdcGlobalLog.setVal(rs.getString("Val"));

                        cdcGlobalLogs.add(cdcGlobalLog);
                    }

                    log.info("capture data: {}", cdcGlobalLogs.size());
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());

                    throw new RuntimeException(e.getMessage());
                }

                if (!cdcGlobalLogs.isEmpty()) {
                    start += cdcGlobalLogs.size();
                    if (start == Long.MAX_VALUE) {
                        start = 1L;
                    }

                    messages.addAll(cdcGlobalLogs);
                }

                cdcGlobalLogs.clear();
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private String getGlobalQuerySQL() {
        //目前不知CDCLOG中ID的最大值是多少，所以直接取范围,出问题后再看
        return String.format(
                "select ID,DBName,GlobalName,RecordType,Val,TS from cdc.CDCLog where ID >= %d and ID < %d",
                start, start + batchSize);
    }

    @Override
    protected void finalize() {
        if (client != null) {
            client.close();
        }
    }
}
