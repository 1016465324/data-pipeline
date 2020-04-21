package com.clinbrain.model;

import java.util.Date;

public class ParseLogs {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.id
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.offset_namespace
     *
     * @mbg.generated
     */
    private String offsetNamespace;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.identification
     *
     * @mbg.generated
     */
    private Integer identification;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.create_time
     *
     * @mbg.generated
     */
    private Date createTime;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.log_id
     *
     * @mbg.generated
     */
    private String logId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column restore_data_logs.offset_index
     *
     * @mbg.generated
     */
    private String offsetIndex;

    public ParseLogs() {
    }

    public ParseLogs(String offsetNamespace, String offsetIndex, Integer identification) {
        this.offsetNamespace = offsetNamespace;
        this.offsetIndex = offsetIndex;
        this.identification = identification;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.id
     *
     * @return the value of restore_data_logs.id
     *
     * @mbg.generated
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.id
     *
     * @param id the value for restore_data_logs.id
     *
     * @mbg.generated
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.offset_namespace
     *
     * @return the value of restore_data_logs.offset_namespace
     *
     * @mbg.generated
     */
    public String getOffsetNamespace() {
        return offsetNamespace;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.offset_namespace
     *
     * @param offsetNamespace the value for restore_data_logs.offset_namespace
     *
     * @mbg.generated
     */
    public void setOffsetNamespace(String offsetNamespace) {
        this.offsetNamespace = offsetNamespace == null ? null : offsetNamespace.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.identification
     *
     * @return the value of restore_data_logs.identification
     *
     * @mbg.generated
     */
    public Integer getIdentification() {
        return identification;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.identification
     *
     * @param identification the value for restore_data_logs.identification
     *
     * @mbg.generated
     */
    public void setIdentification(Integer identification) {
        this.identification = identification;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.create_time
     *
     * @return the value of restore_data_logs.create_time
     *
     * @mbg.generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.create_time
     *
     * @param createTime the value for restore_data_logs.create_time
     *
     * @mbg.generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.log_id
     *
     * @return the value of restore_data_logs.log_id
     *
     * @mbg.generated
     */
    public String getLogId() {
        return logId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.log_id
     *
     * @param logId the value for restore_data_logs.log_id
     *
     * @mbg.generated
     */
    public void setLogId(String logId) {
        this.logId = logId == null ? null : logId.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column restore_data_logs.offset_index
     *
     * @return the value of restore_data_logs.offset_index
     *
     * @mbg.generated
     */
    public String getOffsetIndex() {
        return offsetIndex;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column restore_data_logs.offset_index
     *
     * @param offsetIndex the value for restore_data_logs.offset_index
     *
     * @mbg.generated
     */
    public void setOffsetIndex(String offsetIndex) {
        this.offsetIndex = offsetIndex == null ? null : offsetIndex.trim();
    }
}