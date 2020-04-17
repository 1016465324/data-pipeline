package com.clinbrain.model.cache;

/**
 * @ClassName CacheDataTable
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:20
 * @Version 1.0
 **/
public class CacheDataTable {
    private String schemaName;
    private String tableName;
    private String className;
    private String description;

    public CacheDataTable() {
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "CacheDataTable{" +
                "schemaName='" + schemaName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", className='" + className + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
