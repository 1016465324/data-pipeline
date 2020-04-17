package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheStorageRowidSubscript;

import java.util.List;

/**
 * @ClassName CacheStorageRowidSubscriptMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:39
 * @Version 1.0
 **/
public interface CacheStorageRowidSubscriptMapper {

    /**
     * get all storage rowid subscript
     * @return
     */
    List<CacheStorageRowidSubscript> selectAllStorageRowidSubscript();
}
