package com.clinbrain;

import com.clinbrain.mapper.JournalOffsetMapper;
import com.clinbrain.model.JournalOffset;
import com.clinbrain.model.JournalRecord;
import com.clinbrain.util.Constans;
import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

/**
 * 日志解析启动类
 */
public class DataSyncParse {

    private static final Logger logger = LoggerFactory.getLogger(DataSyncParse.class);

    //程序上下文
    private Context context;

    public DataSyncParse() {
        context = new Context();
        addShutdownHook();
    }

    public static void main(String[] args) {
        try {
            DataSyncParse dataSyncParse = new DataSyncParse();
            ParseCacheLog parseCacheLog = new ParseCacheLog(dataSyncParse.getContext());
            Stopwatch stopwatch = Stopwatch.createUnstarted();
            while (true) {
                //获取当前解析的日志文件和偏移
                JournalOffset currentJournalOffset = dataSyncParse.getContext().getJournalOffset();
                String sql = String.format(Constans.query_sql,
                        currentJournalOffset.getJournalFilePath(), currentJournalOffset.getJournalFileIndex());
                logger.info("execute sql: {}", sql);
                stopwatch.start();
                //获取当批日志
                ResultSet resultSet = dataSyncParse.getContext().getCacheClient().executeQuery(sql);
                stopwatch.stop();
                logger.info("Query batch logs for: {}:{}. elapsed: {} ms", currentJournalOffset.getJournalFilePath(), currentJournalOffset.getJournalFileIndex(),
                        stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
                stopwatch.start();
                int recordCount = 0;
                while (resultSet.next()) {
                    recordCount++;
                    JournalRecord journalRecord = new JournalRecord(resultSet);
//                    System.out.println(String.format("%s -> address: %s recordCount: %d",
//                            currentJournalOffset.getJournalFilePath(), journalRecord.getAddress(), recordCount));
                    //处理当条数据
                    parseCacheLog.parseJournalRecord(journalRecord);
                    //批处理下标记录入缓存，异常中断时执行钩子函数存入数据库
                    currentJournalOffset.setJournalFileIndex(journalRecord.getAddress());
                    dataSyncParse.getContext().setJournalOffset(currentJournalOffset);
                }
                stopwatch.stop();
                logger.info("End deal with batch logs, elapsed: {} ms, record count: {} .",
                        stopwatch.elapsed(TimeUnit.MILLISECONDS), recordCount);
                stopwatch.reset();

                dataSyncParse.getContext().getCacheClient().close();

                //获取下一个需要解析的日志文件
                String nextJournalFile = dataSyncParse.context.findNextJournalFile(currentJournalOffset.getJournalFilePath());
                if (StringUtils.equalsIgnoreCase(currentJournalOffset.getJournalFilePath(), nextJournalFile)) {
                    logger.info("current journal file {} is writing.", currentJournalOffset.getJournalFilePath());
                } else {
                    logger.info("current journal file {} parse over, begin to parse {}", currentJournalOffset.getJournalFilePath(), nextJournalFile);
                    currentJournalOffset.setJournalFilePath(nextJournalFile);
                    currentJournalOffset.setJournalFileIndex("0");
                }
            }
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

    public Context getContext() {
        return context;
    }
}