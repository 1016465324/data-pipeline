package com.clinbrain.util;

import com.google.common.base.Strings;

import java.io.*;
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
        Properties props = new Properties();
        InputStream inputStream = null;
        if (Strings.isNullOrEmpty(path)) {
            inputStream = UtilHelper.class.getClassLoader().getResourceAsStream("cache.properties");
        } else {
            try {
                inputStream = new FileInputStream(new File(path));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return props;
    }

    public static String getMySqlDriver(boolean isMySQL8) {
        return isMySQL8 ? "com.mysql.cj.jdbc.Driver" : "com.mysql.jdbc.Driver";
    }
}
