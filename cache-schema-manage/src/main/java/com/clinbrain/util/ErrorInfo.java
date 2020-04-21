package com.clinbrain.util;

import org.apache.commons.lang3.StringUtils;

public enum ErrorInfo {
    PARSE_JSON_ERROR("JSON解析失败"),
    GLOBMANAGER_NOTFOUND("加载globmanager配置类失败");

    private String desc;

    ErrorInfo() {}

    ErrorInfo(String desc) {
        this.desc = desc;
    }

    public static String getErrDesc(ErrorInfo key){
        for (ErrorInfo info : ErrorInfo.values()) {
            String name = info.name();
            if(StringUtils.equals(key.name(), name)){
                return info.desc;
            }
        }
        return "";
    }

}
