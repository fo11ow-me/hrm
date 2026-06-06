package com.qiujie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 配置Api生成文档
 *
 * @Author : qiujie
 * @Date : 2022/1/30
 */
@Configuration // 标明配置类
@EnableSwagger2 // 开启Swagger功能
public class SwaggerConfig {

    @Bean
    public Docket createRestApi(Environment environment) {
        // 设置要显示的swagger环境
//        Profiles profiles = Profiles.of("dev");
        // 判断是否处在自己设定的环境当中
//        boolean flag = environment.acceptsProfiles(profiles);
        return new Docket(DocumentationType.SWAGGER_2)  // DocumentationType.SWAGGER_2 固定的，代表swagger2
//                .groupName("分布式任务系统") // 如果配置多个文档的时候，那么需要配置groupName来分组标识
                .apiInfo(apiInfo()) // 用于生成API信息
                .enable(true) // 如果为false,则不启动swagger
                .select() // select()函数返回一个ApiSelectorBuilder实例,用来控制接口被swagger做成文档
                .apis(RequestHandlerSelectors.basePackage("com.qiujie.controller")) // 用于指定扫描哪个包下的接口
                /**
                 * RequestHandlerSelectors: 配置要扫描的包
                 * any(): 扫描全部
                 * none(): 不扫描
                 */
                .paths(PathSelectors.any()) // 选择所有的API,如果你想只为部分API生成文档，可以配置这里
                .build();
    }

    /**
     * 用于定义API主界面的信息，比如可以声明所有的API的总标题、描述、版本
     *
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("人力资源管理系统API") //  可以用来自定义API的主标题
                .description("人力资源管理系统API管理") // 可以用来描述整体的API
                .termsOfServiceUrl("") // 用于定义服务的域名
                .version("1.0") // 可以用来定义版本。
                .build(); //
    }


}
