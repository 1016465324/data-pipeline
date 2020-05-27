package com.clinbrain.source.mongo;

/**
 * @author p
 */
public class MongodbUtil {

//    public static Document convertRowToDoc(Row row, List<MetaColumn> columns) throws WriteRecordException {
//        Document doc = new Document();
//        for (int i = 0; i < columns.size(); i++) {
//            MetaColumn column = columns.get(i);
//            Object val = convertField(row.getField(i),column);
//            if (StringUtils.isNotEmpty(column.getSplitter())){
//                val = Arrays.asList(String.valueOf(val).split(column.getSplitter()));
//            }
//
//            doc.append(column.getName(),val);
//        }
//
//        return doc;
//    }
//
//    private static Object convertField(Object val,MetaColumn column){
//        ColumnType type = getType(column.getType());
//        switch (type) {
//            case VARCHAR:
//            case VARCHAR2:
//            case CHAR:
//            case TEXT:
//            case STRING:
//                if (val instanceof Date){
//                    SimpleDateFormat format = DateUtil.getDateTimeFormatter();
//                    val = format.format(val);
//                }
//                break;
//            case TIMESTAMP:
//                if (!(val instanceof Timestamp)) {
//                    val = DateUtil.columnToTimestamp(val, column.getTimeFormat());
//                }
//                break;
//            case DATE:
//                if (!(val instanceof Date)) {
//                    val = DateUtil.columnToDate(val, column.getTimeFormat());
//                }
//                break;
//            default:
//                if(val instanceof BigDecimal){
//                    val = ((BigDecimal) val).doubleValue();
//                }
//                break;
//        }
//
//        return val;
//    }
}
