package com.clinbrain.model;

import java.util.Date;

public class DataTable {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.id
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.ds_id
     *
     * @mbg.generated
     */
    private Integer dsId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.schema_id
     *
     * @mbg.generated
     */
    private Integer schemaId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.schema_name
     *
     * @mbg.generated
     */
    private String schemaName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.table_name
     *
     * @mbg.generated
     */
    private String tableName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.table_name_alias
     *
     * @mbg.generated
     */
    private String tableNameAlias;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.physical_table_regex
     *
     * @mbg.generated
     */
    private String physicalTableRegex;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.output_topic
     *
     * @mbg.generated
     */
    private String outputTopic;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.ver_id
     *
     * @mbg.generated
     */
    private Integer verId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.status
     *
     * @mbg.generated
     */
    private String status;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.meta_change_flg
     *
     * @mbg.generated
     */
    private Integer metaChangeFlg;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.batch_id
     *
     * @mbg.generated
     */
    private Integer batchId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.ver_change_history
     *
     * @mbg.generated
     */
    private String verChangeHistory;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.ver_change_notice_flg
     *
     * @mbg.generated
     */
    private Integer verChangeNoticeFlg;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.output_before_update_flg
     *
     * @mbg.generated
     */
    private Integer outputBeforeUpdateFlg;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.description
     *
     * @mbg.generated
     */
    private String description;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.fullpull_col
     *
     * @mbg.generated
     */
    private String fullpullCol;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.fullpull_split_shard_size
     *
     * @mbg.generated
     */
    private String fullpullSplitShardSize;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.fullpull_split_style
     *
     * @mbg.generated
     */
    private String fullpullSplitStyle;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.fullpull_condition
     *
     * @mbg.generated
     */
    private String fullpullCondition;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.is_open
     *
     * @mbg.generated
     */
    private Integer isOpen;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.is_auto_complete
     *
     * @mbg.generated
     */
    private Byte isAutoComplete;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.class_id
     *
     * @mbg.generated
     */
    private Integer classId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.class_name
     *
     * @mbg.generated
     */
    private String className;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_data_table.create_time
     *
     * @mbg.generated
     */
    private Date createTime;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.id
     *
     * @return the value of t_data_table.id
     *
     * @mbg.generated
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.id
     *
     * @param id the value for t_data_table.id
     *
     * @mbg.generated
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.ds_id
     *
     * @return the value of t_data_table.ds_id
     *
     * @mbg.generated
     */
    public Integer getDsId() {
        return dsId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.ds_id
     *
     * @param dsId the value for t_data_table.ds_id
     *
     * @mbg.generated
     */
    public void setDsId(Integer dsId) {
        this.dsId = dsId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.schema_id
     *
     * @return the value of t_data_table.schema_id
     *
     * @mbg.generated
     */
    public Integer getSchemaId() {
        return schemaId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.schema_id
     *
     * @param schemaId the value for t_data_table.schema_id
     *
     * @mbg.generated
     */
    public void setSchemaId(Integer schemaId) {
        this.schemaId = schemaId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.schema_name
     *
     * @return the value of t_data_table.schema_name
     *
     * @mbg.generated
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.schema_name
     *
     * @param schemaName the value for t_data_table.schema_name
     *
     * @mbg.generated
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName == null ? null : schemaName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.table_name
     *
     * @return the value of t_data_table.table_name
     *
     * @mbg.generated
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.table_name
     *
     * @param tableName the value for t_data_table.table_name
     *
     * @mbg.generated
     */
    public void setTableName(String tableName) {
        this.tableName = tableName == null ? null : tableName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.table_name_alias
     *
     * @return the value of t_data_table.table_name_alias
     *
     * @mbg.generated
     */
    public String getTableNameAlias() {
        return tableNameAlias;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.table_name_alias
     *
     * @param tableNameAlias the value for t_data_table.table_name_alias
     *
     * @mbg.generated
     */
    public void setTableNameAlias(String tableNameAlias) {
        this.tableNameAlias = tableNameAlias == null ? null : tableNameAlias.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.physical_table_regex
     *
     * @return the value of t_data_table.physical_table_regex
     *
     * @mbg.generated
     */
    public String getPhysicalTableRegex() {
        return physicalTableRegex;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.physical_table_regex
     *
     * @param physicalTableRegex the value for t_data_table.physical_table_regex
     *
     * @mbg.generated
     */
    public void setPhysicalTableRegex(String physicalTableRegex) {
        this.physicalTableRegex = physicalTableRegex == null ? null : physicalTableRegex.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.output_topic
     *
     * @return the value of t_data_table.output_topic
     *
     * @mbg.generated
     */
    public String getOutputTopic() {
        return outputTopic;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.output_topic
     *
     * @param outputTopic the value for t_data_table.output_topic
     *
     * @mbg.generated
     */
    public void setOutputTopic(String outputTopic) {
        this.outputTopic = outputTopic == null ? null : outputTopic.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.ver_id
     *
     * @return the value of t_data_table.ver_id
     *
     * @mbg.generated
     */
    public Integer getVerId() {
        return verId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.ver_id
     *
     * @param verId the value for t_data_table.ver_id
     *
     * @mbg.generated
     */
    public void setVerId(Integer verId) {
        this.verId = verId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.status
     *
     * @return the value of t_data_table.status
     *
     * @mbg.generated
     */
    public String getStatus() {
        return status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.status
     *
     * @param status the value for t_data_table.status
     *
     * @mbg.generated
     */
    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.meta_change_flg
     *
     * @return the value of t_data_table.meta_change_flg
     *
     * @mbg.generated
     */
    public Integer getMetaChangeFlg() {
        return metaChangeFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.meta_change_flg
     *
     * @param metaChangeFlg the value for t_data_table.meta_change_flg
     *
     * @mbg.generated
     */
    public void setMetaChangeFlg(Integer metaChangeFlg) {
        this.metaChangeFlg = metaChangeFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.batch_id
     *
     * @return the value of t_data_table.batch_id
     *
     * @mbg.generated
     */
    public Integer getBatchId() {
        return batchId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.batch_id
     *
     * @param batchId the value for t_data_table.batch_id
     *
     * @mbg.generated
     */
    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.ver_change_history
     *
     * @return the value of t_data_table.ver_change_history
     *
     * @mbg.generated
     */
    public String getVerChangeHistory() {
        return verChangeHistory;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.ver_change_history
     *
     * @param verChangeHistory the value for t_data_table.ver_change_history
     *
     * @mbg.generated
     */
    public void setVerChangeHistory(String verChangeHistory) {
        this.verChangeHistory = verChangeHistory == null ? null : verChangeHistory.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.ver_change_notice_flg
     *
     * @return the value of t_data_table.ver_change_notice_flg
     *
     * @mbg.generated
     */
    public Integer getVerChangeNoticeFlg() {
        return verChangeNoticeFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.ver_change_notice_flg
     *
     * @param verChangeNoticeFlg the value for t_data_table.ver_change_notice_flg
     *
     * @mbg.generated
     */
    public void setVerChangeNoticeFlg(Integer verChangeNoticeFlg) {
        this.verChangeNoticeFlg = verChangeNoticeFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.output_before_update_flg
     *
     * @return the value of t_data_table.output_before_update_flg
     *
     * @mbg.generated
     */
    public Integer getOutputBeforeUpdateFlg() {
        return outputBeforeUpdateFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.output_before_update_flg
     *
     * @param outputBeforeUpdateFlg the value for t_data_table.output_before_update_flg
     *
     * @mbg.generated
     */
    public void setOutputBeforeUpdateFlg(Integer outputBeforeUpdateFlg) {
        this.outputBeforeUpdateFlg = outputBeforeUpdateFlg;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.description
     *
     * @return the value of t_data_table.description
     *
     * @mbg.generated
     */
    public String getDescription() {
        return description;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.description
     *
     * @param description the value for t_data_table.description
     *
     * @mbg.generated
     */
    public void setDescription(String description) {
        this.description = description == null ? null : description.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.fullpull_col
     *
     * @return the value of t_data_table.fullpull_col
     *
     * @mbg.generated
     */
    public String getFullpullCol() {
        return fullpullCol;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.fullpull_col
     *
     * @param fullpullCol the value for t_data_table.fullpull_col
     *
     * @mbg.generated
     */
    public void setFullpullCol(String fullpullCol) {
        this.fullpullCol = fullpullCol == null ? null : fullpullCol.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.fullpull_split_shard_size
     *
     * @return the value of t_data_table.fullpull_split_shard_size
     *
     * @mbg.generated
     */
    public String getFullpullSplitShardSize() {
        return fullpullSplitShardSize;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.fullpull_split_shard_size
     *
     * @param fullpullSplitShardSize the value for t_data_table.fullpull_split_shard_size
     *
     * @mbg.generated
     */
    public void setFullpullSplitShardSize(String fullpullSplitShardSize) {
        this.fullpullSplitShardSize = fullpullSplitShardSize == null ? null : fullpullSplitShardSize.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.fullpull_split_style
     *
     * @return the value of t_data_table.fullpull_split_style
     *
     * @mbg.generated
     */
    public String getFullpullSplitStyle() {
        return fullpullSplitStyle;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.fullpull_split_style
     *
     * @param fullpullSplitStyle the value for t_data_table.fullpull_split_style
     *
     * @mbg.generated
     */
    public void setFullpullSplitStyle(String fullpullSplitStyle) {
        this.fullpullSplitStyle = fullpullSplitStyle == null ? null : fullpullSplitStyle.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.fullpull_condition
     *
     * @return the value of t_data_table.fullpull_condition
     *
     * @mbg.generated
     */
    public String getFullpullCondition() {
        return fullpullCondition;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.fullpull_condition
     *
     * @param fullpullCondition the value for t_data_table.fullpull_condition
     *
     * @mbg.generated
     */
    public void setFullpullCondition(String fullpullCondition) {
        this.fullpullCondition = fullpullCondition == null ? null : fullpullCondition.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.is_open
     *
     * @return the value of t_data_table.is_open
     *
     * @mbg.generated
     */
    public Integer getIsOpen() {
        return isOpen;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.is_open
     *
     * @param isOpen the value for t_data_table.is_open
     *
     * @mbg.generated
     */
    public void setIsOpen(Integer isOpen) {
        this.isOpen = isOpen;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.is_auto_complete
     *
     * @return the value of t_data_table.is_auto_complete
     *
     * @mbg.generated
     */
    public Byte getIsAutoComplete() {
        return isAutoComplete;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.is_auto_complete
     *
     * @param isAutoComplete the value for t_data_table.is_auto_complete
     *
     * @mbg.generated
     */
    public void setIsAutoComplete(Byte isAutoComplete) {
        this.isAutoComplete = isAutoComplete;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.class_id
     *
     * @return the value of t_data_table.class_id
     *
     * @mbg.generated
     */
    public Integer getClassId() {
        return classId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.class_id
     *
     * @param classId the value for t_data_table.class_id
     *
     * @mbg.generated
     */
    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.class_name
     *
     * @return the value of t_data_table.class_name
     *
     * @mbg.generated
     */
    public String getClassName() {
        return className;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.class_name
     *
     * @param className the value for t_data_table.class_name
     *
     * @mbg.generated
     */
    public void setClassName(String className) {
        this.className = className == null ? null : className.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_data_table.create_time
     *
     * @return the value of t_data_table.create_time
     *
     * @mbg.generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_data_table.create_time
     *
     * @param createTime the value for t_data_table.create_time
     *
     * @mbg.generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}