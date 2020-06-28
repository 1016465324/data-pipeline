package com.clinbrain.mapper;

import com.clinbrain.model.StorageSubscriptDefine;

import java.util.List;

public interface StorageSubscriptDefineMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    int insert(StorageSubscriptDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    int insertSelective(StorageSubscriptDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    StorageSubscriptDefine selectByPrimaryKey(Integer id);

    /**
     * get all storage subscript define
     * @return
     */
    List<StorageSubscriptDefine> selectAllStorageSubscriptDefine();

    /**
     * get all storage subscript define by ds id
     * @param dsId
     * @return
     */
    List<StorageSubscriptDefine> selectAllByDsId(Integer dsId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(StorageSubscriptDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_storage_subscript_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(StorageSubscriptDefine record);
}