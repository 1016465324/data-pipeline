/*
 Navicat Premium Data Transfer

 Source Server         : agui
 Source Server Type    : MySQL
 Source Server Version : 50729
 Source Host           : 192.168.0.240:3306
 Source Schema         : datahub

 Target Server Type    : MySQL
 Target Server Version : 50729
 File Encoding         : 65001

 Date: 23/04/2020 10:05:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for restore_data_logdetails
-- ----------------------------
DROP TABLE IF EXISTS `restore_data_logdetails`;
CREATE TABLE `restore_data_logdetails`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `logs_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `err_info` text CHARACTER SET utf8 COLLATE utf8_general_ci,
  `create_time` datetime(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for restore_data_logs
-- ----------------------------
DROP TABLE IF EXISTS `restore_data_logs`;
CREATE TABLE `restore_data_logs`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `log_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `offset_namespace` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'namespace + dbName',
  `offset_index` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数据批处理 时间值 + 下标偏移位值',
  `identification` int(11) DEFAULT NULL COMMENT '该条数据处理过程异常异常记录标识(1.常规问题，2.严重错误)',
  `create_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_class_define
-- ----------------------------
DROP TABLE IF EXISTS `t_class_define`;
CREATE TABLE `t_class_define`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ver_id` int(11) NOT NULL DEFAULT 1 COMMENT '版本id（t_meta_version）',
  `ds_id` int(11) NOT NULL,
  `class_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'cache 对象数据库对应class 名称',
  `class_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'cache 对象数据库对应class 类型,分为serial  persistent',
  `client_data_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `class_super` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'class 父类',
  `sql_rowid_private` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'N' COMMENT 'rowid 是否对jdbc/odbc 隐藏',
  `compile_namespace` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `runtime_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '默认存储对象自己关系为父子关系的时候找父类的存储位置用，跟类定义上的super没有必然联系',
  `time_changed` int(11) DEFAULT NULL COMMENT '类编译时间，根据时间戳获取结构变化',
  `time_created` int(11) DEFAULT NULL COMMENT '类编译时间，根据时间戳获取结构变化',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_class`(`ver_id`, `class_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10343 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '抓取的源表数据class meta信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_class_property_define
-- ----------------------------
DROP TABLE IF EXISTS `t_class_property_define`;
CREATE TABLE `t_class_property_define`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_id` int(11) NOT NULL,
  `class_id` int(11) NOT NULL DEFAULT 1 COMMENT 'class_id（t_class_define）',
  `class_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '属性所属的class名字',
  `property_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'cache 对象数据库对应class 属性名称',
  `property_collection` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'class 属性集合类型',
  `property_aliases` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'class 属性别名',
  `property_calculated` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'N' COMMENT '是否计算生成',
  `property_cardinality` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '基数描述，many,parent,one，child等',
  `runtime_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '属性定义，是否为基本数据类型',
  `sql_field_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '对应sql 定义字段名称',
  `sql_list_type` varchar(12) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'list,Delimiter，childsub',
  `sql_list_delimiter` varchar(12) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'list2string分隔符',
  `storable` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'Y' COMMENT '是否持久化',
  `storage_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '存储定义名称',
  `storage_delimiter` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数据分隔符，null 则算$lb 结构',
  `storage_subscript` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数据下标',
  `storage_piece` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数据Piece 位置',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_class_meta`(`class_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 107089 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '抓取的源表数据property meta信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_class_storage_define
-- ----------------------------
DROP TABLE IF EXISTS `t_class_storage_define`;
CREATE TABLE `t_class_storage_define`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_id` int(11) NOT NULL,
  `class_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'cache 对象数据库对应class 名称',
  `storage_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `storage_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'cache storage id',
  `sql_rowid_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'sql rowid 名字',
  `sql_child_sub` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'childsub名字',
  `data_location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT 'N' COMMENT 'global 名字',
  `stream_location` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `global_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `storage_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '存储类型 SQL存储/默认存储/序列化',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_class`(`class_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9817 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '抓取的源表数据class storage信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_data_schema
-- ----------------------------
DROP TABLE IF EXISTS `t_data_schema`;
CREATE TABLE `t_data_schema`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_id` int(11) UNSIGNED NOT NULL COMMENT 't_dbus_datasource 表ID',
  `schema_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'schema',
  `status` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '状态 active/inactive',
  `src_topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '源topic',
  `target_topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '目表topic',
  `db_vip` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'schema对应数据库虚IP',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `description` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'schema描述信息',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_dsid_sname`(`ds_id`, `schema_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_data_table
-- ----------------------------
DROP TABLE IF EXISTS `t_data_table`;
CREATE TABLE `t_data_table`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_id` int(11) UNSIGNED NOT NULL COMMENT 't_dbus_datasource 表ID',
  `schema_id` int(11) UNSIGNED NOT NULL COMMENT 't_tab_schema 表ID',
  `schema_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `table_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '表名',
  `table_name_alias` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '别名',
  `physical_table_regex` varchar(96) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `output_topic` varchar(96) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT 'kafka_topic',
  `ver_id` int(11) UNSIGNED DEFAULT NULL COMMENT '当前使用的meta版本ID',
  `status` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT 'abort' COMMENT 'ok,abort,inactive,waiting ok:正常使用;abort:需要抛弃该表的数据;waiting:等待拉全量;inactive:不可用 ',
  `meta_change_flg` int(1) DEFAULT 0 COMMENT 'meta变更标识，初始值为：0，表示代表没有发生变更，1：代表meta发生变更。该字段目前mysql appender模块使用。',
  `batch_id` int(11) DEFAULT 0 COMMENT '批次ID，用来标记拉全量的批次，每次拉全量会++，增量只使用该字段并不修改',
  `ver_change_history` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `ver_change_notice_flg` int(1) NOT NULL DEFAULT 0,
  `output_before_update_flg` int(1) NOT NULL DEFAULT 0,
  `description` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `fullpull_col` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '全量分片列:配置column名称',
  `fullpull_split_shard_size` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '全量分片大小配置:配置-1代表不分片',
  `fullpull_split_style` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '全量分片类型:MD5',
  `fullpull_condition` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '全量条件',
  `is_open` int(1) DEFAULT 0 COMMENT 'mongo是否展开节点,0不展开,1一级展开',
  `is_auto_complete` tinyint(4) DEFAULT 0 COMMENT 'mongoDB的表是否补全数据；如果开启，增量中更新操作会回查并补全数据',
  `class_id` int(11) UNSIGNED DEFAULT 0 COMMENT 't_dbus_classdefine 表ID cache 用',
  `class_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'cache 中类定义名称',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_sid_tabname`(`schema_id`, `table_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_dbus_datasource
-- ----------------------------
DROP TABLE IF EXISTS `t_dbus_datasource`;
CREATE TABLE `t_dbus_datasource`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据源名字',
  `ds_type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据源类型oracle/mysql/mongo/cache',
  `instance_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '状态：active/inactive',
  `ds_desc` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源描述',
  `topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `ctrl_topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `schema_topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `split_topic` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `master_url` varchar(4000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '主库jdbc连接串',
  `slave_url` varchar(4000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '备库jdbc连接串',
  `dbus_user` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dbus_pwd` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `update_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `ds_partition` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uidx_ds_name`(`ds_name`) USING BTREE,
  UNIQUE INDEX `ds_name`(`ds_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'dbus数据源配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_hospital_visit_data
-- ----------------------------
DROP TABLE IF EXISTS `t_hospital_visit_data`;
CREATE TABLE `t_hospital_visit_data`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `visit_id` int(11) UNSIGNED NOT NULL COMMENT '生成后的唯一就诊号',
  `visit_no` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '原系统中的就诊号',
  `hospital_no` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '医疗机构组织代码',
  `system_source` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '系统来源,体检，门诊，住院等',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_visit_id`(`visit_id`) USING BTREE,
  UNIQUE INDEX `idx_visit_no`(`hospital_no`, `system_source`, `visit_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_storage_subscript_define
-- ----------------------------
DROP TABLE IF EXISTS `t_storage_subscript_define`;
CREATE TABLE `t_storage_subscript_define`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ds_id` int(11) NOT NULL,
  `class_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '类名称',
  `storage_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '存储定义名称',
  `access_type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'node 存取类型，比较复杂，piece 和global 比较复杂，暂时不处理',
  `expression` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'node表达式',
  `is_rowid` int(2) NOT NULL DEFAULT 0 COMMENT '是否为RowID下标，需计算处理',
  `sort_id` int(2) NOT NULL COMMENT 'node 顺序',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_storage_subscrip`(`storage_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16384 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '抓取的源表数据 storage_subscript meta信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_table_meta
-- ----------------------------
DROP TABLE IF EXISTS `t_table_meta`;
CREATE TABLE `t_table_meta`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `ver_id` int(11) NOT NULL COMMENT '版本id（t_meta_version）',
  `ds_id` int(11) NOT NULL,
  `table_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `column_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '替换特殊字符后生成的列名',
  `original_column_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '数据库中原始的列名',
  `column_id` int(4) NOT NULL COMMENT '列ID',
  `internal_column_id` int(11) DEFAULT NULL,
  `hidden_column` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `virtual_column` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `original_ser` int(11) NOT NULL COMMENT '源表变更序号',
  `data_type` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据类型',
  `data_length` bigint(20) NOT NULL COMMENT '数据长度',
  `data_precision` int(11) DEFAULT NULL COMMENT '数据精度',
  `data_scale` int(11) DEFAULT NULL COMMENT '小数部分长度',
  `char_length` int(11) DEFAULT NULL,
  `char_used` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  `nullable` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '是否可空Y/N',
  `is_pk` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '是否为主键Y/N',
  `pk_position` int(2) DEFAULT NULL COMMENT '主键的顺序',
  `alter_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建/修改的时间',
  `create_time` timestamp(0) DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `comments` varchar(2048) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '列注释',
  `default_value` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_meta`(`ver_id`, `column_name`, `original_ser`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '抓取的源表数据meta信息' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
