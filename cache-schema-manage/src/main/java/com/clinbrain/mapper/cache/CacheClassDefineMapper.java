package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheClassDefine;

import java.util.List;

/**
 * @ClassName CacheClassDefineMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 10:52
 * @Version 1.0
 **/
public interface CacheClassDefineMapper {

    /**
     * get all class define from cache
     * @return
     */
    List<CacheClassDefine> selectAllClassDefine();
}
