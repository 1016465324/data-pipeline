package com.clinbrain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName CacheConstant
 * @Description TODO
 * @Author p
 * @Date 2020/3/27 15:54
 * @Version 1.0
 **/
public class CacheConstant {
    private final static String CACHE_SUPPORTED_BASIC_DATATYPE_STR = "%Library.CacheString|%Library.String|%Library.Char|%Library.Text|%Library.CacheLiteral|%Library.GUID|Library.Date|%Library.DateTime|%Library.TimeStamp|%Library.Time|%Library.UTC|%Library.StringTimeStamp|%Library.Integer|%Library.SmallInt|%Library.TinyInt|%Library.BigInt|%Library.Double|%Library.Float|%Library.Numeric|%Library.Decimal%Library.Boolean";
    public final static Set<String> CACHE_SUPPORTED_BASIC_DATATYPE = new HashSet<>(Arrays.asList(CACHE_SUPPORTED_BASIC_DATATYPE_STR.split("\\|")));
    private final static String CACHE_LIST_DATATYPE_STR = "%Collection.ListOfObj|%Collection.ListOfObjCN|%Collection.ListOfStream";
    public final static Set<String> CACHE_LIST_DATATYPE = new HashSet<>(Arrays.asList(CACHE_LIST_DATATYPE_STR.split("\\|")));
}
