package com.clinbrain.global;

import java.io.Serializable;

public class FieldInfo implements Serializable {
    private String fieldName;
    private Integer index;
    private String comment;
    private String type;

    public FieldInfo() {

    }

    public FieldInfo(String fieldName, Integer index, String comment, String type) {
        this.fieldName = fieldName.toLowerCase();
        this.index = index;
        this.comment = comment;
        this.type = type;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
