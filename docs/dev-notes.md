### 2026.4.19

* 使用localThread记录当前请求用户的信息
* 在WebMvcConfiguration中扩展SpringMVC的消息转换器，统一对日期类型进行格式处理
* 使用自定义注解来标识哪些修改公共字段的方法，例如create_time、update_time、create_by、update_by，然后采用aop拦截
使用了该注解方法，对方法的参数中的公共字段进行修改
* 可以添加上oos存储功能
