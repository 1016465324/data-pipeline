package com.wheel.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @ClassName UtilHelper
 * @Description TODO
 * @Author p
 * @Date 2020/3/25 15:12
 * @Version 1.0
 **/
public class UtilHelper {

    public static Properties loadProperties(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new RuntimeException("配置文件路径为空.");
        }
        
        Properties props = new Properties();
        InputStream inputStream = null;
        if (StringUtils.containsNone(path, File.separator)) {
            inputStream = UtilHelper.class.getClassLoader().getResourceAsStream(path);
        } else {
            try {
                inputStream = new FileInputStream(new File(path));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            props.load(inputStream);
            IOUtils.closeQuietly(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return props;
    }
    
    public static int wellDistributedHash(int hashCode, int hashRange) {
        int hash = hashCode ^ (hashCode >>> 16);
        return hash & (hashRange - 1);
    }
}
