package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheTableMeta;

import java.util.List;

/**
 * @ClassName CacheTableMetaMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:57
 * @Version 1.0
 **/
public interface CacheTableMetaMapper {

    /**
     * get all table meta by schema name
     * @param schemaName
     * @return
     */
    List<CacheTableMeta> selectAllTableMeta(String schemaName);

}
