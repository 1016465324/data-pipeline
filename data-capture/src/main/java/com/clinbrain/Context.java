package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.clinbrain.sink.KafkaSink;
import com.clinbrain.source.Reader;
import com.clinbrain.source.cache.CDCGlobalLog;
import com.clinbrain.source.cache.CachedbReader;
import com.clinbrain.source.mongo.MongodbReader;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * @author p
 */
public class Context {
    private static Logger logger = LoggerFactory.getLogger(Context.class);
    private Properties props;
    private Reader reader;
    private KafkaSink kafkaSink;

    Context(Properties props) {
        this.props = props;

        String databaseType = props.getProperty("database_type");
        if (databaseType.equalsIgnoreCase(DatabaseType.CACHE.name())) {
            List<String> dbs = Arrays.asList(props.getProperty("cdc_table").split(","));
            logger.info("cache dbs: {}", dbs);
            reader = new CachedbReader(props.getProperty("cdc_url"), props.getProperty("cdc_username"), props.getProperty("cdc_password"),
                    dbs, Integer.parseInt(props.getProperty("batch_size")), props);
        } else if (databaseType.equalsIgnoreCase(DatabaseType.MONGO.name())) {
            List<String> dbs = Arrays.asList(props.getProperty("mongo_dbs").split(","));
            if (props.getProperty("mongo_uri").isEmpty()) {
                reader = new MongodbReader(props.getProperty("mongo_host"),
                        Integer.parseInt(props.getProperty("mongo_port")),
                        dbs, Integer.parseInt(props.getProperty("batch_size")), props);
            } else {
                reader = new MongodbReader(props.getProperty("mongo_uri"),
                        dbs, Integer.parseInt(props.getProperty("batch_size")), props);
            }
        } else {
            logger.error("database_type error value.");
            System.exit(-1);
        }

        kafkaSink = new KafkaSink(props);
    }

    public void start() {
        reader.run();

        while (true) {
            if (reader.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                String databaseType = props.getProperty("database_type");
                if (databaseType.equalsIgnoreCase(DatabaseType.CACHE.name())) {
                    CDCGlobalLog cdcGlobalLog = (CDCGlobalLog) reader.poll();
                    kafkaSink.sendMessage(props.getProperty("topics"),
                            JSON.toJSONString(cdcGlobalLog, SerializerFeature.WriteMapNullValue), cdcGlobalLog.getDBName());
                    reader.saveOffset(props.getProperty("cdc_table"), cdcGlobalLog.getID());
                } else if (databaseType.equalsIgnoreCase(DatabaseType.MONGO.name())) {
                    Document document = (Document) reader.poll();
                    BsonTimestamp bsonTimestamp = (BsonTimestamp) document.get("ts");
                    int seconds = bsonTimestamp.getTime();
                    Integer order = bsonTimestamp.getInc();

                    String offset = seconds + "_" + order;
                    JSONObject oggObject = new JSONObject();
                    oggObject.put("db_type", "mongo");
                    oggObject.put("table", document.get("ns"));
                    oggObject.put("op_type", document.get("op").toString().toUpperCase());

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(seconds * 1000L);
                    oggObject.put("op_ts", String.format("%04d%02d%02dT%02d:%02d:%02d.%03d",
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), order));
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    oggObject.put("current_ts", String.format("%04d%02d%02dT%02d:%02d:%02d.%03d",
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND),
                            calendar.get(Calendar.MILLISECOND)));

                    oggObject.put("pos", offset);
                    JSONArray primaryKeys = new JSONArray();
                    primaryKeys.add("_id");
                    oggObject.put("primary_keys", primaryKeys);

                    final Document record = (Document) document.get("o");
                    JSONObject message = JSON.parseObject(record.toJson());
                    String op = document.get("op").toString();
                    if (op.equalsIgnoreCase("d")) {
                        oggObject.put("before", message);
                    } else {
                        oggObject.put("after", message);
                    }


                    logger.warn("message: " + oggObject.toJSONString());

                    kafkaSink.sendMessage(props.getProperty("topics"),
                            JSON.toJSONString(oggObject, SerializerFeature.WriteMapNullValue), document.get("ns").toString());
                    logger.warn("offset: database -> {} , timestamp -> {}", document.get("ns").toString(), offset);
                    reader.saveOffset(document.get("ns").toString(), offset);
                } else {
                    logger.error("database_type error value.");
                    System.exit(-1);
                }
            }
        }
    }

    public KafkaSink getKafkaSink() {
        return kafkaSink;
    }

    public void setKafkaSink(KafkaSink kafkaSink) {
        this.kafkaSink = kafkaSink;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }
}
