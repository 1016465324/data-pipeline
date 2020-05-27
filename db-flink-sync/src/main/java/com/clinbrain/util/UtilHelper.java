package com.clinbrain.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @ClassName UtilHelper
 * @Description TODO
 * @Author p
 * @Date 2020/5/21 17:33
 * @Version 1.0
 **/
public class UtilHelper {

    public static Properties loadProperties(String path) {
        Properties props = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = FileUtils.openInputStream(new File(path));
            props.load(inputStream);
        } catch (IOException e) {
            IOUtils.closeQuietly(inputStream);
            throw new RuntimeException(e);
        }

        IOUtils.closeQuietly(inputStream);
        return props;
    }



}
