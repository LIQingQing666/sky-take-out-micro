# 苍穹外卖 - Spring Cloud 微服务版本

## 项目简介

这是一个基于苍穹外卖项目的微服务架构改造版本，使用Spring Cloud技术栈将原有的单体应用拆分为多个独立的服务模块。目前项目还在开发中，部分功能可能不完整或存在bug，请谨慎使用。

## 技术栈

- **框架**: Spring Boot 2.7.3, Spring Cloud
- **数据库访问**: MyBatis, MyBatis Spring Boot Starter
- **数据库连接池**: Druid
- **JSON处理**: FastJSON
- **工具库**: Lombok, Commons Lang
- **分页插件**: PageHelper
- **对象存储**: Aliyun OSS SDK
- **API文档**: Knife4j (Swagger)
- **切面编程**: AspectJ
- **JWT认证**: JJWT
- **XML绑定**: JAXB API
- **Excel处理**: Apache POI

## 项目结构

```
sky-take-out-SpringCloud/
├── sky-common/          # 公共模块，包含工具类、常量等
├── sky-pojo/            # 数据对象模块，包含实体类、DTO等
├── sky-gateway/         # 网关模块，负责路由和过滤
└── sky-servers/         # 服务模块
    ├── merchant-service/    # 商户服务
    ├── order-service/       # 订单服务
    ├── pay-service/         # 支付服务
    ├── rider-service/       # 骑手服务
    └── user-service/        # 用户服务
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis (可选，用于缓存)

### 构建项目

```bash
# 克隆项目
git clone https://github.com/your-username/sky-take-out-SpringCloud.git
cd sky-take-out-SpringCloud

# 编译项目
mvn clean compile
```

### 启动服务

1. 启动注册中心 (如Nacos或Eureka，需要额外配置)
2. 启动配置中心 (如Config Server)
3. 按顺序启动各个服务模块：
   - sky-gateway
   - merchant-service
   - order-service
   - pay-service
   - rider-service
   - user-service

```bash
# 示例启动命令 (具体命令可能因配置而异)
mvn spring-boot:run -pl sky-gateway
mvn spring-boot:run -pl sky-servers/merchant-service
# ... 其他服务
```

### 数据库配置

项目使用MySQL数据库，请在相应服务的配置文件中配置数据库连接信息。

## API文档

启动服务后，可通过Knife4j访问API文档：

- 网关地址: `http://localhost:端口/doc.html`

## 开发状态

⚠️ **注意**: 此项目目前处于开发阶段，功能可能不完整，代码可能存在bug。如需使用，请仔细测试。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 许可证

[MIT License](LICENSE)

## 致谢

感谢苍穹外卖项目的原作者提供的基础代码和思路。
