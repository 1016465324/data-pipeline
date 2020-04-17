package com.clinbrain.rsultMessage;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageBuilder {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public Message message;
    public String version;

    public int umsFixedFields = 0;

    public MessageBuilder() {
        //缺省是1.3版本
        this(MessgeConstants.VERSION_13);
    }

    public MessageBuilder(String intendVersion) {
        if (intendVersion.equals(MessgeConstants.VERSION_12)) {
            version = MessgeConstants.VERSION_12;
        } else if (intendVersion.equals(MessgeConstants.VERSION_14)) {
            version = MessgeConstants.VERSION_14;
        } else {
            version = MessgeConstants.VERSION_13;
        }
    }

    public MessageBuilder build(Message.ProtocolType type, String schemaNs, int batchNo) {
        if (version.equals(MessgeConstants.VERSION_12)) {
            message = new Message12(version, type, schemaNs);
        } else if (version.equals(MessgeConstants.VERSION_14)) {
            message = new Message14(version, type, schemaNs, batchNo);
        } else {
            message = new Message13(version, type, schemaNs, batchNo);
        }

        //TODO 暂时不添加schema中的默认值
//        switch (type) {
//            case DATA_INCREMENT_DATA:
//            case DATA_INITIAL_DATA:
//                // 添加schema中的默认值
//                message.getSchema().addField(Message.Field._UMS_ID_, DataType.LONG, false);
//                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
//                message.getSchema().addField(Message.Field._UMS_OP_, DataType.STRING, false);
//                umsFixedFields = 3;
//                switch (version) {
//                    case MessgeConstants.VERSION_12:
//                        break;
//                    default:
//                        // 1.3 or later
//                        message.getSchema().addField(Message.Field._UMS_UID_, DataType.STRING, false);
//                        umsFixedFields++;
//                        break;
//                }
//                break;
//            case DATA_INCREMENT_TERMINATION:
//                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
//                umsFixedFields = 1;
//                break;
//            case DATA_INCREMENT_HEARTBEAT:
//                message.getSchema().addField(Message.Field._UMS_ID_, DataType.LONG, false);
//                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
//                umsFixedFields = 2;
//                break;
//            default:
//                break;
//        }

        return this;
    }

    /**
     * DbusMessager14 unsetFeild的处理
     *
     * @param type
     * @param schemaNs
     * @param batchNo
     * @param unsetFiled
     * @return
     */
    public MessageBuilder build(Message.ProtocolType type, String schemaNs, int batchNo, List<Message.Field> unsetFiled) {
        if (!version.equals(MessgeConstants.VERSION_14))
            return null;

        message = new Message14(version, type, schemaNs, batchNo, unsetFiled);
        // 添加schema中的默认值
        switch (type) {
            case DATA_INCREMENT_DATA:
            case DATA_INITIAL_DATA:
                // 添加schema中的默认值
                message.getSchema().addField(Message.Field._UMS_ID_, DataType.LONG, false);
                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
                message.getSchema().addField(Message.Field._UMS_OP_, DataType.STRING, false);
                message.getSchema().addField(Message.Field._UMS_UID_, DataType.STRING, false);
                umsFixedFields = 4;
                break;
            case DATA_INCREMENT_TERMINATION:
                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
                umsFixedFields = 1;
                break;
            case DATA_INCREMENT_HEARTBEAT:
                message.getSchema().addField(Message.Field._UMS_ID_, DataType.LONG, false);
                message.getSchema().addField(Message.Field._UMS_TS_, DataType.DATETIME, false);
                umsFixedFields = 2;
                break;
            default:
                break;
        }
        return this;
    }

    public String buildNameSpace(String datasourceNs, String dbSchema, String table, int ver) {
        return Joiner.on(".").join(datasourceNs, dbSchema, table, ver, "0", "0");
    }

    public String buildNameSpace(String datasourceNs, String dbSchema, String table, int ver, String partitionTable) {
        return Joiner.on(".").join(datasourceNs, dbSchema, table, ver, "0", partitionTable);
    }

    public MessageBuilder appendSchema(String name, DataType type, boolean nullable) {
        validateState();
        message.getSchema().addField(name, type, nullable);
        return this;
    }

    public MessageBuilder appendSchema(String name, DataType type, boolean nullable, boolean encoded) {
        validateState();
        message.getSchema().addField(name, type, nullable, encoded);
        return this;
    }

    public MessageBuilder appendPayload(Object[] tuple) {
        validateState();
//      validateAndConvert(tuple);
        message.addTuple(tuple);

        return this;
    }

    public Message getMessage() {
        validateState(); // 验证
        return message;
    }

    public int getUmsFixedFields() {
        return umsFixedFields;
    }

    /**
     * 校验输入数据和schema是否匹配
     *
     * @param tuple
     */
    private void validateAndConvert(Object[] tuple) {
        List<Message.Field> fields = message.getSchema().getFields();

        if (tuple.length != fields.size()) {
            throw new IllegalArgumentException(String.format("Data fields length != schema field length!!  data_length=%d, schema_length=%d",
                    tuple.length, fields.size()));
        }

        Message.Field field;
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            Object value = tuple[i];
            // 增加默认值的处理后不再进行非空校验
            // 判断是否为空
//            if (value == null) {
//                if (field.isNullable()) continue;
//                throw new NullPointerException("Field " + field.getName() + " can not bet set a null value");
//            }

            try {
                tuple[i] = DataType.convertValueByDataType(field.dataType(), value);
            } catch (DataTypeException e) {
                logger.error("Data type '{}' of filed '{}' not match with String", field.dataType(), field.getName());
                throw e;
            } catch (NullPointerException e) {
                logger.error("NullPointerException Data type '{}' of filed '{}'", field.dataType(), field.getName());
                throw e;
            } catch (Exception e) {
                String columns = fields.stream().map(f -> f.getName() + ":" + f.getType()).collect(Collectors.joining(","));
                logger.error("index '{}',namespace '{}', data type '{}' , filed '{}' , value '{}', columns '{}' ", i, message.getSchema().getNamespace(),
                        field.dataType(), field.getName(), value, columns, e);
                throw e;
            }
        }
    }

    private void validateState() {
        if (message == null) {
            throw new IllegalStateException("DbusMessage has bean not built, please call DbusMessageBuilder.build() method ahead");
        }
    }

    //testing
    public static void main(String[] args) {
        List<Object> rowDataValues = new ArrayList<>();

        MessageBuilder builder = new MessageBuilder();
        builder.build(Message.ProtocolType.DATA_INCREMENT_DATA, "dstype.ds1.schema1.table1.5.0.0", 3);
        builder.appendSchema("col1", DataType.INT, false);
        rowDataValues.clear();
        rowDataValues.add(50000012354L);
        rowDataValues.add("2018-01-01 10:13:24.2134");
        rowDataValues.add("i");
        rowDataValues.add("12345"); //uid
        rowDataValues.add(13);
        builder.appendPayload(rowDataValues.toArray());
        System.out.println(builder.message.toString());

        builder = new MessageBuilder(MessgeConstants.VERSION_12);
        builder.build(Message.ProtocolType.DATA_INCREMENT_DATA, "dstype.ds1.schema1.table1.5.0.0", 3);
        builder.appendSchema("col1", DataType.INT, false);
        rowDataValues.clear();
        rowDataValues.add(50000012354L);
        rowDataValues.add("2018-01-01 10:13:24.2134");
        rowDataValues.add("i");
        rowDataValues.add(13);
        builder.appendPayload(rowDataValues.toArray());
        System.out.println(builder.message.toString());

        builder = new MessageBuilder(MessgeConstants.VERSION_13);
        builder.build(Message.ProtocolType.DATA_INCREMENT_DATA, "dstype.ds1.schema1.table1.5.0.0", 3);
        builder.appendSchema("col1", DataType.INT, false);
        rowDataValues.clear();
        rowDataValues.add(50000012354L);
        rowDataValues.add("2018-01-01 10:13:24.2134");
        rowDataValues.add("i");
        rowDataValues.add("12345");
        rowDataValues.add(13);
        builder.appendPayload(rowDataValues.toArray());
        System.out.println(builder.message.toString());
    }
}
