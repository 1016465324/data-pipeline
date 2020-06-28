package com.clinbrain.model;

import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JournalRecord {
    private String address;
    private String recordType;
    private String timestamp;
    private String databaseName;
    private String globalReference;
    private String globalNode;
    private String beforeStr;
    private String afterStr;

    private JSONArray beforeArray;
    private JSONArray afterArray;

    private String[] allGlobalNode;
    private String globalNameAndLength;

    public JournalRecord(ResultSet rs) {
        try {
            address = rs.getString("Address");
            recordType = rs.getString("TypeName");
            timestamp = rs.getString("TimeStamp");
            databaseName = rs.getString("DatabaseName");
            globalReference = String.format("^%s", rs.getString("GlobalReference"));
            globalNode = String.format("^%s", rs.getString("GlobalNode"));
            beforeStr = rs.getString("Before");
            afterStr = rs.getString("After");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void initInfo() {
        beforeArray = StringUtils.isEmpty(beforeStr) ? new JSONArray() : JSONArray.parseArray(beforeStr);
        afterArray = StringUtils.isEmpty(afterStr) ? new JSONArray() : JSONArray.parseArray(afterStr);

        int begin = globalReference.indexOf("(");
        int end = globalReference.indexOf(")");
        if (-1 != begin && -1 != end) {
            allGlobalNode = StringUtils.split(globalReference.substring(begin + 1, end), ",");
            globalNameAndLength = String.format("%s_%s", globalNode, allGlobalNode.length);
        }
    }

    public String getGlobalConfigValue(List<StorageSubscriptDefine> subscriptDefines) {
        StringBuilder sb = new StringBuilder(globalNode);
        sb.append("(");
        for (StorageSubscriptDefine subscriptDefine : subscriptDefines) {
            sb.append(subscriptDefine.getExpression()).append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "JournalRecord{" +
                "address='" + address + '\'' +
                ", recordType='" + recordType + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", globalReference='" + globalReference + '\'' +
                ", globalNode='" + globalNode + '\'' +
                ", beforeStr='" + beforeStr + '\'' +
                ", afterStr='" + afterStr + '\'' +
                ", beforeArray=" + beforeArray +
                ", afterArray=" + afterArray +
                '}';
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getGlobalReference() {
        return globalReference;
    }

    public void setGlobalReference(String globalReference) {
        this.globalReference = globalReference;
    }

    public String getGlobalNode() {
        return globalNode;
    }

    public void setGlobalNode(String globalNode) {
        this.globalNode = globalNode;
    }

    public JSONArray getBeforeArray() {
        return beforeArray;
    }

    public void setBeforeArray(JSONArray beforeArray) {
        this.beforeArray = beforeArray;
    }

    public JSONArray getAfterArray() {
        return afterArray;
    }

    public void setAfterArray(JSONArray afterArray) {
        this.afterArray = afterArray;
    }

    public String[] getAllGlobalNode() {
        return allGlobalNode;
    }

    public String getGlobalNameAndLength() {
        return globalNameAndLength;
    }
}
