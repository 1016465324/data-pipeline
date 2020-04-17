package com.clinbrain.model.cache;

/**
 * @ClassName CacheClassStorageDefine
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:02
 * @Version 1.0
 **/
public class CacheClassStorageDefine {
    private String className;
    private String storageId;
    private String storageName;
    private String sqlRowidName;
    private String sqlChildSub;
    private String dataLocation;
    private String storageType;
    private String streamLocation;

    public CacheClassStorageDefine() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getSqlRowidName() {
        return sqlRowidName;
    }

    public void setSqlRowidName(String sqlRowidName) {
        this.sqlRowidName = sqlRowidName;
    }

    public String getSqlChildSub() {
        return sqlChildSub;
    }

    public void setSqlChildSub(String sqlChildSub) {
        this.sqlChildSub = sqlChildSub;
    }

    public String getDataLocation() {
        return dataLocation;
    }

    public void setDataLocation(String dataLocation) {
        this.dataLocation = dataLocation;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getStreamLocation() {
        return streamLocation;
    }

    public void setStreamLocation(String streamLocation) {
        this.streamLocation = streamLocation;
    }

    @Override
    public String toString() {
        return "CacheClassStorageDefine{" +
                "className='" + className + '\'' +
                ", storageId='" + storageId + '\'' +
                ", storageName='" + storageName + '\'' +
                ", sqlRowidName='" + sqlRowidName + '\'' +
                ", sqlChildSub='" + sqlChildSub + '\'' +
                ", dataLocation='" + dataLocation + '\'' +
                ", storageType='" + storageType + '\'' +
                ", streamLocation='" + streamLocation + '\'' +
                '}';
    }
}
