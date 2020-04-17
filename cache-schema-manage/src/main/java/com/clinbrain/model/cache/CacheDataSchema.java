package com.clinbrain.model.cache;

/**
 * @ClassName CacheDataSchema
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:14
 * @Version 1.0
 **/
public class CacheDataSchema {
    private String schemaName;

    public CacheDataSchema() {
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    @Override
    public String toString() {
        return "CacheDataSchema{" +
                "schemaName='" + schemaName + '\'' +
                '}';
    }
}
