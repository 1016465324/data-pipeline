package com.clinbrain;

import com.clinbrain.mapper.JournalOffsetMapper;
import com.clinbrain.producer.JournalProducer;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @ClassName DataSync
 * @Description TODO
 * @Author p
 * @Date 2020/3/10 10:37
 * @Version 1.0
 **/
public class DataSync {
    private static final Logger logger = LoggerFactory.getLogger(DataSync.class);
    
    //程序上下文
    private SyncContext context;
    
    public DataSync(String confPath) {
        context = new SyncContext(confPath);
        addShutdownHook();
    }
    
    public static void main(String[] args) {
        if (1 != args.length) {
            System.out.println("    param error. example: ");
            System.out.println("        data-sync conf_path");
            return;
        }

        String confPath = args[0];
        try {
            PropertyConfigurator.configure(confPath + File.separator + "log4j.properties");

            DataSync dataSyncParse = new DataSync(confPath);
            JournalProducer journalProducer = new JournalProducer(dataSyncParse.getContext());
            journalProducer.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 在程序结束或者出现异常时保存当前处理的偏移量
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                SqlSession session = context.getDatahubSessionFactory().openSession();
                JournalOffsetMapper journalOffsetMapper = session.getMapper(JournalOffsetMapper.class);
                if (null == context.getJournalOffset().getId()) {
                    journalOffsetMapper.insert(context.getJournalOffset());
                } else {
                    journalOffsetMapper.updateByPrimaryKey(context.getJournalOffset());
                }
                session.commit();
                session.close();
            }
        }));
    }
    
    public SyncContext getContext() {
        return context;
    }
}
