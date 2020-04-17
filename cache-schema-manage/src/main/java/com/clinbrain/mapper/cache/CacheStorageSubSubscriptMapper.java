package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheStorageSubSubscript;

import java.util.List;

/**
 * @ClassName CacheStorageSubSubscriptMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:29
 * @Version 1.0
 **/
public interface CacheStorageSubSubscriptMapper {

    /**
     * get all storage sub subscript
     * @return
     */
    List<CacheStorageSubSubscript> selectAllStorageSubSubscript();

}
