package com.clinbrain;

import com.alibaba.fastjson.JSONObject;
import com.clinbrain.source.mongo.MongodbConfig;
import com.clinbrain.source.mongo.MongodbOplogInputFormat;
import com.clinbrain.util.UtilHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.io.InputFormat;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @ClassName DataSync
 * @Description TODO
 * @Author p
 * @Date 2020/5/21 10:57
 * @Version 1.0
 **/
public class DataSync {
    private static Logger LOG = LoggerFactory.getLogger(DataSync.class);

    private static final String KERBEROS = "kerberos";
    private static final String SECURITY_PROTOCOL = "security.protocol";
    private static final String SASL_MECHANISM = "sasl.mechanism";
    private static final String SASL_KERBEROS_SERVICE_NAME = "sasl.kerberos.service.name";
    private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
    private static final String JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf";

    public static void main(String[] args) throws Exception {
        if (2 != args.length) {
            System.out.println("example: ");
            System.out.println("    DataSync config.properties db.properties");
            return;
        }

        Properties flinkProps = UtilHelper.loadProperties(args[0]);
        Properties dbProps = UtilHelper.loadProperties(args[1]);

        String databaseType = flinkProps.getProperty("database.type");
        boolean localTest = Boolean.parseBoolean(flinkProps.getProperty("local.test"));
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env = openCheckpointConf(env, flinkProps);
        env.setParallelism(Integer.parseInt(flinkProps.getProperty("flink.parallelism")));

        InputFormat<String, InputSplit> inputFormat = createInputFormat(databaseType, dbProps);
        TypeInformation<String> typeInfo = TypeExtractor.getInputFormatTypes(inputFormat);
        DataStreamSource<String> source = env.createInput(inputFormat, typeInfo);

        SingleOutputStreamOperator<String> messageSteam = source.filter((FilterFunction<String>) o -> !StringUtils.equalsIgnoreCase(o.toString(), "null"));
        if (localTest) {
            messageSteam.print();
        } else {
            Properties producerProps = initProducerProps(flinkProps);
            String topic = flinkProps.getProperty("kafka.target.topic");
            int partitionNumber = Integer.parseInt(flinkProps.getProperty("kafka.target.topic.partition"));
            FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(topic,
                    new ProducerKafkaSerializationSchema(topic, partitionNumber),
                    producerProps, FlinkKafkaProducer.Semantic.EXACTLY_ONCE);
            source.addSink(kafkaSink);
        }

        env.execute(String.format("%s stream", databaseType));
    }

    private static Properties initProducerProps(Properties props) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        producerProps.put(ProducerConfig.ACKS_CONFIG, props.getProperty(ProducerConfig.ACKS_CONFIG));
        producerProps.put(ProducerConfig.RETRIES_CONFIG, props.getProperty(ProducerConfig.RETRIES_CONFIG));
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, props.getProperty(ProducerConfig.BATCH_SIZE_CONFIG));
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, props.getProperty(ProducerConfig.LINGER_MS_CONFIG));
        producerProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, props.getProperty(ProducerConfig.BUFFER_MEMORY_CONFIG));
        producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, props.getProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, props.getProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, props.getProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
        //kerberos环境下,需额外添加如下配置
        if (Boolean.parseBoolean(props.getProperty(KERBEROS))) {
            System.setProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG, props.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG));
            System.setProperty(JAVA_SECURITY_KRB5_CONF, props.getProperty(JAVA_SECURITY_KRB5_CONF));

            producerProps.put(SECURITY_PROTOCOL, props.getProperty(SECURITY_PROTOCOL));
            producerProps.put(SASL_MECHANISM, props.getProperty(SASL_MECHANISM));
            producerProps.put(SASL_KERBEROS_SERVICE_NAME, props.getProperty(SASL_KERBEROS_SERVICE_NAME));
        }

        return producerProps;
    }

    private static InputFormat<String, InputSplit> createInputFormat(String dbType, Properties dbProps) {
        InputFormat<String, InputSplit> inputFormat = null;
        if (StringUtils.equalsIgnoreCase(dbType, "mongo")) {
            MongodbConfig mongodbConfig = new MongodbConfig(dbProps);
            inputFormat = new MongodbOplogInputFormat(mongodbConfig);
        } else if (StringUtils.equalsIgnoreCase(dbType, "cache")) {

        } else {
            throw new RuntimeException("database type error. need mongo cache");
        }

        return inputFormat;
    }

    private static StreamExecutionEnvironment openCheckpointConf(StreamExecutionEnvironment env, Properties properties) {
        if (properties != null) {
            String interval = properties.getProperty("flink.checkpoint.interval");
            if (StringUtils.isNotBlank(interval)) {
                env.enableCheckpointing(Long.parseLong(interval.trim()));
                LOG.info("Open checkpoint with interval:" + interval);
            }

            String checkpointPath = properties.getProperty("flink.checkpoint.path");
            if (StringUtils.isNotEmpty(checkpointPath)) {
                env.setStateBackend(new FsStateBackend(checkpointPath));
                LOG.info("Save checkpoint to path:" + checkpointPath);
            }

            String checkpointTimeoutStr = properties.getProperty("flink.checkpoint.timeout");
            if (checkpointTimeoutStr != null) {
                long checkpointTimeout = Long.parseLong(checkpointTimeoutStr.trim());
                //checkpoints have to complete within one min,or are discard
                env.getCheckpointConfig().setCheckpointTimeout(checkpointTimeout);

                LOG.info("Set checkpoint timeout:" + checkpointTimeout);
            }

            env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
            env.getCheckpointConfig().enableExternalizedCheckpoints(
                    CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        }

        return env;
    }

    public static class ProducerKafkaSerializationSchema implements KafkaSerializationSchema<String> {
        private String topic;
        private int partitionNumber;

        ProducerKafkaSerializationSchema(String topic, int partitionNumber) {
            this.topic = topic;
            this.partitionNumber = partitionNumber;
        }

        @Override
        public ProducerRecord<byte[], byte[]> serialize(String element, Long timestamp) {
            JSONObject oggMessage = JSONObject.parseObject(element);
            String table = oggMessage.getString("table");
            return new ProducerRecord<>(topic, customPartition(table),
                    table.getBytes(StandardCharsets.UTF_8),
                    element.getBytes(StandardCharsets.UTF_8));
        }

        private int customPartition(String customValue) {
            int h;
            int hash = (customValue == null) ? 0 : (h = customValue.hashCode()) ^ (h >>> 16);
            return (partitionNumber - 1) & hash;
        }
    }
}
