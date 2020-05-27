package com.clinbrain.source.mongo;

import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @ClassName MongodbOplogInputFormat
 * @Description TODO
 * @Author p
 * @Date 2020/5/20 14:25
 * @Version 1.0
 **/
public class MongodbOplogInputFormat extends RichInputFormat<String, InputSplit> {
    private final Logger LOG = LoggerFactory.getLogger(MongodbOplogInputFormat.class);

    private final static String OPLOG_DB = "local";
    private final static String REPLICA_SET_COLLECTION = "oplog.rs";
    private final static String MASTER_SLAVE_COLLECTION = "oplog.$main";

    private MongodbConfig mongodbConfig;

    private transient MongoClient client;

    private transient MongoCursor<Document> cursor;

    private AtomicReference<BsonTimestamp> offset;

    private InputSplit inputSplit;

    public MongodbOplogInputFormat(MongodbConfig mongodbConfig) {
        this.mongodbConfig = mongodbConfig;
    }

    @Override
    public void configure(Configuration configuration) {

    }

    @Override
    public BaseStatistics getStatistics(BaseStatistics baseStatistics) throws IOException {
        return null;
    }

    @Override
    public InputSplit[] createInputSplits(int i) throws IOException {
        return new InputSplit[]{new GenericInputSplit(1, 1)};
    }

    @Override
    public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
        return new DefaultInputSplitAssigner(inputSplits);
    }

    @Override
    public void open(InputSplit inputSplit) throws IOException {
        openInternal(inputSplit);
    }

    private void openInternal(InputSplit inputSplit) throws IOException {
        this.inputSplit = inputSplit;
        initOffset();

        client = MongodbClientUtil.getClient(mongodbConfig);
        MongoCollection<Document> oplog = getOplogCollection();
        FindIterable<Document> results = oplog.find(buildFilter())
                .sort(new Document("$natural", 1))
                .oplogReplay(true)
                .cursorType(CursorType.TailableAwait);

        cursor = results.iterator();
    }

    /**
     * 在 master/slave 结构下, oplog 位于local.oplog.$main
     * 在 Replca set 结构下， oplog 位于local.oplog.rs
     */
    private MongoCollection<Document> getOplogCollection() {
        if ("REPLICA_SET".equalsIgnoreCase(mongodbConfig.getClusterMode())) {
            return client.getDatabase(OPLOG_DB).getCollection(REPLICA_SET_COLLECTION);
        } else if ("MASTER_SLAVE".equalsIgnoreCase(mongodbConfig.getClusterMode())) {
            return client.getDatabase(OPLOG_DB).getCollection(MASTER_SLAVE_COLLECTION);
        } else {
            throw new RuntimeException("集群模式不支持:" + mongodbConfig.getClusterMode());
        }
    }

    private void initOffset() {
        BsonTimestamp startLocation = new BsonTimestamp(mongodbConfig.getStartLocation(), 0);
        if (null == offset) {
            offset = new AtomicReference<>(startLocation);
        }
    }

    private Bson buildFilter() {
        List<Bson> filters = new ArrayList<>();

        // 设置读取位置
        filters.add(Filters.gt(MongodbEventHandler.EVENT_KEY_TS, new BsonTimestamp(offset.get().getValue())));

        //
        filters.add(Filters.exists("fromMigrate", false));

        // 过滤db和collection
        String pattern = buildPattern();
        if (pattern != null) {
            filters.add(Filters.regex(MongodbEventHandler.EVENT_KEY_NS, pattern));
        }

        // 过滤系统日志
        filters.add(Filters.ne(MongodbEventHandler.EVENT_KEY_NS, "config.system.sessions"));

        // 过滤操作类型
        if (CollectionUtils.isNotEmpty(mongodbConfig.getOperateType())) {
            List<String> operateTypes = MongodbOperation.getInternalNames(mongodbConfig.getOperateType());
            filters.add(Filters.in(MongodbEventHandler.EVENT_KEY_OP, operateTypes));
        }

        return Filters.and(filters);
    }

    private String buildPattern() {
        if (CollectionUtils.isEmpty(mongodbConfig.getMonitorDatabases()) && CollectionUtils.isEmpty(mongodbConfig.getMonitorCollections())){
            return null;
        }

        StringBuilder pattern = new StringBuilder();
        if(CollectionUtils.isNotEmpty(mongodbConfig.getMonitorDatabases())){
            mongodbConfig.getMonitorDatabases().removeIf(StringUtils::isEmpty);
            if(CollectionUtils.isNotEmpty(mongodbConfig.getMonitorDatabases())){
                String databasePattern = StringUtils.join(mongodbConfig.getMonitorDatabases(), "|");
                pattern.append("(").append(databasePattern).append(")");
            } else {
                pattern.append(".*");
            }
        }

        pattern.append("\\.");

        if(CollectionUtils.isNotEmpty(mongodbConfig.getMonitorCollections())){
            mongodbConfig.getMonitorCollections().removeIf(String::isEmpty);
            if(CollectionUtils.isNotEmpty(mongodbConfig.getMonitorCollections())){
                String collectionPattern = StringUtils.join(mongodbConfig.getMonitorCollections(), "|");
                pattern.append("(").append(collectionPattern).append(")");
            } else {
                pattern.append(".*");
            }
        }

        return pattern.toString();
    }

    @Override
    public boolean reachedEnd() throws IOException {
        try {
            return !cursor.hasNext();
        } catch (Exception e) {
            // 这里出现异常可能是因为集群里某个节点挂了，所以不退出程序，调用openInternal方法重新连接，并从offset处开始同步数据，
            // 如果集群有问题，在openInternal方法里结束进程
            LOG.warn("获取数据异常,可能是某个节点出问题了，程序将自动重新选择节点连接", e);
            closeInternal();
            openInternal(inputSplit);

            return false;
        }
    }

    @Override
    public String nextRecord(String row) throws IOException {
        return nextRecordInternal(row);
    }

    private String nextRecordInternal(String row) throws IOException {
        return MongodbEventHandler.handleEvent(cursor.next(), offset, client);
    }

    @Override
    public void close() throws IOException {
        try{
            closeInternal();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void closeInternal() throws IOException {
        MongodbClientUtil.close(client, cursor);
    }
}
