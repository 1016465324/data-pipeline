package com.clinbrain.source;

import java.util.*;

public abstract class Reader {
    protected List<String> dbs;
    protected Integer batchSize;
    protected Map<String, String> start;
    protected List<Thread> threads;
    protected Properties props;

    public Reader(List<String> dbs, Integer batchSize, Properties props) {
        this.batchSize = batchSize;
        this.dbs = new ArrayList<>(0);
        this.dbs.addAll(dbs);
        this.props = props;
        this.threads = new ArrayList<>(0);
        this.start = new HashMap<>();
    }

    public abstract void run();

    protected abstract String getStartOffset(String db);

    public abstract void saveOffset(String db, String offset);

    public abstract void saveOffsetToDisk();

    public void stop(){
        for(Thread thread : threads){
            thread.interrupt();
        }
    }

    public abstract boolean isEmpty();

    public abstract Object poll();
}
