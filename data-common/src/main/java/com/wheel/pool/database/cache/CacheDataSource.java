package com.wheel.pool.database.cache;

import com.intersys.cache.jbind.JBindDatabase;
import com.intersys.objects.CacheException;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CacheDataSource
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 12:09 PM
 * @Version 1.0
 **/
public class CacheDataSource {
    
    private String url;
    private String username;
    private String password;
    
    private List<DatabaseProxy> idleDatabases = new ArrayList<>();
    private List<DatabaseProxy> activeDatabases = new ArrayList<>();
    private int poolMaxActiveDatabases = 8;
    private int poolMaxIdleDatabases = 4;
    /**
     * 从连接池中获取一个连接最大等待时间，单位为ms
     */
    private int poolTimeToWait = 30000;
    
    /**
     * 监视器对象
     */
    private final Object monitor = new Object();
    
    private final Object watch = new Object();
    
    public CacheDataSource(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    
    public JBindDatabase getDatabase() throws CacheException {
        DatabaseProxy databaseProxy = getDatabaseProxy(username, password);
        return databaseProxy.getProxyDatabase();
    }
    
    public DatabaseProxy getDatabaseProxy(String username, String password) throws CacheException {
        DatabaseProxy databaseProxy = null;
        while (null == databaseProxy) {
            synchronized (monitor) {
                if (idleDatabases.isEmpty()) {
                    if (activeDatabases.size() < poolMaxActiveDatabases) {
                        databaseProxy = new DatabaseProxy(new JBindDatabase(url, username, password), this);
                    }
                } else {
                    databaseProxy = idleDatabases.remove(0);
                }
            }
            
            if (null == databaseProxy) {
                try {
                    monitor.wait(poolTimeToWait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            } else {
                activeDatabases.add(databaseProxy);
            }
        }
        
        return databaseProxy;
    }
    
    public void closeDatabase(DatabaseProxy databaseProxy) {
        synchronized (watch) {
            activeDatabases.remove(databaseProxy);
            
            if (idleDatabases.size() < poolMaxIdleDatabases) {
                idleDatabases.add(databaseProxy);
            }
            
            monitor.notifyAll();
        }
    }
}
