package com.clinbrain.source.cache;

import com.clinbrain.DatabaseType;
import com.clinbrain.source.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CachedbReader extends Reader {
    private final Logger logger = LoggerFactory.getLogger(CachedbReader.class);

    private String url;
    private String username;
    private String password;

    private final ConcurrentLinkedQueue<CDCGlobalLog> messages;


    public CachedbReader(String url, String username, String password,
                         List<String> dbs, Integer batchSize, Properties props) {
        super(dbs, batchSize, props);
        this.url = url;
        this.username = username;
        this.password = password;

        this.messages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        // for every database to watch
        for (String db : dbs) {
            final String start = getStartOffset(db);

            DatabaseReader reader = new DatabaseReader(url, username, password, db, start, batchSize, messages);
            final Thread thread = new Thread(reader);
            thread.start();
            threads.add(thread);
        }
    }

    @Override
    protected String getStartOffset(String db) {
        BufferedReader input = null;
        try {
            String line = null;
            File path = new File(props.getProperty("offset_path") + DatabaseType.CACHE.name());
            if (path.exists()) {
                input = new BufferedReader(new FileReader(path));
                while (null != (line = input.readLine())) {
                    String value = line.trim();
                    if (!value.isEmpty()) {
                        String[] fields = value.split(",");
                        start.put(fields[0], fields[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
        }

        String start;
        // get the last message that was read
        String dbOffset = this.start.get(db);
        if (dbOffset == null || dbOffset.isEmpty()) {
            start = "1";
        } else {
            start = dbOffset;
        }
        return start;
    }

    @Override
    public void saveOffset(String db, String offset) {
        this.start.put(db, offset);
    }

    @Override
    public void saveOffsetToDisk() {
        logger.info("save cachedb offset");
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(props.getProperty("offset_path") + DatabaseType.CACHE.name(), false));
            for (Map.Entry<String, String> entry : start.entrySet()) {
                output.write((entry.getKey() + "," + entry.getValue() + "\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    @Override
    public CDCGlobalLog poll() {
        return messages.poll();
    }
}
