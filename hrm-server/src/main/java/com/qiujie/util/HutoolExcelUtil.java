package com.qiujie.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.qiujie.annotation.ExcelColumn;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 将数据写入到excel
 *
 * @Author qiujie
 * @Date 2022/3/7
 * @Version 1.0
 * @deprecated 请使用 {@link EasyExcelUtil} 替代，底层从 Hutool POI 切换为 EasyExcel 流式解析
 */
@Deprecated
public class HutoolExcelUtil {

    /**
     * 如果设置了别名，就只写入设置了别名的列；否则按字段名，默认全部输出
     *
     * @param response
     * @param list     导出的数据
     * @param filename 文件名称
     * @param clazz
     * @throws IOException
     */
    public static <T> void writeExcel(HttpServletResponse response, List<T> list, String filename, Class<T> clazz) throws IOException {
        Integer columnNum = 0; // 统计列数
        // 通过工具类创建对象，可以指定磁盘路径
        ExcelWriter writer = ExcelUtil.getWriter(true);
        // 判断是否序列化
        boolean isSerializable = false;
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 设置私有变量可访问
            field.setAccessible(true);
            if (field.getName().equals("serialVersionUID")) {
                isSerializable = true;
            }
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                // 增加标题别名
                writer.addHeaderAlias(field.getName(), excelColumn.value());
                columnNum++;
            }
        }
        // 如果未设置别名，就原样输出
        if (columnNum == 0) {
            if (isSerializable) {
                /**
                 * merge的第一个参数：表示从列索引0到将要合并的最后一列的索引，而并不是代表合并的列数
                 * 此处减2的原因，是因为我每个实体类中有一个serialVersionUID，不然标题行会多合并一列
                 */
                writer.merge(fields.length - 2, filename);
            } else {
                writer.merge(fields.length - 1, filename);
            }
        } else {
            // 默认的，未添加alias的属性也会写出，如果想只写出加了别名的字段，可以调用此方法排除之
            writer.setOnlyAlias(true);
            // 设置sheet标题
            writer.merge(columnNum - 1, filename);
        }
        // 一次性写出内容，使用默认样式，强制输出标题
        writer.write(list, true);
        // 设置浏览器响应的格式
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + ".xlsx");
        ServletOutputStream outputStream = response.getOutputStream();
        writer.flush(outputStream, true);
        // 关闭writer，释放内存
        writer.close();
        // 此处记得关闭输出Servlet流
        IoUtil.close(outputStream);
    }


    /**
     * 读取excel中的数据
     *
     * @param inputStream
     * @param headerRowIndex 表头行索引
     * @param clazz
     * @return
     */
    public static <T> List<T> readExcel(InputStream inputStream, Integer headerRowIndex, Class<T> clazz) {
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            // 设置私有变量可访问
            field.setAccessible(true);
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                reader.addHeaderAlias(excelColumn.value(), field.getName());
            }
        }
        // 从起始行读取数据
        List<T> list = reader.read(headerRowIndex, headerRowIndex + 1, Integer.MAX_VALUE, clazz);
        reader.close();
        return list;
    }

}
