package com.clinbrain.sink;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @ClassName KafkaSink
 * @Description 将数据库记录发送的消息队列中
 * @Author p
 * @Date 2020/2/19 10:20
 * @Version 1.0
 **/
public class KafkaSink {
    private static Logger logger = LoggerFactory.getLogger(KafkaSink.class);

    private static final String KERBEROS = "kerberos";
    private static final String SECURITY_PROTOCOL = "security.protocol";
    private static final String SASL_MECHANISM = "sasl.mechanism";
    private static final String SASL_KERBEROS_SERVICE_NAME = "sasl.kerberos.service.name";
    private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
    private static final String JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf";

    private KafkaProducer<String, String> kafkaProducer;


    public KafkaSink() {

    }

    public KafkaSink(Properties props) {
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
        producerProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "10485760");
//        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        //kerberos环境下,需额外添加如下配置
        if (Boolean.parseBoolean(props.getProperty(KERBEROS))) {
            System.setProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG, props.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG));
            System.setProperty(JAVA_SECURITY_KRB5_CONF, props.getProperty(JAVA_SECURITY_KRB5_CONF));

            producerProps.put(SECURITY_PROTOCOL, props.getProperty(SECURITY_PROTOCOL));
            producerProps.put(SASL_MECHANISM, props.getProperty(SASL_MECHANISM));
            producerProps.put(SASL_KERBEROS_SERVICE_NAME, props.getProperty(SASL_KERBEROS_SERVICE_NAME));
        }

        kafkaProducer = new KafkaProducer<>(producerProps);
    }

    public void sendMessage(String topic, String message, String key) {
       send(topic, message, key);
    }

    public void sendMessage(String topic, int partition, String message, String key) {
        send(topic, partition, message, key);
    }

    private void send(String topic, String message, String key) {
        try {
            sendInternal(topic, message, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void send(String topic, int partition, String message, String key) {
        try {
            sendInternal(topic, partition, message, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            if (null != kafkaProducer) {
                kafkaProducer.close();
            }
        } catch (Exception e) {
            logger.error("close kafka producer error. reason: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendInternal(String topic, String message, String key) {
        if (null != message) {
            try {
                kafkaProducer.send(new ProducerRecord<>(topic, key, message), new ProducerCallback(message));
            } catch (Exception e) {
                logger.error("send ERROR. reason: {}", e.getMessage());
                try {
                    close();
                } catch (Exception e1) {
                    logger.error("close ERROR.");
                }

                kafkaProducer = null;
                throw e;
            }
        }
    }

    private void sendInternal(String topic, int partition, String message, String key) {
        if (null != message) {
            try {
                kafkaProducer.send(new ProducerRecord<>(topic, partition, key, message), new ProducerCallback(message));
            } catch (Exception e) {
                logger.error("send ERROR. reason: {}", e.getMessage());
                try {
                    close();
                } catch (Exception e1) {
                    logger.error("close ERROR.");
                }

                kafkaProducer = null;
                throw e;
            }
        }
    }

    private static class ProducerCallback implements Callback {
        private Logger logger = LoggerFactory.getLogger(ProducerCallback.class);
        private String message;

        public ProducerCallback(String message) {
            this.message = message;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e != null) {
                e.printStackTrace();
                logger.error("message: (" + message + ") send error.");
                logger.error(e.getMessage());
            }
        }
    }
}
