package com.clinbrain.model.cache;

/**
 * @ClassName CacheStorageSubSubscript
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:29
 * @Version 1.0
 **/
public class CacheStorageSubSubscript {
    private String className;
    private String storageName;
    private String accessType;
    private String delimiter;
    private String expression;
    private String name;

    public CacheStorageSubSubscript() {
    }

    @Override
    public String toString() {
        return "CacheStorageSubSubscript{" +
                "className='" + className + '\'' +
                ", storageName='" + storageName + '\'' +
                ", accessType='" + accessType + '\'' +
                ", delimiter='" + delimiter + '\'' +
                ", expression='" + expression + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
