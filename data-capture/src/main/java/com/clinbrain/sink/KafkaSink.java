package com.clinbrain.sink;

import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Properties;

public class KafkaSink implements Serializable {
    private Logger logger = LoggerFactory.getLogger(KafkaSink.class);

    private String topic;
    private Properties props;
    private KafkaProducer<String, String> kafkaProducer;

    public KafkaSink(Properties props) {
        topic = props.getProperty("topics");
        this.props = props;
    }

    private Properties getProducerProps() {
        //kerberos环境下,需额外加载如下两项配置文件
        System.setProperty("java.security.auth.login.config", "/home/clin/capture/config/kafka_client_jaas.conf");
        System.setProperty("java.security.krb5.conf", "/home/clin/capture/config/krb5.conf");

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        producerProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        //kerberos环境下,需额外添加如下三项配置
        producerProps.put("security.protocol", "SASL_PLAINTEXT");
        producerProps.put("sasl.mechanism", "GSSAPI");
        producerProps.put("sasl.kerberos.service.name", "kafka");


        return producerProps;
    }

    private KafkaProducer<String, String> getProducer() {
        if (null == kafkaProducer) {
            synchronized (this) {
                if (null == kafkaProducer) {
                    Properties producerProps = getProducerProps();
                    kafkaProducer = new KafkaProducer<>(producerProps);
                }
            }
        }

        return kafkaProducer;
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
            logger.warn("retry one times.");
            try {
                sendInternal(topic, message, key);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }

        }
    }

    private void send(String topic, int partition, String message, String key) {
        try {
            sendInternal(topic, partition, message, key);
        } catch (Exception e) {
            logger.warn("retry one times.");
            try {
                sendInternal(topic, partition, message, key);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    public void close() {
        try {
            if (null != kafkaProducer) {
                kafkaProducer.close();
            }
        } catch (Exception e) {
            logger.error("close kafka producer error. reason: {}", e.getMessage());
            throw e;
        }

    }

    private void sendInternal(String topic, String message, String key) {
        if (null != message) {
            try {
                getProducer().send(new ProducerRecord<>(topic, key, message), new ProducerCallback(message));
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
                getProducer().send(new ProducerRecord<>(topic, partition, key, message), new ProducerCallback(message));
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
                e.printStackTrace(); //2
                logger.error("message: (" + message + ") send error.");
                logger.error(e.getMessage());
            }
        }
    }
}
