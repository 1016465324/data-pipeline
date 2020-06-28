package com.clinbrain.model;

import java.util.Date;

public class DataSource {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.id
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.ds_name
     *
     * @mbg.generated
     */
    private String dsName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.ds_type
     *
     * @mbg.generated
     */
    private String dsType;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.instance_name
     *
     * @mbg.generated
     */
    private String instanceName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.status
     *
     * @mbg.generated
     */
    private String status;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.ds_desc
     *
     * @mbg.generated
     */
    private String dsDesc;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.topic
     *
     * @mbg.generated
     */
    private String topic;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.ctrl_topic
     *
     * @mbg.generated
     */
    private String ctrlTopic;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.schema_topic
     *
     * @mbg.generated
     */
    private String schemaTopic;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.split_topic
     *
     * @mbg.generated
     */
    private String splitTopic;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.master_url
     *
     * @mbg.generated
     */
    private String masterUrl;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.slave_url
     *
     * @mbg.generated
     */
    private String slaveUrl;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.dbus_user
     *
     * @mbg.generated
     */
    private String dbusUser;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.dbus_pwd
     *
     * @mbg.generated
     */
    private String dbusPwd;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.update_time
     *
     * @mbg.generated
     */
    private Date updateTime;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.create_time
     *
     * @mbg.generated
     */
    private Date createTime;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_dbus_datasource.ds_partition
     *
     * @mbg.generated
     */
    private String dsPartition;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.id
     *
     * @return the value of t_dbus_datasource.id
     *
     * @mbg.generated
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.id
     *
     * @param id the value for t_dbus_datasource.id
     *
     * @mbg.generated
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.ds_name
     *
     * @return the value of t_dbus_datasource.ds_name
     *
     * @mbg.generated
     */
    public String getDsName() {
        return dsName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.ds_name
     *
     * @param dsName the value for t_dbus_datasource.ds_name
     *
     * @mbg.generated
     */
    public void setDsName(String dsName) {
        this.dsName = dsName == null ? null : dsName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.ds_type
     *
     * @return the value of t_dbus_datasource.ds_type
     *
     * @mbg.generated
     */
    public String getDsType() {
        return dsType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.ds_type
     *
     * @param dsType the value for t_dbus_datasource.ds_type
     *
     * @mbg.generated
     */
    public void setDsType(String dsType) {
        this.dsType = dsType == null ? null : dsType.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.instance_name
     *
     * @return the value of t_dbus_datasource.instance_name
     *
     * @mbg.generated
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.instance_name
     *
     * @param instanceName the value for t_dbus_datasource.instance_name
     *
     * @mbg.generated
     */
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName == null ? null : instanceName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.status
     *
     * @return the value of t_dbus_datasource.status
     *
     * @mbg.generated
     */
    public String getStatus() {
        return status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.status
     *
     * @param status the value for t_dbus_datasource.status
     *
     * @mbg.generated
     */
    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.ds_desc
     *
     * @return the value of t_dbus_datasource.ds_desc
     *
     * @mbg.generated
     */
    public String getDsDesc() {
        return dsDesc;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.ds_desc
     *
     * @param dsDesc the value for t_dbus_datasource.ds_desc
     *
     * @mbg.generated
     */
    public void setDsDesc(String dsDesc) {
        this.dsDesc = dsDesc == null ? null : dsDesc.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.topic
     *
     * @return the value of t_dbus_datasource.topic
     *
     * @mbg.generated
     */
    public String getTopic() {
        return topic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.topic
     *
     * @param topic the value for t_dbus_datasource.topic
     *
     * @mbg.generated
     */
    public void setTopic(String topic) {
        this.topic = topic == null ? null : topic.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.ctrl_topic
     *
     * @return the value of t_dbus_datasource.ctrl_topic
     *
     * @mbg.generated
     */
    public String getCtrlTopic() {
        return ctrlTopic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.ctrl_topic
     *
     * @param ctrlTopic the value for t_dbus_datasource.ctrl_topic
     *
     * @mbg.generated
     */
    public void setCtrlTopic(String ctrlTopic) {
        this.ctrlTopic = ctrlTopic == null ? null : ctrlTopic.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.schema_topic
     *
     * @return the value of t_dbus_datasource.schema_topic
     *
     * @mbg.generated
     */
    public String getSchemaTopic() {
        return schemaTopic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.schema_topic
     *
     * @param schemaTopic the value for t_dbus_datasource.schema_topic
     *
     * @mbg.generated
     */
    public void setSchemaTopic(String schemaTopic) {
        this.schemaTopic = schemaTopic == null ? null : schemaTopic.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.split_topic
     *
     * @return the value of t_dbus_datasource.split_topic
     *
     * @mbg.generated
     */
    public String getSplitTopic() {
        return splitTopic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.split_topic
     *
     * @param splitTopic the value for t_dbus_datasource.split_topic
     *
     * @mbg.generated
     */
    public void setSplitTopic(String splitTopic) {
        this.splitTopic = splitTopic == null ? null : splitTopic.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.master_url
     *
     * @return the value of t_dbus_datasource.master_url
     *
     * @mbg.generated
     */
    public String getMasterUrl() {
        return masterUrl;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.master_url
     *
     * @param masterUrl the value for t_dbus_datasource.master_url
     *
     * @mbg.generated
     */
    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl == null ? null : masterUrl.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.slave_url
     *
     * @return the value of t_dbus_datasource.slave_url
     *
     * @mbg.generated
     */
    public String getSlaveUrl() {
        return slaveUrl;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.slave_url
     *
     * @param slaveUrl the value for t_dbus_datasource.slave_url
     *
     * @mbg.generated
     */
    public void setSlaveUrl(String slaveUrl) {
        this.slaveUrl = slaveUrl == null ? null : slaveUrl.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.dbus_user
     *
     * @return the value of t_dbus_datasource.dbus_user
     *
     * @mbg.generated
     */
    public String getDbusUser() {
        return dbusUser;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.dbus_user
     *
     * @param dbusUser the value for t_dbus_datasource.dbus_user
     *
     * @mbg.generated
     */
    public void setDbusUser(String dbusUser) {
        this.dbusUser = dbusUser == null ? null : dbusUser.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.dbus_pwd
     *
     * @return the value of t_dbus_datasource.dbus_pwd
     *
     * @mbg.generated
     */
    public String getDbusPwd() {
        return dbusPwd;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.dbus_pwd
     *
     * @param dbusPwd the value for t_dbus_datasource.dbus_pwd
     *
     * @mbg.generated
     */
    public void setDbusPwd(String dbusPwd) {
        this.dbusPwd = dbusPwd == null ? null : dbusPwd.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.update_time
     *
     * @return the value of t_dbus_datasource.update_time
     *
     * @mbg.generated
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.update_time
     *
     * @param updateTime the value for t_dbus_datasource.update_time
     *
     * @mbg.generated
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.create_time
     *
     * @return the value of t_dbus_datasource.create_time
     *
     * @mbg.generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.create_time
     *
     * @param createTime the value for t_dbus_datasource.create_time
     *
     * @mbg.generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_dbus_datasource.ds_partition
     *
     * @return the value of t_dbus_datasource.ds_partition
     *
     * @mbg.generated
     */
    public String getDsPartition() {
        return dsPartition;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_dbus_datasource.ds_partition
     *
     * @param dsPartition the value for t_dbus_datasource.ds_partition
     *
     * @mbg.generated
     */
    public void setDsPartition(String dsPartition) {
        this.dsPartition = dsPartition == null ? null : dsPartition.trim();
    }
}