package com.qiujie.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.rules.DateType;

import java.util.Collections;

public class CodeGenerateUtil {
    public static void main(String[] args) {
        String dbUrl = requireEnvironment("CODEGEN_DB_URL");
        String dbUsername = requireEnvironment("CODEGEN_DB_USERNAME");
        String dbPassword = requireEnvironment("CODEGEN_DB_PASSWORD");
        String javaOutputDir = requireEnvironment("CODEGEN_JAVA_OUTPUT_DIR");
        String mapperOutputDir = requireEnvironment("CODEGEN_MAPPER_OUTPUT_DIR");

        DataSourceConfig dataSourceConfig = new DataSourceConfig
                .Builder(dbUrl, dbUsername, dbPassword)
                .typeConvert(new MySqlTypeConvert()) // 数据库字段类型转换
                .build();

        // 全局配置
        GlobalConfig globalConfig = new GlobalConfig.Builder()
                .fileOverride() // 覆盖已生成的文件
                .outputDir(javaOutputDir)
                .author("qiujie")
                .enableSwagger() // 便于生成Api文档
                .dateType(DateType.SQL_PACK) // 使用java.sql.Timestamp
                .commentDate("yyyy-MM-dd")
                .build();

        // 配置包名
        PackageConfig packageConfig = new PackageConfig.Builder()
                .parent("com.qiujie")
                .entity("entity")
                .service("service")
                .serviceImpl("service.impl")
                .mapper("mapper")
                .controller("controller")
                .pathInfo(Collections.singletonMap(OutputFile.mapperXml, mapperOutputDir))
                .build();

        StrategyConfig strategyConfig = new StrategyConfig.Builder()
                .addTablePrefix("sys_", "per_", "soc_", "sal_", "att_", "act_re_", "sal_") // 根据表名生成实体名，去除指定的表前缀
                .addInclude("per_permission")
                .entityBuilder() // 1. entity策略配置
                .enableLombok()
                .enableTableFieldAnnotation() // 生成字段注解
//                .logicDeleteColumnName("is_deleted") // 指明逻辑删除字段
//                .addTableFills(new Column("create_time", FieldFill.INSERT)) // 插入时自动填入时间
//                .addTableFills(new Property("updateTime", FieldFill.INSERT_UPDATE)) // 插入或更新时自动填入时间
                .idType(IdType.AUTO) // 主键自增
                .enableChainModel() // 链式
                .mapperBuilder() // 2. mapper策略配置
                .superClass(BaseMapper.class) // 设置父类
                .serviceBuilder() // 3. service策略配置
                .formatServiceFileName("%sService") // 如果不设置，则默认为I%sService
                .controllerBuilder() // 4. controller策略配置
                .enableRestStyle() //  开启@RestController
                .enableHyphenStyle() // 开启驼峰转连字符
                .build();

        new AutoGenerator(dataSourceConfig)
                .global(globalConfig)
                .packageInfo(packageConfig)
                .strategy(strategyConfig)
                .execute(); // 执行
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " environment variable is required");
        }
        return value;
    }
}
