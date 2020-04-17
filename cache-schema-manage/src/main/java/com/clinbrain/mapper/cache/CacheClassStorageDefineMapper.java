package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheClassStorageDefine;

import java.util.List;

/**
 * @ClassName CacheClassStorageDefineMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 15:04
 * @Version 1.0
 **/
public interface CacheClassStorageDefineMapper {

    /**
     * get all class storage define
     * @return
     */
    List<CacheClassStorageDefine> selectAllClassStorageDefine();

}
