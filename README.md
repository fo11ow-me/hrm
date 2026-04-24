### 项目演示

1. 地址：<a href="http://www.qiujie.net.cn" target="_blank" rel="noopener noreferrer">http://www.qiujie.net.cn</a>
2. 账号：admin
3. 密码：123 (其他账号的密码也为123)

### 日志

#### v1.0-221229

1. 解决了第一次点击导航栏，页面出现空白的问题
2. 取消使用自定义的封装组件CommonForm、CommonTable
3. 优化了一些对话框布局
4. 增加修改头像功能，员工未设置头像时，显示默认头像
5. 删除了att_leave_type、sal_deduct_type、att_work_time、att_overtime_type等数据库表，具体原因见项目文档的总结部分
6. token过期直接退出登录
7. 实现了当访问不存在的路由时，出现404页面的功能

#### v1.1-240323

1. 解决了一些关于路由warning的问题 2023/2/3
2. 优化了界面，增强了搜索功能 2023/2/15
3. 解决了添加子部门时，出现异常的问题 2023/2/17
4. 解决了不能修改密码的问题 2023/3/10
5. 解决了不能添加参保城市的问题 2023/4/6

#### v1.2-240402

1. 完成了项目的加班模块 2024/3/23
2. 解决了当数据表格的列fixed时，行未对齐的问题 2024/3/26

#### v2.0-240428

1. 整合了spring security，实现了基于jwt的认证以及授权 2024/4/3
2. 解决了文件上传、下载，数据导入、导出的跨域问题 2024/4/5
3. 实现了自定义的认证、授权失败处理器，认证失败直接退出登录 2024/4/7
4. 结合自定义指令，实现了功能点的权限控制 2024/4/11
5. 修复了一些小bug，完善了权限控制 2024/4/13
6. 整合了redis，实现了验证码登录功能 2024/4/18

#### master

1. 整合了activiti，实现了基于工作流的请假审批 2024/5/1
2. 指明技术栈版本 2025/8/17

### 项目介绍

#### 项目概述

本项目是一款基于Spring
Boot+Vue+ElementUI的人力资源管理系统，有权限管理、财务管理、系统管理、考勤管理等功能模块。

#### 技术栈

1. 前端

|     技术     |   版本    |
  |:----------:|:-------:|
|    Vue     | 2.6.14  |
|   Axios    | 0.25.0  |
| ElementUI  | 2.15.7  |
| Vue-Router |  3.5.3  |
|    Vuex    |  3.6.2  |
|  ECharts   |  5.3.0  |
|    node    | 22.14.0 |
|    npm     | 11.3.0  |

2. 后端

|       技术        |    版本    |
|:---------------:|:--------:|
|   Spring Boot   |  2.5.6   |
|       jdk       | 17.0.12  |
|       Jwt       |  0.11.5  |
|  MyBatis-Plus   |  3.5.1   |
|     Hutool      |  5.8.25  |
| Spring Security |  5.5.3   |
|    Activiti     | 7.0.0.GA |
|     Swagger     |  2.9.2   |
|      MySQL      |  8.1.0   |
|      Redis      | 5.0.14.1 |

### 系统设计

#### 功能模块

