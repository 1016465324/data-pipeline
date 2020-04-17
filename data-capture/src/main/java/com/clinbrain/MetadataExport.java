package com.clinbrain;

import com.intersys.cache.Dataholder;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;

import java.util.Properties;

import static com.clinbrain.DataCapture.loadProperties;
import static com.intersys.objects.Database.RET_PRIM;

/**
 * @author p
 */
public class MetadataExport {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: ");
            System.out.println("    MetadataExport config.properties");
            return;
        }

        System.out.println("start to export metadata!");
        Properties props = loadProperties(args[0]);
        String url = props.getProperty("cache_url");
        String[] cacheNamespaces = props.getProperty("cache_namespace").split(",");
        String[] usernames = props.getProperty("namespace_username").split(",");
        String[] passwords = props.getProperty("namespace_password").split(",");

        for (String cacheNamespace : cacheNamespaces) {
            String realUrl = url + cacheNamespace;
            Database database = CacheDatabase.getDatabase (realUrl, usernames[0], passwords[0]);
            System.out.println("create database connect success! url: " + realUrl);
            try {
                GetTableInfo(database, null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                database.close();
            }
        }
    }

    private static String GetTableInfo(Database cacheDatabase, String table) throws CacheException {
        System.out.println("get table info");
        Dataholder[] args = new Dataholder[2];
        args[0] = new Dataholder(table);
        args[1] = new Dataholder("12");
        Dataholder res = cacheDatabase.runClassMethod("User.TableMeta", "GetTableInfo", args, RET_PRIM);
        return res.getString();
    }
}
