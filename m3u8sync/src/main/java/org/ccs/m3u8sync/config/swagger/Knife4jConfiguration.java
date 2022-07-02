//package org.ccs.m3u8sync.config.swagger;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import springfox.documentation.builders.ApiInfoBuilder;
//import springfox.documentation.builders.PathSelectors;
//import springfox.documentation.builders.RequestHandlerSelectors;
//import springfox.documentation.service.Contact;
//import springfox.documentation.spi.DocumentationType;
//import springfox.documentation.spring.web.plugins.Docket;
//import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;
//
//@Configuration
//@EnableSwagger2WebMvc
//public class Knife4jConfiguration {
//
//    @Bean(value = "defaultApi2")
//    public Docket defaultApi2() {
//        Contact contact = new Contact("asgard","","asgard2023@outlook.com");
//        Docket docket=new Docket(DocumentationType.SWAGGER_2)
//                .apiInfo(new ApiInfoBuilder()
//                        //.title("swagger-bootstrap-ui-demo RESTful APIs")
//                        .description("# nginx file sync APIs")
//                        //.termsOfServiceUrl("http://127.0.0.1/")
//                        .contact(contact)
//                        .version("1.0")
//                        .build())
//                //分组名称
//                .groupName("2.X版本")
//                .select()
//                //这里指定Controller扫描包路径
//                .apis(RequestHandlerSelectors.basePackage("org.ccs.m3u8sync.controller"))
//                .paths(PathSelectors.any())
//                .build();
//        return docket;
//    }
//
//}
