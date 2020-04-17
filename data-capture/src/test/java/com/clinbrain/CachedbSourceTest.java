package com.clinbrain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.clinbrain.database.*;
import com.clinbrain.global.CacheGlobalConfig;
import com.clinbrain.global.ConfigIndex;
import com.clinbrain.source.cache.CDCGlobalLog;
import com.clinbrain.utils.ExcelUtils;
import com.intersys.classes.Persistent;
import com.intersys.objects.CacheDatabase;
import com.intersys.objects.Database;
import com.intersys.objects.Id;
import com.intersys.objects.reflect.CacheClass;
import com.intersys.objects.reflect.CacheField;
import com.intersys.objects.reflect.SQLColumn;
import com.intersys.objects.reflect.TableMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class CachedbSourceTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void test() throws Exception {
        Properties props = new Properties();
        props.put("url", "jdbc:Cache://localhost:1972/SAMPLES");
        props.put("username", "_SYSTEM");
        props.put("password", "sys");
        props.put("batch_size", "10000");
        props.put("offset_path", "D:/IDEAProject/CacheDBCapture/logs/offset");
        props.put("topics", "test");
        props.put("bootstrap.servers", "localhost:9092");

        String url="jdbc:Cache://localhost:1972/%SYS";
        String username="_SYSTEM";
        String password="sys";

        Context context = new Context(props);
//        CachedbRecord cachedbRecord = context.getCacheGlobalConfig().findByGlobal("^VendorData(2,1)", "SAMPLES");
//        List<String> one = cachedbRecord.buildRecord(context.getCacheDBMetadata().getAllDBTableMeta(),
//                CacheDatabase.getDatabase (url, username, password));
//        System.out.println(one);
    }

    @Test
    public void testConnection() {
        String url="jdbc:Cache://localhost:1972/%SYS";
        String username="_SYSTEM";
        String password="sys";
        try {
            String driverName = "com.intersys.jdbc.CacheDriver";
            Class.forName(driverName);
            Connection conn = DriverManager.getConnection(url, username, password);
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMetadata() throws Exception {
        IDatabaseClient client = new CachedbClient("jdbc:Cache://localhost:1972/SAMPLES", "_SYSTEM", "sys");
        CachedbMetadata cacheDBMetadata = new CachedbMetadata();
        cacheDBMetadata.init(client);
    }

    public static class TestStruct {
        private String id;
        private Timestamp ts;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Timestamp getTs() {
            return ts;
        }

        public void setTs(Timestamp ts) {
            this.ts = ts;
        }
    }

    @Test
    public void testMetadata2() throws Exception {
        IDatabaseClient client = new CachedbClient("jdbc:Cache://localhost:1972/SAMPLES", "_SYSTEM", "sys");
        List<TestStruct> testStructs = client.queryWithBean("select ID, LogTimeStamp as TS from CDC.GlobalLog where ID >= 0 and ID < 100 and LogTimeStamp >= '2019-10-17 16:24:00'",
                TestStruct.class, null);
        System.out.println(testStructs.get(0).ts.getTime());
        System.out.println(testStructs.size());

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(1572575124000L);
        System.out.println(String.format("%04d%02d%02dT%02d:%02d:%02d.%03d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), 1));

        c.setTimeInMillis(System.currentTimeMillis());
        System.out.println(String.format("%04d%02d%02dT%02d:%02d:%02d.%03d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND)));
    }

    @Test
    public void testMetadata1() throws Exception {
        String url="jdbc:Cache://localhost:1972/%SYS";
        String username="_SYSTEM";
        String password="sys";

        String str = "[{\"table\":\"Sample.Vendor\",\"item\":[\"^|\"SAMPLES\"|VendorData(1)\"]}]";
        JSONArray message = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("table", "Sample.Vendor");
        JSONArray globals = new JSONArray();
        globals.add("^|\"SAMPLES\"|VendorData(1)");
        globals.add("^|\"SAMPLES\"|VendorData(1,1)");
        obj.put("item", globals);
        message.add(obj);
        System.out.println(message.toJSONString());
        Database dbconnection = CacheDatabase.getDatabase (url, username, password);
//        String result = CacheDBSource.queryRecodrd(dbconnection, message.toJSONString());
//        System.out.println(result.replaceAll("\u0010\u0001", ""));


        //Sample.Person  Sample.Vendor
        CacheClass cls = dbconnection.getCacheClass("%SYS.Journal.File");
        CacheField[] declaredFields = cls.getDeclaredFields();
        Class javaClass = cls.getJavaClass();
        Method[] methods = javaClass.getMethods();
        Method method = javaClass.getMethod("IsValidJournal",
                Database.class, String.class);
        System.out.println(method.invoke(null, dbconnection, "D:\\InterSystems\\Cache\\mgr\\journal\\20191109.001"));
//        Method firstRecordGet = javaClass.getMethod()
        Persistent persistent = (Persistent) cls.openObject(new Id(2));


        TableMetadata sqlTableMetadata = cls.getSQLTableMetadata();
        for (int i = 1; i < sqlTableMetadata.getMaxColumnNumber(); i++) {
            SQLColumn column = sqlTableMetadata.getColumn(i);
            if (column.isHidden()) {
                System.out.println("hidden: " + i + " -> " + column.getFieldName());
                continue;
            }

            if (column.isRowIdColumn()) {
                System.out.println("row id: " + column.getName());
            }

         //   persistent.getField(column.getName());

            CacheField field = column.getField();
            if (null != field && field.isCalculated()) {
                System.out.println("calc: " + i + " -> " + column.getFieldName() + " : " + persistent.getField(column.getName()));
                continue;
            }

            System.out.println(column.getName() + " -> " + column.getField());
        }
    }

    @Test
    public void testMetadata3() throws Exception {
        String url="jdbc:Cache://localhost:1972/SAMPLES";
        String username="_SYSTEM";
        String password="sys";

        String str = "[{\"table\":\"Sample.Vendor\",\"item\":[\"^|\"SAMPLES\"|VendorData(1)\"]}]";
        JSONArray message = new JSONArray();
        JSONObject obj = new JSONObject();
        obj.put("table", "Sample.Vendor");
        JSONArray globals = new JSONArray();
        globals.add("^|\"SAMPLES\"|VendorData(1)");
        globals.add("^|\"SAMPLES\"|VendorData(1,1)");
        obj.put("item", globals);
        message.add(obj);
        System.out.println(message.toJSONString());
        Database dbconnection = CacheDatabase.getDatabase (url, username, password);
//        String result = CacheDBSource.queryRecodrd(dbconnection, message.toJSONString());
//        System.out.println(result.replaceAll("\u0010\u0001", ""));


        //Sample.Person  Sample.Vendor
        CacheClass cls = dbconnection.getCacheClass("Sample.Person");
        Persistent persistent = (Persistent) cls.openObject(new Id(301));


        TableMetadata sqlTableMetadata = cls.getSQLTableMetadata();
        for (int i = 1; i < sqlTableMetadata.getMaxColumnNumber(); i++) {
            SQLColumn column = sqlTableMetadata.getColumn(i);
            if (column.isHidden()) {
                System.out.println("hidden: " + i + " -> " + column.getFieldName());
                continue;
            }

            if (column.isRowIdColumn()) {
                System.out.println("row id: " + column.getName());
            }

            //   persistent.getField(column.getName());

            CacheField field = column.getField();
            if (null != field && field.isCalculated()) {
                System.out.println("calc: " + i + " -> " + column.getFieldName() + " : " + persistent.getField(column.getName()));
                continue;
            }

            System.out.println(column.getName() + " -> " + column.getField());
        }
    }

    @Test
    public void testJson() {
        CDCGlobalLog cdcGlobalLog = new CDCGlobalLog();
        cdcGlobalLog.setID("1");
        cdcGlobalLog.setDBName("1");
        cdcGlobalLog.setGlobalName("1");
        cdcGlobalLog.setRecordType("1");
        cdcGlobalLog.setTS("1");
        cdcGlobalLog.setVal("1");

        byte[] tmp = JSON.toJSONBytes(cdcGlobalLog);
        CDCGlobalLog t = JSON.parseObject(tmp, CDCGlobalLog.class);
        System.out.println(JSON.toJSONString(cdcGlobalLog));

        String str = "{\"b\":null,\"a\":123, \"c\": {\"2\": 2, \"1\": 1}}";
        JSONObject o = JSON.parseObject(str, Feature.OrderedField);
        for (Map.Entry<String, Object> entry : o.entrySet()) {
            System.out.println(entry.getKey());
            if (entry.getValue() == null) {
                System.out.println(entry.getValue());
            }
            System.out.println(entry.getValue());
        }
        JSONObject c = o.getJSONObject("c");
        for (Map.Entry<String, Object> entry : c.entrySet()) {
            System.out.println(entry.getKey());
            if (entry.getValue() == null) {
                System.out.println(entry.getValue());
            }
            System.out.println(entry.getValue());
        }
        c.put("0", 0);
        System.out.println(c.toJSONString());
        System.out.println(JSON.toJSONString(o, SerializerFeature.WriteMapNullValue));

    }

    @Test
    public void testBuild() throws Exception {
        String data = "^2451318^0032494640^3^144904077^EP0004286692^20191122^57409^^^513125195402102025^^方仕秀^方仕秀^2^19540210^^65^1^^1^48^661^20191122^58781^^014床^脑梗死,肺部感染,高血压^18383516217^^^^^^^^^^^^^^^^^^^^^0^20191122^59143^10627^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^5^^^^";
        String[] fields = data.split("\\^");
        System.out.println(fields.length);

        String str = "{\"dBName\":\":mirror:NEWMIRROR:DHC-LISDATA\",\"globalName\":\"dbo.RPVisitNumberD(605461)\",\"iD\":\"540372\",\"recordType\":\"S\",\"tS\":\"2019-11-22 16:25:33.492\",\"val\":\"^2451320^0032494640^3^144904077^EP0004286692^20191122^57409^^^513125195402102025^^方仕秀^方仕秀^2^19540210^^65^1^^1^48^661^20191122^58794^^014床^脑梗死,肺部感染,高血压^18383516217^^^^^^^^^^^^^^^^^^^^^0^20191122^59143^10627^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^5^^^^\"}";
        CDCGlobalLog cdcGlobalLog = JSON.parseObject(str, CDCGlobalLog.class);
        CacheGlobalConfig config = new CacheGlobalConfig();
        config.init("D:\\work\\global\\sumary.xlsx");
        String namespace = cdcGlobalLog.getDBName().substring(cdcGlobalLog.getDBName().lastIndexOf(":") + 1);
        CachedbRecord record = config.findByGlobal("^" + cdcGlobalLog.getGlobalName(), namespace, null);

        String meta = "rp_visitnumber(!){\"allColumns\":[{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"rowid\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"visitnumber\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"regno\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"admissiontypedr\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"admno\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"medicalrecordno\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"admdate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"admtime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"certtypedr\"},{\"comment\":\"\",\"dataLength\":\"50\",\"dataType\":\"varchar\",\"name\":\"certno\"},{\"comment\":\"\",\"dataLength\":\"50\",\"dataType\":\"varchar\",\"name\":\"idnumber\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"paymenttypedr\"},{\"comment\":\"\",\"dataLength\":\"40\",\"dataType\":\"varchar\",\"name\":\"surname\"},{\"comment\":\"\",\"dataLength\":\"40\",\"dataType\":\"varchar\",\"name\":\"givenname\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"speciesdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"birthdate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"birthtime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"age\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"ageunitdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"ethnicitydr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"hospitaldr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"locationdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"doctordr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"requestdate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"requesttime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"warddr\"},{\"comment\":\"\",\"dataLength\":\"20\",\"dataType\":\"varchar\",\"name\":\"bedno\"},{\"comment\":\"\",\"dataLength\":\"600\",\"dataType\":\"varchar\",\"name\":\"symptom\"},{\"comment\":\"\",\"dataLength\":\"50\",\"dataType\":\"varchar\",\"name\":\"mobileno\"},{\"comment\":\"\",\"dataLength\":\"50\",\"dataType\":\"varchar\",\"name\":\"phoneno\"},{\"comment\":\"\",\"dataLength\":\"50\",\"dataType\":\"varchar\",\"name\":\"email\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"double\",\"name\":\"height\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"double\",\"name\":\"weight\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"bloodpressure\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"clinicalconditionsdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"lmpdate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"bit\",\"name\":\"pregnant\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"double\",\"name\":\"pregnantweeks\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"abodr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"rhdr\"},{\"comment\":\"\",\"dataLength\":\"60\",\"dataType\":\"varchar\",\"name\":\"address\"},{\"comment\":\"\",\"dataLength\":\"10\",\"dataType\":\"varchar\",\"name\":\"postcode\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"bit\",\"name\":\"infectflag\"},{\"comment\":\"\",\"dataLength\":\"20\",\"dataType\":\"varchar\",\"name\":\"infectalert\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"bit\",\"name\":\"specialflag\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"pregnantnum\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"childbirthnum\"},{\"comment\":\"\",\"dataLength\":\"100\",\"dataType\":\"varchar\",\"name\":\"reqnotes\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"requestno\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"bit\",\"name\":\"urgent\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"collectdate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"collecttime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"collectuserdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"anatomicalsitedr\"},{\"comment\":\"\",\"dataLength\":\"40\",\"dataType\":\"varchar\",\"name\":\"collectpositon\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"specimendr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"containerdr\"},{\"comment\":\"\",\"dataLength\":\"100\",\"dataType\":\"varchar\",\"name\":\"collectnotes\"},{\"comment\":\"\",\"dataLength\":\"10\",\"dataType\":\"varchar\",\"name\":\"h24uvolume\"},{\"comment\":\"\",\"dataLength\":\"15\",\"dataType\":\"varchar\",\"name\":\"h24utimeperiod\"},{\"comment\":\"\",\"dataLength\":\"10\",\"dataType\":\"varchar\",\"name\":\"bodytemp\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"bit\",\"name\":\"confidential\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"carrydate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"carrytime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"carryuserdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"receivedate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"receivetime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"receiveuserdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"specimenqualitydr\"},{\"comment\":\"\",\"dataLength\":\"100\",\"dataType\":\"varchar\",\"name\":\"receivenotes\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"instoragedate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"instoragetime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"instorageuserdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"outstoragedate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"outstoragetime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"outstorageuserdr\"},{\"comment\":\"\",\"dataLength\":\"1\",\"dataType\":\"varchar\",\"name\":\"outstoragetype\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"receivehospitaldr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"adddate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"addtime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"adduserdr\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"numeric\",\"name\":\"feesum\"},{\"comment\":\"\",\"dataLength\":\"1\",\"dataType\":\"varchar\",\"name\":\"addtype\"},{\"comment\":\"\",\"dataLength\":\"20\",\"dataType\":\"varchar\",\"name\":\"agedesc\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"arrivedate\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"arrivetime\"},{\"comment\":\"\",\"dataLength\":null,\"dataType\":\"integer\",\"name\":\"arriveuserdr\"}],\"database\":\"dbo\",\"generatedColumns\":[],\"pkColumns\":[{\"$ref\":\"$.allColumns[0]\"}],\"tableName\":\"rp_visitnumber\"}";
        Map<String, TableMeta> allDBTableMeta = new HashMap<>();
        int index = meta.indexOf("(!)");
        String key = meta.substring(0, index);
        String json = meta.substring(index + 3);
        allDBTableMeta.put(key, JSON.parseObject(json, TableMeta.class));

        List<String> columns = record.buildRecord(allDBTableMeta, null, null, data, null);
        System.out.println(columns);
    }

    @Test
    public void testCacheMetaSQLUserMap() throws Exception {
        int[] range = new int[ConfigIndex.MAX.ordinal() - 1];
        for (int i = 0; i < range.length; i++) {
            range[i] = i;
        }

        ArrayList<ArrayList<String>> rows = ExcelUtils.excelReader("D:\\work\\global\\sumary.xlsx", range);
        Set<String> globalTables = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            globalTables.add(rows.get(i).get(0) + "#" + rows.get(i).get(1) + "#" + rows.get(i).get(2));
        }

        List<String[]> gTables = new LinkedList<>();
        for (String t : globalTables) {
            gTables.add(t.split("#"));
        }

        List<String> tables = new LinkedList<>();
        BufferedReader input = null;
        try {
            String line = null;
            input = new BufferedReader(new FileReader(new File("D:\\work\\table\\DHC-APP.txt")));
            while (null != (line = input.readLine())) {
                String value = line.trim();
                if (!value.isEmpty()) {
                    String tableName = value.substring(0, value.indexOf(CachedbMetadataExport.META_SEPARATOR));
                    if (tableName.startsWith("sqluser.")) {
                        tables.add(tableName.split("\\.")[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream("D:\\work\\table\\map.txt", false));
            for (String table : tables) {
                boolean isFind = false;
                boolean exist = false;
                for (String[] t : gTables) {
                    if (table.equalsIgnoreCase(t[2])) {
                        if (t[1].startsWith("User")) {
                            output.write((t[0] + "," + t[1] + "," + t[2] + "\n").getBytes());
                        }

                        isFind = true;
                    }

                    if (("sqluser." + table).equalsIgnoreCase(t[2])) {
                        exist = true;
//                        System.out.println(t[0] + "," + t[1] + "," + t[2]);
                    }
                }

                if (!isFind) {
                    if (!exist) {
                        System.out.println(table);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    @Test
    public void testCode() {
        List<String> t = new ArrayList<>();
        t.add("2");
        t.add("12");
        t.add("1");

        t.sort(Comparator.comparingInt(Integer::parseInt));

        for (String tmp : t) {
            System.out.println(tmp);
        }
    }


    @After
    public void tearDown() throws Exception {
    }
}