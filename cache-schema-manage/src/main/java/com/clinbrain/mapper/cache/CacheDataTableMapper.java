package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheDataTable;

import java.util.List;

/**
 * @ClassName CacheDataTableMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:20
 * @Version 1.0
 **/
public interface CacheDataTableMapper {

    /**
     * get all data table by schema name
     * @param schemaName
     * @return
     */
    List<CacheDataTable> selectAllDataTable(String schemaName);
}
