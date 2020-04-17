package com.clinbrain;

/**
 * @ClassName CacheSchemaManage
 * @Description TODO
 * @Author p
 * @Date 2020/3/24 16:11
 * @Version 1.0
 **/
public class CacheSchemaManage {

    public static void main(String[] args) {
        String path;
        if (1 == args.length) {
            path = args[0];
        } else {
            System.out.println("parameter error. example: CacheSchemaManage config.properties");
            return;
        }

        new SchemaInfoSync(path).schemaInfoSync();
    }

}
