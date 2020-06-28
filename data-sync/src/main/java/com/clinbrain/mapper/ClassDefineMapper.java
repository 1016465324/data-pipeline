package com.clinbrain.mapper;

import com.clinbrain.model.ClassDefine;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClassDefineMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    int insert(ClassDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    int insertSelective(ClassDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    ClassDefine selectByPrimaryKey(Integer id);

    /**
     * get class define by class name and ds id
     * @param className
     * @param dsId
     * @return
     */
    ClassDefine selectByClassNameAndDsId(@Param("className") String className, @Param("dsId") Integer dsId);

    /**
     * get all class define by ds id
     * @param dsId
     * @return
     */
    List<ClassDefine> selectAllByDsId(Integer dsId);

    /**
     * get all class define
     * @return
     */
    List<ClassDefine> selectAllClassDefine();

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(ClassDefine record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_class_define
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(ClassDefine record);
}