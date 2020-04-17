package com.clinbrain.database;

import java.io.Serializable;
import java.util.List;

public class TableMeta implements Serializable {
    public static class ColumnMeta implements Serializable {
        public String name;
        public String dataType;
        public String comment;
        public String dataLength;

        public ColumnMeta(String name, String dataType, String comment) {
            this.name = name;
            this.dataType = dataType;
            this.comment = comment;
        }

        public ColumnMeta(String name, String dataType, String comment, String dataLength) {
            this(name, dataType, comment);
            this.dataLength = dataLength;
        }

        public ColumnMeta() {

        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getDataLength() {
            return dataLength;
        }

        public void setDataLength(String dataLength) {
            this.dataLength = dataLength;
        }

        @Override
        public String toString() {
            return "ColumnMeta{" +
                    "name='" + name + '\'' +
                    ", dataType='" + dataType + '\'' +
                    ", comment='" + comment + '\'' +
                    ", dataLength='" + dataLength + '\'' +
                    '}';
        }
    }

    private String database;
    private String tableName;
    private List<ColumnMeta> allColumns;
    private List<ColumnMeta> pkColumns;
    private List<ColumnMeta> generatedColumns;

    private TableMeta() {
    }

    private TableMeta(String database, String tableName,
                     List<ColumnMeta> allColumns,
                     List<ColumnMeta> pkColumns,
                     List<ColumnMeta> generatedColumns) {
        this.database = database;
        this.tableName = tableName;
        this.allColumns = allColumns;
        this.pkColumns = pkColumns;
        this.generatedColumns = generatedColumns;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String database;
        private String tableName;
        private List<ColumnMeta> allColumns;
        private List<ColumnMeta> primaryKeyColumns;
        private List<ColumnMeta> generatedColumns;

        private Builder() {

        }

        public Builder setDatabase(String database) {
            this.database = database;
            return this;
        }

        public Builder setTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder setAllColumns(List<ColumnMeta> allColumns) {
            this.allColumns = allColumns;
            return this;
        }

        public Builder setPrimaryKeyColumns(List<ColumnMeta> primaryKeyColumns) {
            this.primaryKeyColumns = primaryKeyColumns;
            return this;
        }

        public Builder setGeneratedColumns(List<ColumnMeta> generatedColumns) {
            this.generatedColumns = generatedColumns;
            return this;
        }

        public TableMeta get() {
            return new TableMeta(database, tableName, allColumns, primaryKeyColumns, generatedColumns);
        }
    }

    @Override
    public String toString() {
        return "TableMeta{" +
                "database='" + database + '\'' +
                ", tableName='" + tableName + '\'' +
                ", allColumns=" + allColumns +
                ", pkColumns=" + pkColumns +
                ", generatedColumns=" + generatedColumns +
                '}';
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<ColumnMeta> getAllColumns() {
        return allColumns;
    }

    public void setAllColumns(List<ColumnMeta> allColumns) {
        this.allColumns = allColumns;
    }

    public List<ColumnMeta> getPkColumns() {
        return pkColumns;
    }

    public void setPkColumns(List<ColumnMeta> pkColumns) {
        this.pkColumns = pkColumns;
    }

    public List<ColumnMeta> getGeneratedColumns() {
        return generatedColumns;
    }

    public void setGeneratedColumns(List<ColumnMeta> generatedColumns) {
        this.generatedColumns = generatedColumns;
    }
}
