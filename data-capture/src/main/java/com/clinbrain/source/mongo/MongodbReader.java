package com.clinbrain.source.mongo;

import com.clinbrain.DatabaseType;
import com.clinbrain.source.Reader;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class that creates a new DatabaseReader thread for every db
 *
 *
 */
public class MongodbReader extends Reader {
    private final Logger logger = LoggerFactory.getLogger(MongodbReader.class);
    private final ConcurrentLinkedQueue<Document> messages;
    private final String uri;
    private final String host;
    private final Integer port;

    public MongodbReader(String uri, List<String> dbs, Integer batchSize, Properties props) {
        super(dbs, batchSize, props);
        this.uri = uri;
        this.host = null;
        this.port = null;
        this.messages = new ConcurrentLinkedQueue<>();
    }
    
    public MongodbReader(String host, Integer port, List<String> dbs, Integer batchSize, Properties props) {
        super(dbs, batchSize, props);
    	this.uri = null;
        this.host = host;
        this.port = port;
        this.messages = new ConcurrentLinkedQueue<>();
    }

    @Override
	public void run() {
        // for every database to watch
        for (String db : dbs) {            
            final String start = getStartOffset(db);
            
            DatabaseReader reader;
            if (uri != null) {
            	reader = new DatabaseReader(uri, db, start, batchSize, messages);
            } else {
            	reader = new DatabaseReader(host, port, db, start, batchSize, messages);
            }
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
            File path = new File(props.getProperty("offset_path") + DatabaseType.MONGO.name());
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
            start = "0";
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
        logger.info("save mongodb offset");
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(props.getProperty("offset_path") + DatabaseType.MONGO.name(), false));
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
    public boolean isEmpty(){
        return messages.isEmpty();
    }

    @Override
	public Document poll() {
		return messages.poll();
	}
}
