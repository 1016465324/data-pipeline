package com.clinbrain.source.cache;

import java.io.Serializable;
import java.sql.ResultSet;

public class CDCGlobalLog implements Serializable {
    private String ID;
    private String DBName;
    private String GlobalName;
    private String RecordType;
    private String Val;
    private String TS;

    public CDCGlobalLog() {

    }

    public CDCGlobalLog(ResultSet rs) {

    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getDBName() {
        return DBName;
    }

    public void setDBName(String DBName) {
        this.DBName = DBName;
    }

    public String getGlobalName() {
        return GlobalName;
    }

    public void setGlobalName(String globalName) {
        GlobalName = globalName;
    }

    public String getRecordType() {
        return RecordType;
    }

    public void setRecordType(String recordType) {
        RecordType = recordType;
    }

    public String getVal() {
        return Val;
    }

    public void setVal(String val) {
        Val = val;
    }

    public String getTS() {
        return TS;
    }

    public void setTS(String TS) {
        this.TS = TS;
    }
}
