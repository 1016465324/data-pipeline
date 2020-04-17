package com.clinbrain;

import com.clinbrain.mapper.cache.CacheClassDefineMapper;
import com.clinbrain.mapper.cache.CacheClassPropertyDefineMapper;
import com.clinbrain.model.cache.CacheClassDefine;
import com.clinbrain.model.cache.CacheClassPropertyDefine;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * @ClassName SchemaInfoSyncTest
 * @Description TODO
 * @Author p
 * @Date 2020/3/30 16:19
 * @Version 1.0
 **/
public class SchemaInfoSyncTest {
    private CacheClassDefineMapper cacheClassDefineMapper;
    private CacheClassPropertyDefineMapper cacheClassPropertyDefineMapper;
    private SqlSession sqlSession;

    @Before
    public void setUp() throws Exception {
        InputStream is = Resources.getResourceAsStream("mybatis-config-cache-samples.xml");
        // 构建SqlSessionFactory
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        // 获取sqlSession
        sqlSession = sqlSessionFactory.openSession();
        cacheClassDefineMapper = sqlSession.getMapper(CacheClassDefineMapper.class);
        cacheClassPropertyDefineMapper = sqlSession.getMapper(CacheClassPropertyDefineMapper.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void schemaInfoSync() {
        List<CacheClassDefine> classDefines = cacheClassDefineMapper.selectAllClassDefine();
        System.out.println(classDefines.size());
        List<CacheClassPropertyDefine> cacheClassPropertyDefines = cacheClassPropertyDefineMapper.selectAllClassPropertyDefine();
        System.out.println(cacheClassPropertyDefines.size());
    }

    @Test
    public void schemaInfoSyncTest() {
        SchemaInfoSync schemaInfoSync = new SchemaInfoSync(null);
        schemaInfoSync.schemaInfoSync();
    }

//    @Test
//    public void generateRuleTest() {
//        GlobalManager globalManager = new GlobalManager();
//        globalManager.generateRule();
//    }
}