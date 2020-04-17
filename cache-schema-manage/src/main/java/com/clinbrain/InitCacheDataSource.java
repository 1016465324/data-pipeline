package com.clinbrain;

import com.clinbrain.mapper.DataSourceMapper;
import com.clinbrain.model.DataSource;
import com.clinbrain.util.UtilHelper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * @ClassName InitCacheDataSource
 * @Description TODO
 * @Author p
 * @Date 2020/3/25 15:10
 * @Version 1.0
 **/
public class InitCacheDataSource {
    private static Logger logger = LoggerFactory.getLogger(InitCacheDataSource.class);

    public static void initCacheDataSource() {
        Properties props = UtilHelper.loadProperties(null);

        try {
            InputStream inputStream = Resources.getResourceAsStream("mybatis-config-mysql-datahub.xml");
            // 构建SqlSessionFactory
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
            // 获取sqlSession
            SqlSession sqlSession = sqlSessionFactory.openSession();
            DataSourceMapper dataSourceMapperService = sqlSession.getMapper(DataSourceMapper.class);

            String[] namespaces = props.getProperty("cache_namespace").split(",");
            String[] accounts = props.getProperty("cache_namespace_account").split(":");
            for (int i = 0; i < namespaces.length; i++) {
                String[] fields = accounts[i].split(",");
                String user = fields[0];
                String password = fields[1];

                DataSource dataSource = new DataSource();
                dataSource.setDsType("cache");
                dataSource.setDsName(namespaces[i]);
                dataSource.setInstanceName(namespaces[i]);
                dataSource.setDsDesc("namespace " + namespaces[i]);
                dataSource.setStatus("active");
                dataSource.setSchemaTopic("");
                dataSource.setSplitTopic("");
                Date date = new Date();
                dataSource.setCreateTime(date);
                dataSource.setUpdateTime(date);
                String slaveUrl = String.format("jdbc:Cache://%s:%s/%s", props.getProperty("host"),
                        props.getProperty("port"), namespaces[i]);
                dataSource.setSlaveUrl(slaveUrl);
                dataSource.setDbusUser(user);
                dataSource.setDbusPwd(password);
                dataSourceMapperService.insertSelective(dataSource);
            }

            sqlSession.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        initCacheDataSource();
    }
}
