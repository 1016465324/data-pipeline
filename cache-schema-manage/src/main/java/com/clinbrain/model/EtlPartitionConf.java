package com.clinbrain.model;

import java.util.Date;

public class EtlPartitionConf {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.id
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.hospital_no
     *
     * @mbg.generated
     */
    private String hospitalNo;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.his_db_name
     *
     * @mbg.generated
     */
    private String hisDbName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.his_tb_name
     *
     * @mbg.generated
     */
    private String hisTbName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.his_tb_partition_column_name
     *
     * @mbg.generated
     */
    private String hisTbPartitionColumnName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.partition_type
     *
     * @mbg.generated
     */
    private String partitionType;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.ref_cdr_tb_name
     *
     * @mbg.generated
     */
    private String refCdrTbName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.ref_cdr_column_name
     *
     * @mbg.generated
     */
    private String refCdrColumnName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.date_format
     *
     * @mbg.generated
     */
    private String dateFormat;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.created_at
     *
     * @mbg.generated
     */
    private Date createdAt;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.updated_at
     *
     * @mbg.generated
     */
    private Date updatedAt;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column etl_histable_partitions_configuration.hiveSourceDbName
     *
     * @mbg.generated
     */
    private String hivesourcedbname;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.id
     *
     * @return the value of etl_histable_partitions_configuration.id
     *
     * @mbg.generated
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.id
     *
     * @param id the value for etl_histable_partitions_configuration.id
     *
     * @mbg.generated
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.hospital_no
     *
     * @return the value of etl_histable_partitions_configuration.hospital_no
     *
     * @mbg.generated
     */
    public String getHospitalNo() {
        return hospitalNo;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.hospital_no
     *
     * @param hospitalNo the value for etl_histable_partitions_configuration.hospital_no
     *
     * @mbg.generated
     */
    public void setHospitalNo(String hospitalNo) {
        this.hospitalNo = hospitalNo == null ? null : hospitalNo.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.his_db_name
     *
     * @return the value of etl_histable_partitions_configuration.his_db_name
     *
     * @mbg.generated
     */
    public String getHisDbName() {
        return hisDbName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.his_db_name
     *
     * @param hisDbName the value for etl_histable_partitions_configuration.his_db_name
     *
     * @mbg.generated
     */
    public void setHisDbName(String hisDbName) {
        this.hisDbName = hisDbName == null ? null : hisDbName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.his_tb_name
     *
     * @return the value of etl_histable_partitions_configuration.his_tb_name
     *
     * @mbg.generated
     */
    public String getHisTbName() {
        return hisTbName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.his_tb_name
     *
     * @param hisTbName the value for etl_histable_partitions_configuration.his_tb_name
     *
     * @mbg.generated
     */
    public void setHisTbName(String hisTbName) {
        this.hisTbName = hisTbName == null ? null : hisTbName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.his_tb_partition_column_name
     *
     * @return the value of etl_histable_partitions_configuration.his_tb_partition_column_name
     *
     * @mbg.generated
     */
    public String getHisTbPartitionColumnName() {
        return hisTbPartitionColumnName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.his_tb_partition_column_name
     *
     * @param hisTbPartitionColumnName the value for etl_histable_partitions_configuration.his_tb_partition_column_name
     *
     * @mbg.generated
     */
    public void setHisTbPartitionColumnName(String hisTbPartitionColumnName) {
        this.hisTbPartitionColumnName = hisTbPartitionColumnName == null ? null : hisTbPartitionColumnName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.partition_type
     *
     * @return the value of etl_histable_partitions_configuration.partition_type
     *
     * @mbg.generated
     */
    public String getPartitionType() {
        return partitionType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.partition_type
     *
     * @param partitionType the value for etl_histable_partitions_configuration.partition_type
     *
     * @mbg.generated
     */
    public void setPartitionType(String partitionType) {
        this.partitionType = partitionType == null ? null : partitionType.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.ref_cdr_tb_name
     *
     * @return the value of etl_histable_partitions_configuration.ref_cdr_tb_name
     *
     * @mbg.generated
     */
    public String getRefCdrTbName() {
        return refCdrTbName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.ref_cdr_tb_name
     *
     * @param refCdrTbName the value for etl_histable_partitions_configuration.ref_cdr_tb_name
     *
     * @mbg.generated
     */
    public void setRefCdrTbName(String refCdrTbName) {
        this.refCdrTbName = refCdrTbName == null ? null : refCdrTbName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.ref_cdr_column_name
     *
     * @return the value of etl_histable_partitions_configuration.ref_cdr_column_name
     *
     * @mbg.generated
     */
    public String getRefCdrColumnName() {
        return refCdrColumnName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.ref_cdr_column_name
     *
     * @param refCdrColumnName the value for etl_histable_partitions_configuration.ref_cdr_column_name
     *
     * @mbg.generated
     */
    public void setRefCdrColumnName(String refCdrColumnName) {
        this.refCdrColumnName = refCdrColumnName == null ? null : refCdrColumnName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.date_format
     *
     * @return the value of etl_histable_partitions_configuration.date_format
     *
     * @mbg.generated
     */
    public String getDateFormat() {
        return dateFormat;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.date_format
     *
     * @param dateFormat the value for etl_histable_partitions_configuration.date_format
     *
     * @mbg.generated
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat == null ? null : dateFormat.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.created_at
     *
     * @return the value of etl_histable_partitions_configuration.created_at
     *
     * @mbg.generated
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.created_at
     *
     * @param createdAt the value for etl_histable_partitions_configuration.created_at
     *
     * @mbg.generated
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.updated_at
     *
     * @return the value of etl_histable_partitions_configuration.updated_at
     *
     * @mbg.generated
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.updated_at
     *
     * @param updatedAt the value for etl_histable_partitions_configuration.updated_at
     *
     * @mbg.generated
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column etl_histable_partitions_configuration.hiveSourceDbName
     *
     * @return the value of etl_histable_partitions_configuration.hiveSourceDbName
     *
     * @mbg.generated
     */
    public String getHivesourcedbname() {
        return hivesourcedbname;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column etl_histable_partitions_configuration.hiveSourceDbName
     *
     * @param hivesourcedbname the value for etl_histable_partitions_configuration.hiveSourceDbName
     *
     * @mbg.generated
     */
    public void setHivesourcedbname(String hivesourcedbname) {
        this.hivesourcedbname = hivesourcedbname == null ? null : hivesourcedbname.trim();
    }
}