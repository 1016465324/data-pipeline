package com.clinbrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author p
 */
public class DataCapture {
    private static Logger logger = LoggerFactory.getLogger(DataCapture.class);

    private Context context;

    private DataCapture(String[] args) {
        context = new Context(loadProperties(args[0]));
    }

    static Properties loadProperties(String filePath) {
        Properties props = new Properties();
        FileInputStream input = null;
        try {
            input = new FileInputStream(filePath);
            props.load(input);
        } catch (Exception e) {
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

        return props;
    }

    public void start() {
        context.start();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (context.getReader() != null) {
                context.getReader().saveOffsetToDisk();
            }
        }));
    }

    public static void main(String[] args) {
        if (1 != args.length) {
            logger.error("usage: ");
            logger.error("    DataCapture.jar config.properties");
            return;
        }

        logger.info("start data capture.");
        DataCapture dataCapture = new DataCapture(args);
        dataCapture.addShutdownHook();
        dataCapture.start();
    }
}
