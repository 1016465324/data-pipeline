package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.clinbrain.database.CachedbClient;
import com.clinbrain.database.CachedbMetadata;
import com.clinbrain.database.TableMeta;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static com.clinbrain.DataCapture.loadProperties;

/**
 * @author p
 */
public class CachedbMetadataExport {
    public static String META_SEPARATOR = "(!)";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: ");
            System.out.println("    CachedbMetadataExport config.properties");
            return;
        }

        System.out.println("start to export Cachedb metadata!");
        Properties props = loadProperties(args[0]);
        String url = props.getProperty("cache_url");
        String[] cacheNamespaces = props.getProperty("cache_namespace").split(",");
        String[] usernames = props.getProperty("namespace_username").split(",");
        String[] passwords = props.getProperty("namespace_password").split(",");
        for (String cacheNamespace : cacheNamespaces) {
            String realUrl = url + cacheNamespace;
            CachedbClient client = new CachedbClient(realUrl, usernames[0], passwords[0]);

            System.out.println("create database connect success! url: " + realUrl);

            CachedbMetadata cacheDBMetadata = new CachedbMetadata();
            cacheDBMetadata.init(client);

            BufferedOutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream("./" + cacheNamespace + "_table.txt", false));
                for (Map.Entry<String, TableMeta> entry : cacheDBMetadata.getAllDBTableMeta().entrySet()) {
                    output.write(entry.getKey().getBytes());
                    output.write(META_SEPARATOR.getBytes());
                    output.write(JSON.toJSONString(entry.getValue(), SerializerFeature.WriteMapNullValue).getBytes());
                    output.write("\n".getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                client.close();
            }
        }



    }
}
