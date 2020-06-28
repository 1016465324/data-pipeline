package com.wheel.pool.database.cache;

import com.intersys.cache.jbind.JBindDatabase;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @ClassName DatabaseProxy
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 12:37 PM
 * @Version 1.0
 **/
public class DatabaseProxy implements InvocationHandler {
    private JBindDatabase realDatabase;
    private JBindDatabase proxyDatabase;
    private CacheDataSource cacheDataSource;
    
    public DatabaseProxy(JBindDatabase realDatabase, CacheDataSource cacheDataSource) {
        this.realDatabase = realDatabase;
        this.cacheDataSource = cacheDataSource;
        this.proxyDatabase = (JBindDatabase) Proxy.newProxyInstance(
                JBindDatabase.class.getClassLoader(),
                new Class<?>[] {JBindDatabase.class},
                this
        );
    }
    
    /**
     * 当调用JBindDatabase的方法都会先调用invoke方法
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //获取当前JBindDatabase对象执行的方法名
        String methodName = method.getName();
        //当方法为close时,不需要真正关闭,而是将其放回池中,其他方法正常执行
        if (StringUtils.endsWithIgnoreCase(methodName, "close")) {
            cacheDataSource.closeDatabase(this);
            return null;
        } else {
            return method.invoke(realDatabase, args);
        }
    }
    
    public JBindDatabase getRealDatabase() {
        return realDatabase;
    }
    
    public void setRealDatabase(JBindDatabase realDatabase) {
        this.realDatabase = realDatabase;
    }
    
    public JBindDatabase getProxyDatabase() {
        return proxyDatabase;
    }
    
    public void setProxyDatabase(JBindDatabase proxyDatabase) {
        this.proxyDatabase = proxyDatabase;
    }
    
    public CacheDataSource getCacheDataSource() {
        return cacheDataSource;
    }
    
    public void setCacheDataSource(CacheDataSource cacheDataSource) {
        this.cacheDataSource = cacheDataSource;
    }
}
