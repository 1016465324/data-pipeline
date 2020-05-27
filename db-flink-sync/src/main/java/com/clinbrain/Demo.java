package com.clinbrain;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @ClassName Demo
 * @Description TODO
 * @Author p
 * @Date 2020/3/18 16:22
 * @Version 1.0
 **/
public class Demo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();
        env.enableCheckpointing(5000);
        env.setParallelism(1);
        DataStream<String> text = env.readTextFile("D:\\IDEAProject\\FlinkTest\\src\\main\\resources\\source.txt");
        DataStream<Integer> parsed = text.map(new MapFunction<String, Integer>() {
            @Override
            public Integer map(String value) {
                return Integer.parseInt(value);
            }
        });

        parsed.print();
        env.execute("flink demo");
    }
}