![功能模块.png](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711450961011.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

本系统主要分四个模块，分别是权限管理、财务管理、系统管理、考勤管理。

### 系统展示

#### 首页

![首页](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451220221.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

首页主要展示了员工以及系统的一些基本信息。

#### 请假

![请假](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451261849.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

在请假面板中，员工可以进行请假。在右侧的请假记录中可以查看以往的请假申请，或对当前未被审核的申请进行撤销。

![审批](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240428-1714314768552.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

对员工的请假进行审批，比如通过或驳回。员工申请请假之后，等待相关人员进行审批。如果审批通过，则将员工休假时间段的考勤状态设置为休假（但当请假类型是调休时，考勤状态要设为调休）。

#### 五险一金

![五险一金](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451370245.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

![明细](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451410947.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

实现了对员工的五险一金进行管理，以及员工五险一金报表的导出。

#### 薪资管理

![薪资管理](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451455146.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

![明细](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451550417.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

实现了对员工的薪资进行管理，以及员工月薪资报表的导出。

#### 考勤表现

![考勤状态](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451623975.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

实现了考勤数据的导入，以及月考勤报表的导出，员工考勤状态的查看与修改。

![考勤数据导入模板](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451829770.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

通过导入员工的考勤记录表，系统就会将员工的上下班时间与员工所在部门规定的上班时间进行比对，判断员工是否迟到、早退、旷工。
（这是数据导入的模板，需要按照此格式填写数据。系统只会读取上下班时间的时间部分，并以考勤日期作为打卡日期）

另外，介绍一下考勤规则：

* 若考勤日期对应的是周末，则不判读员工考勤状态，默认为休假。
* 若考勤日期对应的是员工请假的日期，则员工考勤状态设为休假。
* 若考勤日期对应的是员工调休的日期，则员工考勤状态设为调休。
* 若员工的四个打卡时间，只要有一个为空，则员工考勤状态设为旷工。
* 若员工既迟到又早退，则员工考勤状态设为旷工。
* 若员工迟到，则员工考勤状态设为迟到。
* 若员工早退，则员工考勤状态设为早退。
* 若不是以上任何情况，则员工考勤状态设为正常。

#### 加班详情

![加班详情](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451716398.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

实现了加班数据的导入，以及月加班报表的导出，员工加班状态的查看与加班信息的修改。

![加班数据导入模板](https://image-qiu.oss-cn-hangzhou.aliyuncs.com/project/hrm/README-20240326-1711451890484.png?x-oss-process=image/auto-orient,1/interlace,1/quality,q_50/format,jpg)

通过导入员工的加班记录表（导入数据时，数据项有缺失的加班记录会被直接丢弃），系统会首先计算员工当天的加班总时长，然后再根据员工所在部门的加班规则等信息，对员工的加班状态进行判断。

加班类型：

* 节假日加班：根据国家的节假日安排来判断当天是否是节假日。
* 休息日加班：将周末视为休息日；如果是休息日加班，部门可以选择不调休或者调休；当选择调休时，只有当员工当天的总加班时长不少于8个小时时，才可以获得一天的调休，否则没有调休。
* 工作日加班：工作日就是周一到周五。

加班费计算方式：

* 以小时为单位：员工当天的总加班时长不能少于2小时，否则没有加班费；加班费 = 时薪 x 时长 x 倍数 + 奖金
* 以日为单位：员工当天的总加班时长不能少于8个小时，否则没有加班费；加班费 = 日薪 x 倍数 + 奖金

加班状态：

* 若当天是休息日加班，部门选择了调休，并且员工当天的加班总时长不少于8个小时，则员工获得一天的调休，当天的加班状态设置为调休。
* 其他情况下，若员工当天的加班时长大于0，当天的加班状态设置为加班。
* 若不是以上任何情况，则员工加班状态设置为正常。

### 项目搭建

#### 项目配置

1. 前端

   在`.env`文件中

   ```xml
   # 修改端口号，此端口号与在application.yml中配置的端口号相同
   VUE_APP_PORT = 8888
   ```

2. 后端

   在`application.yml`中进行相关的配置

   ```xml
   # 这是后端项目的运行端口，可自行修改
   server:
     port: 8888
   
   # 配置多数据源，hrm是主数据库，保存系统业务信息；hrm_activiti是从数据库，保存工作流信息
   datasource:
    master:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/hrm?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
      type: com.zaxxer.hikari.HikariDataSource
    activiti:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/hrm_activiti?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
      type: com.zaxxer.hikari.HikariDataSource
   
   # redis配置
   redis:
    host: localhost
    port: 6379
    password: 123456
    database: 0
    lettuce:
      pool:
        max-idle: 16
        max-active: 32
        min-idle: 8
   
   # 因为此项目涉及到了文件上传与下载，此路径用来存储上传的文件。
   file-path: E:/project/idea/hrm/file/ # 修改为自己的路径
   
   # 因为每年的法定节假日是不确定的，这里我只列举了2024年的节假日，其他年份的可根据个人需要添加即可
   holidays:
     - 2024-01-01
     - 2024-02-10
     - 2024-02-11
     - 2024-02-12
     - 2024-02-13
     - 2024-02-14
     - 2024-02-15
     - 2024-02-16
     - 2024-02-17
     - 2024-04-04
     - 2024-04-05
     - 2024-04-06
     - 2024-05-01
     - 2024-05-02
     - 2024-05-03
     - 2024-05-04
     - 2024-05-05
     - 2024-06-08
     - 2024-06-09
     - 2024-06-10
     - 2024-09-15
     - 2024-09-16
     - 2024-09-17
     - 2024-10-01
     - 2024-10-02
     - 2024-10-03
     - 2024-10-04
     - 2024-10-05
     - 2024-10-06
     - 2024-10-07
   ```

#### 项目启动

1. 启动mysql，新建数据库，执行sql文件
2. 配置redis，启动redis
3. 启动后端项目/hrm/hrm
4. 在前端项目/hrm/vue-elementui-hrm目录下，执行如下命令

   ```bash
   # 下载依赖
   npm install 
   # 启动
   npm run serve
   ```

#### 登录

1. 地址：http://localhost:8080/login
2. 账号： admin
3. 密码： 123 (其他账号的密码也为123)
