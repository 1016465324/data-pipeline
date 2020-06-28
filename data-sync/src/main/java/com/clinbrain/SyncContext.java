package com.clinbrain;

import com.alibaba.fastjson.JSONArray;
import com.clinbrain.config.cache.GlobalManager;
import com.clinbrain.consumer.JournalConsumer;
import com.clinbrain.mapper.EtlPartitionConfMapper;
import com.clinbrain.mapper.JournalOffsetMapper;
import com.clinbrain.model.EtlPartitionConf;
import com.clinbrain.model.JournalOffset;
import com.clinbrain.sink.KafkaSink;
import com.clinbrain.util.CacheQueryUtil;
import com.wheel.pool.database.cache.CacheDataSource;
import com.wheel.pool.database.common.BasicClient;
import com.wheel.pool.database.common.CacheClient;
import com.wheel.util.UtilHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName SyncContext
 * @Description TODO
 * @Author agui
 * @Date 2020/6/21 12:09 PM
 * @Version 1.0
 **/
public class SyncContext {
    /**
     * 程序所需的配置
     */
    private Properties appProps;
    /**
     * kafka配置
     */
    private Properties kafkaProps;
    /**
     * cache 连接池
     */
    private BasicClient cacheClient;
    /**
     * cache JBindDatabase 连接
     */
    private CacheDataSource cacheDataSource;
    /**
     * 程序当前处理偏移
     */
    private JournalOffset journalOffset;
    /**
     * mysql datahub数据库的SqlSessionFactory
     */
    private SqlSessionFactory datahubSessionFactory;
    /**
     * ETL分区表配置
     */
    private Map<String, EtlPartitionConf> allEtlPartitionConf;
    /**
     * cache所有namespace的Global配置
     */
    private Map<String, GlobalManager> allGlobalManager;
    /**
     * global + database 与 namespace的映射
     */
    private Map<String, List<String>> allGlobalDbMapNamespace;
    /**
     * cache需要处理的global的名字
     */
    private Set<String> allGlobalName;
    /**
     * 发往kafka
     */
    private KafkaSink kafkaSink;
    /**
     * 本地测试
     */
    private boolean localTest;
    
    private JournalConsumer journalConsumer;
    
    private AtomicInteger currentDealRecordSize;
    
    public SyncContext(String confPath) {
        init(confPath);
    }
    
