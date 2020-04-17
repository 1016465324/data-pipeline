package com.clinbrain.model.cache;

/**
 * @ClassName CacheClassDefine
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 14:06
 * @Version 1.0
 **/
public class CacheClassDefine {
    private String className;
    private String classType;
    private String clientDataType;
    private String classSuper;
    private Boolean sqlRowidPrivate;
    private String compileNamespace;
    private String runtimeType;
    private String timeChanged;
    private String timeCreated;

    public CacheClassDefine() {

    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getClientDataType() {
        return clientDataType;
    }

    public void setClientDataType(String clientDataType) {
        this.clientDataType = clientDataType;
    }

    public String getClassSuper() {
        return classSuper;
    }

    public void setClassSuper(String classSuper) {
        this.classSuper = classSuper;
    }

    public Boolean getSqlRowidPrivate() {
        return sqlRowidPrivate;
    }

    public void setSqlRowidPrivate(Boolean sqlRowidPrivate) {
        this.sqlRowidPrivate = sqlRowidPrivate;
    }

    public String getCompileNamespace() {
        return compileNamespace;
    }

    public void setCompileNamespace(String compileNamespace) {
        this.compileNamespace = compileNamespace;
    }

    public String getRuntimeType() {
        return runtimeType;
    }

    public void setRuntimeType(String runtimeType) {
        this.runtimeType = runtimeType;
    }

    public String getTimeChanged() {
        return timeChanged;
    }

    public void setTimeChanged(String timeChanged) {
        this.timeChanged = timeChanged;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Override
    public String toString() {
        return "CacheClassDefine{" +
                "className='" + className + '\'' +
                ", classType='" + classType + '\'' +
                ", clientDataType='" + clientDataType + '\'' +
                ", classSuper='" + classSuper + '\'' +
                ", sqlRowidPrivate='" + sqlRowidPrivate + '\'' +
                ", compileNamespace='" + compileNamespace + '\'' +
                ", runtimeType='" + runtimeType + '\'' +
                ", timeChanged='" + timeChanged + '\'' +
                ", timeCreated='" + timeCreated + '\'' +
                '}';
    }
}
