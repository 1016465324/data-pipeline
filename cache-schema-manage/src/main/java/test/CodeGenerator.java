package test;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CodeGenerator
 * @Description TODO
 * @Author p
 * @Date 2020/3/26 10:43
 * @Version 1.0
 **/
public class CodeGenerator {

    public static void main(String[] args) {
        List<String> war = new ArrayList<>();
        File file = new File("C:\\xhy\\workspace\\data-pipeline\\cache-schema-manage\\src\\main\\java\\test\\generatorConfig.xml");
        ConfigurationParser cp = new ConfigurationParser(war);
        try {
            Configuration config = cp.parseConfiguration(file);
            DefaultShellCallback back = new DefaultShellCallback(true);
            MyBatisGenerator my = new MyBatisGenerator(config, back, war);
            my.generate(null);
            for(String s : war) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
