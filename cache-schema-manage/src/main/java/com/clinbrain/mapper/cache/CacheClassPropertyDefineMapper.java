package com.clinbrain.mapper.cache;

import com.clinbrain.model.cache.CacheClassPropertyDefine;

import java.util.List;

/**
 * @ClassName CacheClassPropertyDefineMapper
 * @Description TODO
 * @Author p
 * @Date 2020/4/2 14:47
 * @Version 1.0
 **/
public interface CacheClassPropertyDefineMapper {

    /**
     * get all class property define
     * @return
     */
    List<CacheClassPropertyDefine> selectAllClassPropertyDefine();
}
