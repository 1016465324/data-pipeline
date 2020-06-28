package com.clinbrain.mapper;

import com.clinbrain.model.DataSchema;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DataSchemaMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    int insert(DataSchema record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    int insertSelective(DataSchema record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    DataSchema selectByPrimaryKey(Integer id);

    /**
     * get all data table
     * @return
     */
    List<DataSchema> selectAllDataSchema();

    /**
     * get data schema by schema name and data source id
     * @param schemaName
     * @param dsId
     * @return
     */
    DataSchema selectBySchemaNameAndDsId(@Param("schemaName") String schemaName, @Param("dsId") Integer dsId);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DataSchema record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_data_schema
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DataSchema record);
}