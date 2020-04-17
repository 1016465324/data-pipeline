package com.clinbrain.mapper;

import com.clinbrain.model.DataSource;

import java.util.List;

public interface DataSourceMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    int insert(DataSource record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    int insertSelective(DataSource record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    DataSource selectByPrimaryKey(Integer id);

    /**
     * get data source by instance name and db type
     * @param instanceName
     * @return
     */
    DataSource selectByInstanceAndDbType(String instanceName);

    /**
     * get all cache data source
     * @return
     */
    List<DataSource> selectAllCacheDataSource();

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    int updateByPrimaryKeySelective(DataSource record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_dbus_datasource
     *
     * @mbg.generated
     */
    int updateByPrimaryKey(DataSource record);
}