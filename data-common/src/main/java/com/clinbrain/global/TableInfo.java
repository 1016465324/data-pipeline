package com.clinbrain.global;

import java.io.Serializable;
import java.util.Set;

public class TableInfo implements Serializable {
    private String tableName;
    private String namespace;
    private String entityName;

    public TableInfo(String namespace, String entityName, String tableName) {
        this.tableName = tableName.toLowerCase();
        this.namespace = namespace.toLowerCase();
        this.entityName = entityName;
    }

    @Override
    public int hashCode() {
        return tableName.hashCode() + namespace.hashCode() + entityName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableInfo) {
            TableInfo other = (TableInfo) obj;

            return tableName.equals(other.tableName) &&
                    namespace.equals(other.namespace) &&
                    entityName.equals(other.entityName);
        }

        return false;
    }


    public String getSQLTableName(Set<String> sqluserTables) {
        //如果配置中的SQL表名中有.则默认它与jdbc导出schema.tableName是相同的
        if (tableName.contains(".")) {
            return tableName;
        }

        String tmpEntityName = entityName.toLowerCase();
        //因为部分表通过jdbc导出schema.tableName是sqluser的，然后配置表中为User，所以维护那些为sqluser的，直接查询
        if (sqluserTables.contains(tmpEntityName)) {
            return "sqluser." + tableName;
        }

        //都不是以上两种情况，则按正常逻辑处理，实体类最后个点保留，之前的点替换为下划线
        String[] fields = tmpEntityName.split("\\.");
        if (2 == fields.length) {
            return fields[0] + "." + tableName;
        } else if (fields.length > 2) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (; i < fields.length - 2; i++) {
                sb.append(fields[i]);
                sb.append("_");
            }
            sb.append(fields[i++]);
            sb.append(".");
            sb.append(fields[i]);

            return sb.toString();
        } else {
            return null;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}
