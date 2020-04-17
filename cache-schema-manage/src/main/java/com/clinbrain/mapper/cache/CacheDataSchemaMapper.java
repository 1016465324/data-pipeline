package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheDataSchema;

import java.util.List;

/**
 * @ClassName CacheDataSchemaMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:14
 * @Version 1.0
 **/
public interface CacheDataSchemaMapper {

    /**
     * get all data schema
     * @return
     */
    List<CacheDataSchema> selectAllDataSchema();

}
