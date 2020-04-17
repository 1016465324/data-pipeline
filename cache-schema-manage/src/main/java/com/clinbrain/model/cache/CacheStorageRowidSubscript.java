package com.clinbrain.model.cache;

/**
 * @ClassName CacheStorageRowidSubscript
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:39
 * @Version 1.0
 **/
public class CacheStorageRowidSubscript {
    private String className;
    private String parent;
    private String rowidName;
    private String expression;
    private String name;

    public CacheStorageRowidSubscript() {
    }

    @Override
    public String toString() {
        return "CacheStorageRowidSubscript{" +
                "className='" + className + '\'' +
                ", parent='" + parent + '\'' +
                ", rowidName='" + rowidName + '\'' +
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

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getRowidName() {
        return rowidName;
    }

    public void setRowidName(String rowidName) {
        this.rowidName = rowidName;
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
