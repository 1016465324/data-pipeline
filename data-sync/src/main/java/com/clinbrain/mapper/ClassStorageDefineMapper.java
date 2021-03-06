package com.clinbrain.mapper;

import com.clinbrain.model.ClassStorageDefine;

import java.util.List;

public interface ClassStorageDefineMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    int insert(ClassStorageDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    int insertSelective(ClassStorageDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    ClassStorageDefine selectByPrimaryKey(Integer id);

    /**
     * get all class storage define
     * @return
     */
    List<ClassStorageDefine> selectAllClassStorageDefine();

    /**
     * get all class storage define by ds id
     * @param dsId
     * @return
     */
    List<ClassStorageDefine> selectAllByDsId(Integer dsId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(ClassStorageDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_storage_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(ClassStorageDefine record);
}