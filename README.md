# Sky Take Out - 外卖系统后端

## 项目介绍

这是一个基于 Spring Boot 开发的外卖系统后端项目，是我第一次手敲的入门级 Java 项目。通过这个项目，我学习了 Spring Boot 框架的基本使用、数据库操作、缓存机制、RESTful API 设计等核心技术。

项目采用前后端分离架构，后端提供完整的 API 接口，支持用户端和管理员端的功能。用户可以通过移动端或网页端进行点餐、购物车管理、订单处理等操作；管理员可以通过后台管理系统进行菜品管理、订单管理、员工管理等。

## 技术栈

- **后端框架**: Spring Boot 2.7.3
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **ORM**: MyBatis + MyBatis-Plus
- **连接池**: Druid
- **工具库**: Lombok, FastJSON, Apache Commons Lang
- **文档**: Knife4j (Swagger)
- **其他**: JWT 认证, OSS 文件上传, POI 导出, 定时任务

## 项目结构

```
sky-take-out/
├── pom.xml                          # 根 POM 文件
├── sky-common/                      # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/sky/
│       ├── constant/                # 常量类
│       ├── context/                 # 上下文工具
│       ├── enumeration/             # 枚举类
│       ├── exception/               # 自定义异常
│       ├── json/                    # JSON 处理工具
│       ├── properties/              # 配置属性
│       ├── result/                  # 统一响应结果
│       └── utils/                   # 工具类
├── sky-pojo/                        # 数据对象模块
│   ├── pom.xml
│   └── src/main/java/com/sky/
│       ├── dto/                     # 数据传输对象
│       ├── entity/                  # 实体类
│       └── vo/                      # 视图对象
└── sky-server/                      # 服务模块
    ├── pom.xml
    └── src/main/java/com/sky/
        ├── controller/               # 控制器层
        │   ├── admin/               # 管理员接口
        │   └── user/                # 用户接口
        ├── service/                 # 服务层
        ├── mapper/                  # 数据访问层
        ├── config/                  # 配置类
        └── SkyApplication.java      # 启动类
```

## 主要功能

### 用户端功能
- 用户注册与登录
- 菜品浏览与搜索
- 购物车管理
- 订单创建与支付
- 地址簿管理
- 历史订单查询

### 管理员端功能
- 员工管理
- 分类管理
- 菜品管理（包含口味设置）
- 套餐管理
- 订单管理
- 营业数据统计
- 工作台数据展示

## 环境要求

- JDK 8+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

## 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/your-username/sky-take-out.git
cd sky-take-out
```

### 2. 数据库初始化
- 创建 MySQL 数据库 `sky_take_out`
- 执行项目中的 SQL 脚本（如果有的话）或根据实体类创建表结构

### 3. 配置环境变量
在 `sky-server/src/main/resources/application.yml` 中配置数据库和 Redis 连接信息：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: sky_take_out
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_password  # 如果有密码
```

### 4. 编译运行
```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run -pl sky-server
```

项目启动后，访问 `http://localhost:8080` 查看 API 文档（如果配置了 Knife4j）。

## API 文档

项目集成了 Knife4j (Swagger) 用于 API 文档生成。启动项目后访问：
- Swagger UI: `http://localhost:8080/doc.html`

## 学习收获

作为第一次手敲的 Spring Boot 项目，这个项目让我掌握了以下技能：

1. **Spring Boot 基础**
   - 自动配置原理
   - 依赖注入
   - 配置文件管理

2. **数据库操作**
   - MyBatis 映射器配置
   - 连接池优化（Druid）
   - 事务管理

3. **缓存机制**
   - Redis 集成
   - 缓存注解使用

4. **RESTful API 设计**
   - 统一响应格式
   - 异常处理
   - 参数校验

5. **安全认证**
   - JWT Token 认证
   - 拦截器使用

6. **文件上传**
   - 阿里云 OSS 集成

7. **数据导出**
   - POI 库使用

8. **定时任务**
   - Spring 定时任务配置

## 项目亮点

- **模块化设计**: 采用多模块 Maven 项目结构，便于维护和扩展
- **统一异常处理**: 全局异常处理器，提供友好的错误响应
- **性能优化**: 配置了数据库连接池和 Redis 连接池
- **代码规范**: 使用 Lombok 简化代码，遵循阿里巴巴 Java 开发规范
- **文档完善**: 集成 API 文档工具，便于前后端联调

## 后续计划

- [ ] 前端页面开发
- [ ] 微信小程序接入
- [ ] 支付功能集成
- [ ] 消息推送功能
- [ ] 性能监控和优化

## 许可证

MIT License

## 联系方式

如果您对这个项目有任何问题或建议，欢迎通过以下方式联系：

- Email: your-email@example.com
- GitHub: https://github.com/your-username

---

**特别说明**: 这是一个学习项目，代码可能存在不足之处，欢迎各位大佬指正和建议！