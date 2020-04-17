package com.clinbrain.util;

import com.google.common.base.Strings;

import java.io.*;
import java.util.Properties;

public class PopLoadUtils {

    private PopLoadUtils(){ }

    public static PopLoadUtils PopLoadUtilsFactory(){
        return new PopLoadUtils();
    }

    public static Properties loadProperties(String path) {
        Properties props = new Properties();
        InputStream inputStream = null;
        if (Strings.isNullOrEmpty(path)) {
            inputStream = UtilHelper.class.getClassLoader().getResourceAsStream("application.properties");
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
}
