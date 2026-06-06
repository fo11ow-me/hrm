package com.qiujie.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileTaskErrorExportRow {

    @ExcelProperty("行号")
    private Integer rowNum;

    @ExcelProperty("原始数据")
    private String rawData;

    @ExcelProperty("错误原因")
    private String errorMessage;
}
