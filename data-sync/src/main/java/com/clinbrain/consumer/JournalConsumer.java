package com.clinbrain.consumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @ClassName JournalConsumer
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 2:24 PM
 * @Version 1.0
 **/
public class JournalConsumer {
    private ExecutorService executorService;

    public JournalConsumer(int threadSize) {
        executorService = Executors.newFixedThreadPool(threadSize);
    }
    
    public void accept(JournalParseTask journalParseTask) {
        executorService.submit(journalParseTask);
    }
}
