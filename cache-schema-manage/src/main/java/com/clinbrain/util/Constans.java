package com.clinbrain.util;

public interface Constans {

    String query_sql = "select * from ClinBrain.JournalTail_RecordList('%s', %s)";

    String query_logs_sql = "select * from ClinBrain.SP_Global_By_Mask('CDCLog','^CDCDataLog(\"%s\",%s,%s','',1,1,0)";

    String query_globals_sql = "select expression from t_storage_subscript_define GROUP BY class_name HAVING class_name = '%s' ORDER BY sort_id asc";

    String query_storageInfo_sql = "select a.global_name as global_name,a.storage_name as storage_name,b.class_name as class_name,GROUP_CONCAT(b.expression SEPARATOR '||') as expressionStr from t_class_storage_define a left JOIN t_storage_subscript_define b on a.storage_name = b.storage_name group by a.global_name,a.storage_name,b.class_name HAVING b.class_name is not null";

    String query_classinfo_sql = "select class_name,GROUP_CONCAT(property_name SEPARATOR '||' ) AS propertyStr  from t_class_property_define GROUP BY class_name";

    String query_expressionInfo_sql = "select a.global_name,a.class_name,GROUP_CONCAT(b.expression SEPARATOR '||' ) AS expressionStr,GROUP_CONCAT(b.is_rowid SEPARATOR '||' ) AS isRowidStr, GROUP_CONCAT(b.sort_id SEPARATOR '||' ) AS sortIdStr from t_class_storage_define a left join t_storage_subscript_define b on a.class_name = b.class_name group by a.class_name";

}
