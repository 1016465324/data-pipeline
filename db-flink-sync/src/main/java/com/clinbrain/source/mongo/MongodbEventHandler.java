package com.clinbrain.source.mongo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author p
 */
public class MongodbEventHandler {

    public final static String EVENT_KEY_OP = "op";
    public final static String EVENT_KEY_NS = "ns";
    public final static String EVENT_KEY_TS = "ts";
    public final static String EVENT_KEY_DATA = "o";
    private final static String EVENT_KEY_BEFORE_DATA = "o2";

    public static String handleEvent(final Document event, AtomicReference<BsonTimestamp> offset, MongoClient client) {
        MongodbOperation mongodbOperation = MongodbOperation.getByInternalNames(event.getString(EVENT_KEY_OP));
        if (StringUtils.equalsIgnoreCase(mongodbOperation.getInternalName(), MongodbOperation.UPDATE.getInternalName())) {
            if (null == handleUpdate(event, client)) {
                return "null";
            }
        }

        JSONObject eventMap = new JSONObject();
        eventMap.put("table", event.get(EVENT_KEY_NS));
        eventMap.put("op_type", mongodbOperation.getInternalName().toUpperCase());

        BsonTimestamp timestamp = event.get(EVENT_KEY_TS, BsonTimestamp.class);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.getTime()), ZoneId.systemDefault());
        eventMap.put("op_ts", String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d%03d",
                localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond(),
                0, timestamp.getInc()));

        localDateTime = LocalDateTime.now();
        eventMap.put("current_ts", String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d%03d",
                localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(),
                localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond(),
                0, timestamp.getInc()));

        eventMap.put("pos", timestamp.getValue());

        JSONArray primaryKeys = new JSONArray();
        primaryKeys.add("rowkey");
        eventMap.put("primary_keys", primaryKeys);

        final Document record = (Document) event.get(EVENT_KEY_DATA);
        JSONObject message = new JSONObject();
        message.put("rowkey", record.get("_id").toString());
        message.put("row", JSON.parseObject(record.toJson()));
        if (mongodbOperation.getInternalName().equalsIgnoreCase(MongodbOperation.DELETE.getInternalName())) {
            eventMap.put("before", message);
        } else if (mongodbOperation.getInternalName().equalsIgnoreCase(MongodbOperation.UPDATE.getInternalName())) {
            eventMap.put("before", null);
            eventMap.put("after", message);
        } else  {
            eventMap.put("after", message);
        }

        offset.set(timestamp);
        return JSON.toJSONString(eventMap, SerializerFeature.WriteMapNullValue);
    }

    private static Document handleUpdate(Document event, MongoClient client) {
        String[] dbAndCollection = StringUtils.split(event.get(EVENT_KEY_NS).toString(), ".");
        MongoCollection<Document> collection = client.getDatabase(dbAndCollection[0]).getCollection(dbAndCollection[1]);
        Object id = ((Document) event.get(EVENT_KEY_BEFORE_DATA)).get("_id");

        List<Document> docs = new ArrayList<>();
        collection.find(Filters.eq("_id", id)).into(docs);
        if (CollectionUtils.isEmpty(docs)) {
            return null;
        } else {
            event.append(EVENT_KEY_DATA, docs.get(0));
            return event;
        }
    }

    private static Map<String, Object> processColumnList(Set<String> keys, Document data, boolean valueNull) {
        Map<String, Object> map = new HashMap<>(keys.size());
        for (String key : keys) {
            if (valueNull) {
                map.put(key, null);
            } else {
                map.put(key, data.get(key));
            }
        }

        return map;
    }

    private static void parseDbAndCollection(final Document event, Map<String, Object> eventMap) {
        String dbCollection = event.getString(EVENT_KEY_NS);
        String[] split = dbCollection.split("\\.");
        eventMap.put("schema", split[0]);
        eventMap.put("table", split[1]);
    }
}