    private void init(String confPath) {
        try {
            appProps = UtilHelper.loadProperties(confPath + File.separator + "application.properties");
            kafkaProps = UtilHelper.loadProperties(confPath + File.separator + "mq.properties");
            cacheClient = new CacheClient(appProps.getProperty("cache.url"), appProps.getProperty("cache.username"), appProps.getProperty("cache.password"));
            CacheQueryUtil.cacheClient = new CacheClient(appProps.getProperty("cache.url"), appProps.getProperty("cache.username"), appProps.getProperty("cache.password"));
            
            initDataHub();
            initEtlTablePartition();
            
            allGlobalManager = GlobalManager.buildGlobalManager(datahubSessionFactory);
            allGlobalDbMapNamespace = GlobalManager.buildGlobalDbMapNamespace();
            allGlobalName = GlobalManager.buildAllGlobalName(datahubSessionFactory);
            
            localTest = Boolean.parseBoolean(kafkaProps.getProperty("local.test"));
            if (!localTest) {
                kafkaSink = new KafkaSink(kafkaProps);
            }
    
            int journalConsumerSize = Integer.parseInt(appProps.getProperty("journal.consumer.size"));
            journalConsumer = new JournalConsumer(journalConsumerSize);
    
            currentDealRecordSize = new AtomicInteger(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 初始化程序的起始处理文件路径和偏移
     */
    private void initDataHub() {
        SqlSession sqlSession = null;
        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
            datahubSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            IOUtils.closeQuietly(inputStream);

            sqlSession = datahubSessionFactory.openSession();
            JournalOffsetMapper journalOffsetMapper = sqlSession.getMapper(JournalOffsetMapper.class);
            List<JournalOffset> journalOffsets = journalOffsetMapper.selectAllJournalOffset();
            if (journalOffsets.isEmpty()) {
                journalOffset = new JournalOffset();
                journalOffset.setJournalFilePath(CacheQueryUtil.getCurrentJournalFile());
                journalOffset.setJournalFileIndex("0");
            } else {
                journalOffset = journalOffsets.get(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (null != sqlSession) {
                sqlSession.close();
            }
        }
    }
    
    /**
     * 初始化cache表的分区配置
     */
    private void initEtlTablePartition() {
        SqlSession etlSqlSession = null;
        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config-mysql-etlPartitionsConf.xml");
            SqlSessionFactory buildEtl = new SqlSessionFactoryBuilder().build(inputStream);
            IOUtils.closeQuietly(inputStream);

            etlSqlSession = buildEtl.openSession();
            EtlPartitionConfMapper partitionMapper = etlSqlSession.getMapper(EtlPartitionConfMapper.class);
            
            allEtlPartitionConf = new HashMap<>(128);
            partitionMapper.selectAllPartitionConf().forEach(o -> {
                if (null != o.getHisDbName() && null != o.getHisTbName()) {
                    allEtlPartitionConf.put(String.format("%s.%s", o.getHisDbName().toLowerCase(),
                            o.getHisTbName().toLowerCase()), o);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (null != etlSqlSession) {
                etlSqlSession.close();
            }
        }
    }
    
    /**
     * 获取下一个需要处理的日志文件
     * @param currentJournalFilePath
     * @return
     * @throws Exception
     */
    public String findNextJournalFile(String currentJournalFilePath) throws Exception {
        String journalFileListStr = CacheQueryUtil.getJournalFileList();
        JSONArray objects = JSONArray.parseArray(journalFileListStr);
        if (objects.isEmpty()) {
            return currentJournalFilePath;
        } else {
            int i = 0;
            for (; i < objects.size(); i++) {
                String journalFilePath = objects.getJSONObject(i).getString("Name");
                if (StringUtils.equalsIgnoreCase(journalFilePath, currentJournalFilePath)) {
                    break;
                }
            }
            
            if (0 == i) {
                return currentJournalFilePath;
            } else {
                return objects.getJSONObject(i - 1).getString("Name");
            }
        }
        
    }
    
    public Properties getAppProps() {
        return appProps;
    }
    
    public void setAppProps(Properties appProps) {
        this.appProps = appProps;
    }
    
    public Properties getKafkaProps() {
        return kafkaProps;
    }
    
    public void setKafkaProps(Properties kafkaProps) {
        this.kafkaProps = kafkaProps;
    }
    
    public BasicClient getCacheClient() {
        return cacheClient;
    }
    
    public void setCacheClient(BasicClient cacheClient) {
        this.cacheClient = cacheClient;
    }
    
    public CacheDataSource getCacheDataSource() {
        return cacheDataSource;
    }
    
    public void setCacheDataSource(CacheDataSource cacheDataSource) {
        this.cacheDataSource = cacheDataSource;
    }
    
    public JournalOffset getJournalOffset() {
        return journalOffset;
    }
    
    public void setJournalOffset(JournalOffset journalOffset) {
        this.journalOffset = journalOffset;
    }
    
    public SqlSessionFactory getDatahubSessionFactory() {
        return datahubSessionFactory;
    }
    
    public void setDatahubSessionFactory(SqlSessionFactory datahubSessionFactory) {
        this.datahubSessionFactory = datahubSessionFactory;
    }
    
    public Map<String, EtlPartitionConf> getAllEtlPartitionConf() {
        return allEtlPartitionConf;
    }
    
    public void setAllEtlPartitionConf(Map<String, EtlPartitionConf> allEtlPartitionConf) {
        this.allEtlPartitionConf = allEtlPartitionConf;
    }
    
    public Map<String, GlobalManager> getAllGlobalManager() {
        return allGlobalManager;
    }
    
    public void setAllGlobalManager(Map<String, GlobalManager> allGlobalManager) {
        this.allGlobalManager = allGlobalManager;
    }
    
    public Map<String, List<String>> getAllGlobalDbMapNamespace() {
        return allGlobalDbMapNamespace;
    }
    
    public void setAllGlobalDbMapNamespace(Map<String, List<String>> allGlobalDbMapNamespace) {
        this.allGlobalDbMapNamespace = allGlobalDbMapNamespace;
    }
    
    public Set<String> getAllGlobalName() {
        return allGlobalName;
    }
    
    public void setAllGlobalName(Set<String> allGlobalName) {
        this.allGlobalName = allGlobalName;
    }
    
    public KafkaSink getKafkaSink() {
        return kafkaSink;
    }
    
    public void setKafkaSink(KafkaSink kafkaSink) {
        this.kafkaSink = kafkaSink;
    }
    
    public boolean isLocalTest() {
        return localTest;
    }
    
    public void setLocalTest(boolean localTest) {
        this.localTest = localTest;
    }

    public JournalConsumer getJournalConsumer() {
        return journalConsumer;
    }

    public void setJournalConsumer(JournalConsumer journalConsumer) {
        this.journalConsumer = journalConsumer;
    }

    public AtomicInteger getCurrentDealRecordSize() {
        return currentDealRecordSize;
    }
    
    public void setCurrentDealRecordSize(AtomicInteger currentDealRecordSize) {
        this.currentDealRecordSize = currentDealRecordSize;
    }
}
