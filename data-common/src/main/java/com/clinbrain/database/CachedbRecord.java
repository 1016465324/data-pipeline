package com.clinbrain.database;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clinbrain.global.GlobalInfo;
import com.intersys.cache.Dataholder;
import com.intersys.objects.CacheException;
import com.intersys.objects.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intersys.objects.Database.RET_PRIM;

public class CachedbRecord implements Serializable {
    private static Logger logger = LoggerFactory.getLogger(CachedbRecord.class);

    private String tableName;
    private String entityName;
    private List<GlobalInfo> globals;
    private GlobalInfo cdcGlobal;

    public CachedbRecord(String tableName, String entityName, List<GlobalInfo> globals, GlobalInfo cdcGlobal) {
        this.tableName = tableName;
        this.entityName = entityName;
        this.globals = globals;
        this.cdcGlobal = cdcGlobal;
    }

    public List<String> buildRecord(Map<String, TableMeta> allDBTableMeta,
                                    String namespace,
                                    Database cacheDatabase,
                                    String recordType,
                                    String cdclogValue) {
        Map<String, String> values = new HashMap<>();

        // TODO: 2019/12/17 不要cdclogValue，但是要处理recordType的问题K ，12月18号 处理 为K时目前只填充rowkey其他为null
        if (recordType.equalsIgnoreCase("s")) {
            if (globals.size() != 1) {
                //DHC-LISDATA的GLOBAL数据[{"item":["^|\"dhc-app\"|dbo.RPVisitNumberRecordD(1364490)"],"table":"rp_visitnumberrecord"}]查询报错DHC-LISDATA的GLOBAL数据[{"item":["^|\"dhc-app\"|dbo.RPVisitNumberRecordD(1364490)"],"table":"rp_visitnumberrecord"}]查询报错
                //根据CDCLOG中的DBName获取该日志所属的所有namespace，在其中的namespace中查找，找不到返回null
                String data = null;
                String queryParam = buildRequestJSON(namespace);
                try {
                    data = queryRecodrd(cacheDatabase, queryParam);
                    logger.info("query {} in namespace {} success.", queryParam, namespace);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.warn("can't query {} in namespace {}. exception: {}", queryParam, namespace, e.getMessage());
                }

                if (null == data || data.isEmpty()) {
                    return null;
                }

//            String data = "[{\"table\":\"rp_visitnumber\",\"data\":[\"^2451320^0032494640^3^144904077^EP0004286692^20191122^57409^^^513125195402102025^^方仕秀^方仕秀^2^19540210^^65^1^^1^48^661^20191122^58794^^014床^脑梗死,肺部感染,高血压^18383516217^^^^^^^^^^^^^^^^^^^^^0^20191122^59143^10627^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^5^^^^\"]}]";
                logger.info("data: {}.", data);
                JSONArray results = JSON.parseArray(data);
                JSONObject result = results.getJSONObject(0);
                JSONArray dataArray = result.getJSONArray("data");

                for (int i = 0; i < dataArray.size(); i++) {
                    if (!setGlobalFieldValue(values, dataArray.get(i).toString(), globals.get(i))) {
                        logger.warn("{} query result error. value {} is different with global field.", queryParam, data);
                        return null;
                    }
                }
            } else {
                if (null == cdclogValue) {
                    logger.warn("cdclog Val is null.");
                    return null;
                }

                setGlobalFieldValue(values, cdclogValue, globals.get(0));
            }

        }

        // TODO: 2019/11/30 因为sql查询元数据时名字存的 schema.table 这个可能跟global配置中的表面不一致，此问题需确认，以dhc_patientbill为例
        logger.info("tableName: {}.", tableName);
        List<String> tableRecord = new ArrayList<>();
        TableMeta tableMeta = allDBTableMeta.get(tableName);
        if (null == tableMeta) {
            logger.warn("table {} metadata not exist.", tableName);
            return null;
        }

        StringBuilder rowKey = new StringBuilder();
        for (Integer index : globals.get(0).getAllKeyIndex()) {
            String tmp = globals.get(0).getGlobalFields()[index];
            if (tmp.equalsIgnoreCase("primarykey")) {
                for (TableMeta.ColumnMeta columnMeta : tableMeta.getPkColumns()) {
                    values.put(columnMeta.getName(), cdcGlobal.getGlobalFields()[index]);
                }
            } else {
                for (TableMeta.ColumnMeta columnMeta : tableMeta.getPkColumns()) {
                    if (columnMeta.name.equalsIgnoreCase(tmp)) {
                        values.put(columnMeta.getName(), cdcGlobal.getGlobalFields()[index]);
                    }
                }
            }

            rowKey.append(cdcGlobal.getGlobalFields()[index]);
        }

        tableRecord.add("rowkey");
        tableRecord.add(rowKey.toString());
        List<TableMeta.ColumnMeta> allColumns = tableMeta.getAllColumns();
        for (TableMeta.ColumnMeta column : allColumns) {
            tableRecord.add(column.getName());
            tableRecord.add(values.get(column.getName()));
        }

        return tableRecord;
    }

