package com.clinbrain.global;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GlobalInfo implements Serializable {
    private String globalName;
    private Integer length;
    private List<Integer> allConstantIndex;
    private List<Integer> allKeyIndex;
    private List<String> allConstant;
    private String[] globalFields;
    private List<FieldInfo> allFieldInfo;
    private TableInfo tableInfo;

    public GlobalInfo(String globalStr) {
        this(globalStr, true);
    }

    public GlobalInfo(String globalStr, boolean isConfig) {
        allConstantIndex = new ArrayList<>();
        allKeyIndex = new ArrayList<>();
        allConstant = new ArrayList<>();
        allFieldInfo = new ArrayList<>();

        if (isConfig) {
            int index = globalStr.indexOf("(");
            if (-1 == index) {
                globalName = globalStr;
                length = 1;
                globalFields = new String[]{"primarykey"};
                allKeyIndex.add(0);
            } else {
                globalName = globalStr.substring(0, index);
                globalFields = globalStr.substring(index + 1, globalStr.lastIndexOf(")")).split(",");
                length = globalFields.length;
                for (int i = 0; i < globalFields.length; i++) {
                    int beginPos = globalFields[i].indexOf("{");
                    if (-1 == beginPos) {
                        allConstantIndex.add(i);
                        allConstant.add(globalFields[i]);
                    } else {
                        int endPos = globalFields[i].indexOf("}");
                        globalFields[i] = globalFields[i].substring(beginPos + 1, endPos);
                        allKeyIndex.add(i);
                    }
                }
            }
        } else {
            int index = globalStr.indexOf("(");
            if (-1 == index) {
                globalName = globalStr;
                globalFields = null;
                length = 0;
            } else {
                globalName = globalStr.substring(0, index);
                globalFields = globalStr.substring(index + 1, globalStr.lastIndexOf(")")).split(",");
                length = globalFields.length;
            }
        }
    }

    @Override
    public int hashCode() {
        return globalName.hashCode() + length.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GlobalInfo) {
            GlobalInfo other = (GlobalInfo) obj;
            if (globalName.equals(other.globalName)) {
                if (length.equals(other.length)) {
                    if (allConstantIndex.equals(other.allConstantIndex)) {
                        return allConstant.equals(other.allConstant);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    public String buildGlobal(GlobalInfo other) {
        StringBuilder sb = new StringBuilder();
        sb.append(globalName);
        sb.append("(");
        for (int i = 0; i < globalFields.length; i++) {
            if (allConstantIndex.contains(i)) {
                sb.append(globalFields[i]);
            } else {
                sb.append(other.globalFields[i]);
            }

            sb.append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        return sb.toString();
    }

    public String getGlobalName() {
        return globalName;
    }

    public void setGlobalName(String globalName) {
        this.globalName = globalName;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public List<Integer> getAllConstantIndex() {
        return allConstantIndex;
    }

    public void setAllConstantIndex(List<Integer> allConstantIndex) {
        this.allConstantIndex = allConstantIndex;
    }

    public List<Integer> getAllKeyIndex() {
        return allKeyIndex;
    }

    public void setAllKeyIndex(List<Integer> allKeyIndex) {
        this.allKeyIndex = allKeyIndex;
    }

    public List<String> getAllConstant() {
        return allConstant;
    }

    public void setAllConstant(List<String> allConstant) {
        this.allConstant = allConstant;
    }

    public String[] getGlobalFields() {
        return globalFields;
    }

    public void setGlobalFields(String[] globalFields) {
        this.globalFields = globalFields;
    }

    public List<FieldInfo> getAllFieldInfo() {
        return allFieldInfo;
    }

    public void setAllFieldInfo(List<FieldInfo> allFieldInfo) {
        this.allFieldInfo = allFieldInfo;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public void setTableInfo(TableInfo tableInfo) {
        this.tableInfo = tableInfo;
    }
}
