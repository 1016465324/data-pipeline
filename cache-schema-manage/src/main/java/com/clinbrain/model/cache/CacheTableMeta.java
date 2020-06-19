package com.clinbrain.model.cache;

/**
 * @ClassName CacheTableMeta
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:57
 * @Version 1.0
 **/
public class CacheTableMeta {
    private String tableName;
    private String columnName;
    private Integer columnId;
    private String dataType;
    private Long charLength;
    private Integer dataPrecision;
    private Integer dataScale;
    private String isPk;
    private String virtualColumn;
    private String defaultValue;
    private String nullable;
    private String comments;

    public CacheTableMeta() {
    }

    @Override
    public String toString() {
        return "CacheTableMeta{" +
                "tableName='" + tableName + '\'' +
                ", columnName='" + columnName + '\'' +
                ", columnId=" + columnId +
                ", dataType='" + dataType + '\'' +
                ", charLength=" + charLength +
                ", dataPrecision=" + dataPrecision +
                ", dataScale=" + dataScale +
                ", isPk='" + isPk + '\'' +
                ", virtualColumn='" + virtualColumn + '\'' +
                ", defaultValue='" + defaultValue + '\'' +
                ", nullable='" + nullable + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Integer getColumnId() {
        return columnId;
    }

    public void setColumnId(Integer columnId) {
        this.columnId = columnId;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Long getCharLength() {
        return charLength;
    }

    public void setCharLength(Long charLength) {
        this.charLength = charLength;
    }

    public Integer getDataPrecision() {
        return dataPrecision;
    }

    public void setDataPrecision(Integer dataPrecision) {
        this.dataPrecision = dataPrecision;
    }

    public Integer getDataScale() {
        return dataScale;
    }

    public void setDataScale(Integer dataScale) {
        this.dataScale = dataScale;
    }

    public String getIsPk() {
        return isPk;
    }

    public void setIsPk(String isPk) {
        this.isPk = isPk;
    }

    public String getVirtualColumn() {
        return virtualColumn;
    }

    public void setVirtualColumn(String virtualColumn) {
        this.virtualColumn = virtualColumn;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getNullable() {
        return nullable;
    }

    public void setNullable(String nullable) {
        this.nullable = nullable;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