    public String queryObject(Database cacheDatabase) throws CacheException {
        if (null == cacheDatabase) {
            return null;
        }

        String id = buildRowID();
        if (null == id) {
            return null;
        }

        Dataholder[] args = new Dataholder[2];
        args[0] = new Dataholder(entityName);
        args[1] = new Dataholder(id);
        logger.info("query rowid " + id + " in entity class " + entityName);
        Dataholder res = cacheDatabase.runClassMethod("cdc.JSONUtil", "ClassJSON", args, RET_PRIM);
        return res.getString();
    }

    private String buildRowID() {
        StringBuilder sb = new StringBuilder();
        for (Integer index : globals.get(0).getAllKeyIndex()) {
            sb.append(cdcGlobal.getGlobalFields()[index]);
            sb.append("||");
        }

        if (0 == sb.length()) {
            logger.info("rowID is null, allKeyIndex size: " + globals.get(0).getAllKeyIndex().size());
            return null;
        }

        String rowID = sb.delete(sb.lastIndexOf("||"), sb.length()).toString();
        //如果rowid以||分割，最后一个值为0则跳过
        String[] fields = rowID.split("\\|\\|");
        if (fields[fields.length - 1].equals("0")) {
            logger.info("row id split by || and then last value is 0");
            return null;
        }

        logger.info("rowID is " + rowID);
        return rowID;
    }

    private String queryRecodrd(Database cacheDatabase, String request) throws CacheException {
        Dataholder[] args = new Dataholder[1];
        args[0] = new Dataholder(request);
        Dataholder res = cacheDatabase.runClassMethod("cdc.CDCLog", "queryRecodrd", args, RET_PRIM);
        return res.getString();
    }

    private String buildRequestJSON(String namespace) {
        JSONArray array = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("table", tableName);
        JSONArray globalArray = new JSONArray();
        for (GlobalInfo globalInfo : globals) {
            String globalStr = globalInfo.buildGlobal(cdcGlobal);
            // TODO: 2019/12/11 通过CDCLOG的DBName取出其映射到的namespace中查找
            globalArray.add("^|\"" + namespace + "\"|" + globalStr.substring(1));
//            globalArray.add("^|\"" + "dhc-app" + "\"|" + globalStr.substring(1));
        }
        jsonObject.put("item", globalArray);
        array.add(jsonObject);

        String queryParam = array.toJSONString();
        logger.info("queryParam: {}.", queryParam);
        return queryParam;
    }

    private boolean setGlobalFieldValue(Map<String, String> values, String globalValue, GlobalInfo globalInfo) {
        boolean fill = false;
        String[] valueArray;
        if (globalValue.endsWith("^")) {
            globalValue += "0";
            fill = true;
        }
        // TODO: 2019/12/10 表对应的配置global值为空，global列会错位，目前做法是如果global的第一个字段的下标不等于1，则认为要跳过值的第一个^符合
        //DHC-APP RP_VisitNumber导出的GLOBAL配置中RowID的global是空的
        if (globalInfo.getAllFieldInfo().get(0).getIndex() != 1) {
            valueArray = globalValue.substring(1).split("\\^");
        } else {
            valueArray = globalValue.split("\\^");
        }
//            valueArray = globalValue.split("\\^");

        if (fill) {
            valueArray[valueArray.length - 1] = "";
        }

        // TODO: 2019/12/17 如果出现以下情况说明globalValue的值不对，如
        //  message: {"dBName":":mirror:NEWMIRROR:DHC-DATA","globalName":"DHCICUPara(85922,\"I\",1,\"D\",0)","iD":"2073433364","recordType":"S","tS":"2019-12-05 23:39:22.344","val":"1"}
        // 目前修改所有值通过查接口获取，以上值是cdclog的val，查询所得结果应该不会出现以下情况
//        if (valueArray.length != globalInfo.getAllFieldInfo().size()) {
//            return false;
//        }

        for (int j = 0; j < valueArray.length; j++) {
            values.put(globalInfo.getAllFieldInfo().get(j).getFieldName(), valueArray[j]);
        }

        return true;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
