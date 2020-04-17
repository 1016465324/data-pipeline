package com.clinbrain.model.cache;

/**
 * @ClassName CacheClassPropertyDefine
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 14:37
 * @Version 1.0
 **/
public class CacheClassPropertyDefine {
    private String className;
    private String propertyName;
    private String propertyCollection;
    private String propertyAliases;
    private Boolean propertyCalculated;
    private String propertyCardinality;
    private String runtimeType;
    private String sqlFieldName;
    private String sqlListType;
    private String sqlListDelimiter;
    private Boolean storable;
    private String storageName;
    private String storageDelimiter;
    private String storageSubscript;
    private String storagePiece;

    public CacheClassPropertyDefine() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyCollection() {
        return propertyCollection;
    }

    public void setPropertyCollection(String propertyCollection) {
        this.propertyCollection = propertyCollection;
    }

    public String getPropertyAliases() {
        return propertyAliases;
    }

    public void setPropertyAliases(String propertyAliases) {
        this.propertyAliases = propertyAliases;
    }

    public Boolean getPropertyCalculated() {
        return propertyCalculated;
    }

    public void setPropertyCalculated(Boolean propertyCalculated) {
        this.propertyCalculated = propertyCalculated;
    }

    public String getPropertyCardinality() {
        return propertyCardinality;
    }

    public void setPropertyCardinality(String propertyCardinality) {
        this.propertyCardinality = propertyCardinality;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }

    public String getSqlFieldName() {
        return sqlFieldName;
    }

    public void setSqlFieldName(String sqlFieldName) {
        this.sqlFieldName = sqlFieldName;
    }

    public String getSqlListType() {
        return sqlListType;
    }

    public void setSqlListType(String sqlListType) {
        this.sqlListType = sqlListType;
    }

    public String getSqlListDelimiter() {
        return sqlListDelimiter;
    }

    public void setSqlListDelimiter(String sqlListDelimiter) {
        this.sqlListDelimiter = sqlListDelimiter;
    }

    public Boolean getStorable() {
        return storable;
    }

    public void setStorable(Boolean storable) {
        this.storable = storable;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getStorageDelimiter() {
        return storageDelimiter;
    }

    public void setStorageDelimiter(String storageDelimiter) {
        this.storageDelimiter = storageDelimiter;
    }

    public String getStorageSubscript() {
        return storageSubscript;
    }

    public void setStorageSubscript(String storageSubscript) {
        this.storageSubscript = storageSubscript;
    }

    public String getStoragePiece() {
        return storagePiece;
    }

    public void setStoragePiece(String storagePiece) {
        this.storagePiece = storagePiece;
    }

    @Override
    public String toString() {
        return "CacheClassPropertyDefine{" +
                "className='" + className + '\'' +
                ", propertyName='" + propertyName + '\'' +
                ", propertyCollection='" + propertyCollection + '\'' +
                ", propertyAliases='" + propertyAliases + '\'' +
                ", propertyCalculated=" + propertyCalculated +
                ", propertyCardinality='" + propertyCardinality + '\'' +
                ", runtimeType='" + runtimeType + '\'' +
                ", sqlFieldName='" + sqlFieldName + '\'' +
                ", sqlListType='" + sqlListType + '\'' +
                ", sqlListDelimiter='" + sqlListDelimiter + '\'' +
                ", storable=" + storable +
                ", storageName='" + storageName + '\'' +
                ", storageDelimiter='" + storageDelimiter + '\'' +
                ", storageSubscript='" + storageSubscript + '\'' +
                ", storagePiece='" + storagePiece + '\'' +
                '}';
    }
}
