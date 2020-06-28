package com.clinbrain.producer;

import com.clinbrain.SyncContext;
import com.clinbrain.consumer.JournalParseTask;
import com.clinbrain.model.JournalOffset;
import com.clinbrain.model.JournalRecord;
import com.google.common.base.Stopwatch;
import com.wheel.util.UtilHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName JournalProducer
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 2:23 PM
 * @Version 1.0
 **/
public class JournalProducer {
    private final Logger logger = LoggerFactory.getLogger(JournalProducer.class);
    
    private final String JOURNAL_PRODUCER_SQL = "select * from ClinBrain.JournalTail_RecordList('%s', %s)";
    
    private SyncContext context;
    
    public JournalProducer(SyncContext context) {
        this.context = context;
    }
    
    public void start() throws Exception {
        List<JournalParseTask> allJournalParseTask = new LinkedList<>();
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        while (true)  {
            //获取当前解析的日志文件和偏移
            JournalOffset currentJournalOffset = context.getJournalOffset();
            String sql = String.format(JOURNAL_PRODUCER_SQL,
                    currentJournalOffset.getJournalFilePath(), currentJournalOffset.getJournalFileIndex());
            logger.info("execute sql: {}", sql);
            stopwatch.start();
            //获取当批日志
            ResultSet resultSet = context.getCacheClient().executeQuery(sql);
            stopwatch.stop();
            logger.info("Query batch logs for: {}:{}. elapsed: {} ms", currentJournalOffset.getJournalFilePath(), currentJournalOffset.getJournalFileIndex(),
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();
            stopwatch.start();
            int recordCount = 0;
            while (resultSet.next()) {
                recordCount++;
                JournalRecord journalRecord = new JournalRecord(resultSet);
                //journal日志分发
                JournalParseTask journalParseTask = new JournalParseTask(context, journalRecord);
                journalRecordDistribute(journalParseTask);

                allJournalParseTask.add(journalParseTask);
            }
            
            while (context.getCurrentDealRecordSize().intValue() != recordCount) {
                Thread.sleep(1000);
            }
    
            context.getCurrentDealRecordSize().set(0);
            allJournalParseTask.forEach(o -> {
                if (StringUtils.isNotEmpty(o.getOggMessage()) && !context.isLocalTest()) {
                    //发送kafka
                    context.getKafkaSink().sendMessage(o.getTopicName(), o.getPartitionIndex(),
                            o.getOggMessage(), o.getGlobalNode());

                }

                //批处理下标记录入缓存，异常中断时执行钩子函数存入数据库
                currentJournalOffset.setJournalFileIndex(o.getCurrentJournalRecord().getAddress());
            });
            allJournalParseTask.clear();
            
            stopwatch.stop();
            logger.info("End deal with batch logs, elapsed: {} ms, record count: {} .",
                    stopwatch.elapsed(TimeUnit.MILLISECONDS), recordCount);
            stopwatch.reset();
    
            context.getCacheClient().close();
    
            //获取下一个需要解析的日志文件
            String nextJournalFile = context.findNextJournalFile(currentJournalOffset.getJournalFilePath());
            if (StringUtils.equalsIgnoreCase(currentJournalOffset.getJournalFilePath(), nextJournalFile)) {
                Thread.sleep(1000);
                logger.info("current journal file {} is writing.", currentJournalOffset.getJournalFilePath());
            } else {
                logger.info("current journal file {} parse over, begin to parse {}", currentJournalOffset.getJournalFilePath(), nextJournalFile);
                currentJournalOffset.setJournalFilePath(nextJournalFile);
                currentJournalOffset.setJournalFileIndex("0");
            }
        }
    }
    
    /**
     *
     * @param journalParseTask
     */
    private void journalRecordDistribute(JournalParseTask journalParseTask) {
        context.getJournalConsumer().accept(journalParseTask);
    }
}
