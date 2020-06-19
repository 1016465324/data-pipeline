package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
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
import java.util.Map;

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
        SchemaInfoSync schemaInfoSync = new SchemaInfoSync("cache.properties");
        schemaInfoSync.schemaInfoSync(false, false, false,
                false, false, true, true);
    }

    @Test
    public void globalManagerTest() {
        GlobalManager.buildGlobalManager();
    }

    @Test
    public void generateRuleTest() throws Exception {
        String url="jdbc:Cache://192.168.0.242:1972/%SYS";
        String username="_SYSTEM";
        String password="sys";

        Map<String, List<String>> globalDBMapNamespace = GlobalManager.buildGlobalDbMapNamespace(null);
        System.out.println(globalDBMapNamespace.size());
    }

    @Test
    public void testJson() {
        String json = "{\n" +
                "\t\"after\": {\n" +
                "\t\t\"chartitemid\": \"58\",\n" +
                "\t\t\"compositecode\": \"V187\",\n" +
                "\t\t\"episodeid\": \"148297481\",\n" +
                "\t\t\"id\": \"179426785\",\n" +
                "\t\t\"instancedataid\": \"6898850||1\",\n" +
                "\t\t\"patientid\": \"20865884\",\n" +
                "\t\t\"rowkey\": \"179426785\",\n" +
                "\t\t\"isdeleted\": 0\n" +
                "\t},\n" +
                "\t\"before\": null,\n" +
                "\t\"current_ts\": \"2020-06-12T15:30:22.500640\",\n" +
                "\t\"op_ts\": \"2020-06-12T15:23:19.000640\",\n" +
                "\t\"op_type\": \"I\",\n" +
                "\t\"pos\": \"691219640\",\n" +
                "\t\"primary_keys\": [\n" +
                "\t\t\"rowkey\"\n" +
                "\t],\n" +
                "\t\"table\": \"hid0101_cache_his_dhcapp_emrinstance.icompositedesc\"\n" +
                "}";
        JSONObject object = JSON.parseObject(json);
        System.out.println(JSON.toJSONString(object, SerializerFeature.MapSortField, SerializerFeature.WriteMapNullValue));
    }
}