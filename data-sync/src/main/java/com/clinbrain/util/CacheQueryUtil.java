package com.clinbrain.util;

import com.intersys.cache.Dataholder;
import com.intersys.cache.jbind.JBindDatabase;
import com.intersys.objects.CacheException;
import com.wheel.pool.database.common.BasicClient;

import java.sql.Connection;

import static com.intersys.objects.Database.RET_PRIM;

/**
 * @ClassName CacheQueryUtil
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 4:14 PM
 * @Version 1.0
 **/
public class CacheQueryUtil {
    /**
     * 使用前需要初始化
     */
    public static BasicClient cacheClient = null;
    
    /**
     * 获取下一个日志文件
     * @param filePath
     * @return
     * @throws Exception
     */
    public static String getNextJournalFile(String filePath) throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(filePath);
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetNextJournalFile", args, RET_PRIM);
        connection.close();
        return res.toString();
    }
    
    /**
     * 获取当前的日志文件
     * @return
     * @throws Exception
     */
    public static String getCurrentJournalFile() throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[0];
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetCurrentJournalFile", args, RET_PRIM);
        connection.close();
        return res.toString();
    }
    
    /**
     * 获取所有的日志文件
     * @return
     * @throws Exception
     */
    public static String getJournalFileList() throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[0];
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetJournalFileList", args, RET_PRIM);
        connection.close();
        return res.toString();
    }
    
    public static String getGlobals(String request) throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(request);
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetGlobals", args, RET_PRIM);
        connection.close();
        return res.getString();
    }
    
    public static String getDbs() throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[0];
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetDBs", args, RET_PRIM);
        connection.close();
        return res.getString();
    }
    
    
    public static String getNameSpaces() throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[0];
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "GetNameSpaces", args, RET_PRIM);
        connection.close();
        return res.getString();
    }
    
    /**
     * 反查cahce库接口
     * @param queryParam
     * @return
     * @throws CacheException
     */
    public static String queryBatchGlobalValue(String queryParam) throws Exception {
        Connection connection = cacheClient.getConnection();
        JBindDatabase database = JBindDatabase.getDatabase(connection);
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(queryParam);
        Dataholder res = database.runClassMethod("ClinBrain.CacheBasic", "QueryGlobals", args, RET_PRIM);
        connection.close();
        return res.toString();
    }
}
