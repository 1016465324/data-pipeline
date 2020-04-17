package com.clinbrain.utils;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * excel文件读取工具类,支持xls,xlsx两种格式
 * @author Andrew
 *
 */
public class ExcelUtils {

    /**
     * excel文件读取指定列的数据
     * @author Andrew
     * @param excelPath        文件名
     * @param args            需要查询的列号
     * @return    ArrayList<ArrayList<String>>    二维字符串数组
     * @throws IOException
     */
    @SuppressWarnings({ "unused" })
    public static ArrayList<ArrayList<String>> excelReader(String excelPath,int ... args) throws IOException {
        // 创建excel工作簿对象
        Workbook workbook = null;
        FormulaEvaluator formulaEvaluator = null;
        // 读取目标文件
        File excelFile = new File(excelPath);
        InputStream is = new FileInputStream(excelFile);
//        InputStream is = ExcelUtils.class.getClassLoader().getResourceAsStream("HIS8.2DHC-APP.xlsx");
        // 判断文件是xlsx还是xls
        if (excelFile.getName().endsWith("xlsx")) {
            workbook = new XSSFWorkbook(is);
            formulaEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) workbook);
        }else {
            workbook = new HSSFWorkbook(is);
            formulaEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) workbook);
        }

        //判断excel文件打开是否正确
        if(workbook == null){
            System.err.println("未读取到内容,请检查路径！");
            return null;
        }
        //创建二维数组,储存excel行列数据
        ArrayList<ArrayList<String>> als = new ArrayList<ArrayList<String>>();
        //遍历工作簿中的sheet
        for (int numSheet = 0; numSheet < workbook.getNumberOfSheets(); numSheet++) {
            Sheet sheet = workbook.getSheetAt(numSheet);
            //当前sheet页面为空,继续遍历
            if (sheet == null) {
                continue;
            }
            // 对于每个sheet，读取其中的每一行
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                ArrayList<String> al = new ArrayList<String>();
                // 遍历每一行的每一列
                for(int columnNum = 0 ; columnNum < args.length ; columnNum++){
                    Cell cell = row.getCell(args[columnNum]);
                    al.add(getValue(cell, formulaEvaluator));
                }
                als.add(al);
            }
        }
        is.close();
        return als;
    }

    /**
     * excel文件读取全部信息
     * @author Andrew
     * @param excelPath        文件名
     * @return    ArrayList<ArrayList<String>>    二维字符串数组
     * @throws IOException
     */
    @SuppressWarnings({ "unused" })
    public static ArrayList<ArrayList<String>> excelReader(String excelPath) throws IOException {
        // 创建excel工作簿对象
        Workbook workbook = null;
        FormulaEvaluator formulaEvaluator = null;
        // 读取目标文件
        File excelFile = new File(excelPath);
        InputStream is = new FileInputStream(excelFile);
        // 判断文件是xlsx还是xls
        if (excelFile.getName().endsWith("xlsx")) {
            workbook = new XSSFWorkbook(is);
            formulaEvaluator = new XSSFFormulaEvaluator((XSSFWorkbook) workbook);
        }else {
            workbook = new HSSFWorkbook(is);
            formulaEvaluator = new HSSFFormulaEvaluator((HSSFWorkbook) workbook);
        }

        //判断excel文件打开是否正确
        if(workbook == null){
            System.err.println("未读取到内容,请检查路径！");
            return null;
        }
        //创建二维数组,储存excel行列数据
        ArrayList<ArrayList<String>> als = new ArrayList<ArrayList<String>>();
        //遍历工作簿中的sheet
        for (int numSheet = 0; numSheet < workbook.getNumberOfSheets(); numSheet++) {
            Sheet sheet = workbook.getSheetAt(numSheet);
            //当前sheet页面为空,继续遍历
            if (sheet == null) {
                continue;
            }
            // 对于每个sheet，读取其中的每一行
            for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }
                // 遍历每一行的每一列
                ArrayList<String> al = new ArrayList<String>();
                for(int columnNum = 0 ; columnNum < row.getLastCellNum(); columnNum++){
                    Cell cell = row.getCell(columnNum);
                    al.add(getValue(cell, formulaEvaluator));
                }
                als.add(al);
            }
        }
        is.close();
        return als;
    }

    /**
     *
     * @param cell
     * @param formulaEvaluator
     * @return
     */
    @SuppressWarnings("deprecation")
    private static String getValue(Cell cell, FormulaEvaluator formulaEvaluator) {
        if(cell==null){
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getRichStringCellValue().getString();
            case NUMERIC:
                // 判断是日期时间类型还是数值类型
                if (DateUtil.isCellDateFormatted(cell)) {
                    short format = cell.getCellStyle().getDataFormat();
                    SimpleDateFormat sdf = null;
                    /* 所有日期格式都可以通过getDataFormat()值来判断
                     *     yyyy-MM-dd----- 14
                     *    yyyy年m月d日----- 31
                     *    yyyy年m月--------57
                     *    m月d日  --------- 58
                     *    HH:mm---------- 20
                     *    h时mm分  --------- 32
                     */
                    if(format == 14 || format == 31 || format == 57 || format == 58){
                        //日期
                        sdf = new SimpleDateFormat("yyyy-MM-dd");
                    }else if (format == 20 || format == 32) {
                        //时间
                        sdf = new SimpleDateFormat("HH:mm");
                    }
                    return sdf.format(cell.getDateCellValue());
                } else {
                    // 对整数进行判断处理
                    double cur = cell.getNumericCellValue();
                    long longVal = Math.round(cur);
                    Object inputValue = null;
                    if(Double.parseDouble(longVal + ".0") == cur) {
                        inputValue = longVal;
                    }
                    else {
                        inputValue = cur;
                    }
                    return String.valueOf(inputValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                //对公式进行处理,返回公式计算后的值,使用cell.getCellFormula()只会返回公式
                return String.valueOf(formulaEvaluator.evaluate(cell).getNumberValue());
            //Cell.CELL_TYPE_BLANK || Cell.CELL_TYPE_ERROR
            default:
                return null;
        }
    }
}