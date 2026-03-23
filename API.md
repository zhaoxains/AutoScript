# API 接口文档

本文档详细说明 AutoScript 后端提供的所有 API 接口。

---

## 目录

- [接口规范](#接口规范)
- [认证接口](#认证接口)
- [配置接口](#配置接口)
- [心跳接口](#心跳接口)
- [上报接口](#上报接口)
- [管理接口](#管理接口)
- [错误码说明](#错误码说明)

---

## 接口规范

### 基础信息

| 项目 | 说明 |
|------|------|
| 基础 URL | `https://your-domain.com/api` |
| 协议 | HTTPS |
| 数据格式 | JSON |
| 编码 | UTF-8 |

### 请求头

```
Content-Type: application/json
Authorization: Bearer {token}  // 需要认证的接口
```

### 响应格式

```json
{
    "code": 0,
    "msg": "success",
    "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，0 表示成功 |
| msg | string | 状态信息 |
| data | object | 响应数据 |

---

## 认证接口

### 1. 用户登录

**POST** `/auth/login`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（MD5 哈希后） |
| device_id | string | 是 | 设备 ID |

**请求示例：**

```json
{
    "username": "test",
    "password": "e10adc3949ba59abbe56e057f20f883e",
    "device_id": "device-001"
}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
        "user_info": {
            "user_id": 1,
            "username": "test",
            "role": 0,
            "expire_time": "2025-12-31 23:59:59"
        }
    }
}
```

### 2. 用户注册

**POST** `/auth/register`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| confirm_password | string | 是 | 确认密码 |
| device_id | string | 是 | 设备 ID |
| referrer | string | 否 | 邀请人用户名 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
        "user_info": {
            "user_id": 2,
            "username": "newuser",
            "role": 0
        }
    }
}
```

### 3. 用户登出

**POST** `/auth/logout`

**请求头：**

```
Authorization: Bearer {token}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

---

## 配置接口

### 1. 获取脚本列表

**GET** `/config/scripts`

**请求头：**

```
Authorization: Bearer {token}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "scripts": [
            {
                "script_id": 1,
                "script_name": "抖音自动观看",
                "script_desc": "自动观看抖音视频",
                "target_package": "com.ss.android.ugc.aweme",
                "version": "1.0.0",
                "status": 1,
                "priority": 2,
                "config": { ... }
            }
        ]
    }
}
```

### 2. 获取全局配置

**GET** `/config/global`

**请求头：**

```
Authorization: Bearer {token}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "heartbeat_interval": 30000,
        "log_upload_interval": 60000,
        "max_retry_count": 3
    }
}
```

---

## 心跳接口

### 1. 发送心跳

**POST** `/heartbeat`

**请求头：**

```
Authorization: Bearer {token}
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| device_id | string | 是 | 设备 ID |
| status | string | 否 | 当前状态 |
| current_script | string | 否 | 当前执行的脚本 |

**请求示例：**

```json
{
    "device_id": "device-001",
    "status": "idle",
    "current_script": null
}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "commands": [
            {
                "command_id": 1,
                "command": "execute_script",
                "params": {
                    "script_id": 1
                }
            }
        ]
    }
}
```

---

## 上报接口

### 1. 上报任务结果

**POST** `/report/task`

**请求头：**

```
Authorization: Bearer {token}
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| script_id | int | 是 | 脚本 ID |
| task_name | string | 是 | 任务名称 |
| start_time | long | 是 | 开始时间戳 |
| end_time | long | 否 | 结束时间戳 |
| status | int | 是 | 状态：0运行中/1成功/2失败/3中断 |
| error_message | string | 否 | 错误信息 |

**请求示例：**

```json
{
    "script_id": 1,
    "task_name": "抖音自动观看",
    "start_time": 1703001600000,
    "end_time": 1703001660000,
    "status": 1,
    "error_message": null
}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "record_id": 1
    }
}
```

### 2. 批量上报日志

**POST** `/report/logs`

**请求头：**

```
Authorization: Bearer {token}
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| logs | array | 是 | 日志数组 |

**日志对象：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| task_id | int | 是 | 任务记录 ID |
| log_type | string | 是 | 日志类型 |
| log_time | string | 是 | 日志时间 |
| log_content | string | 是 | 日志内容 |
| detail | string | 否 | 详细信息 |

**请求示例：**

```json
{
    "logs": [
        {
            "task_id": 1,
            "log_type": "OPERATION",
            "log_time": "2024-12-20 10:00:00",
            "log_content": "点击搜索按钮",
            "detail": "匹配类型: text\n匹配值: 搜索"
        }
    ]
}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "success_count": 1,
        "fail_count": 0
    }
}
```

### 3. 上传截屏

**POST** `/report/screenshot`

**请求头：**

```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| screenshot | file | 是 | 截屏图片文件 |
| device_id | string | 是 | 设备 ID |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "url": "https://your-domain.com/uploads/screenshots/xxx.png"
    }
}
```

---

## 管理接口

> 以下接口需要管理员权限

### 1. 用户管理

#### 1.1 获取用户列表

**GET** `/admin/users`

**请求头：**

```
Authorization: Bearer {token}
```

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| page_size | int | 否 | 每页数量，默认 20 |
| keyword | string | 否 | 搜索关键词 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 100,
        "page": 1,
        "page_size": 20,
        "users": [
            {
                "user_id": 1,
                "username": "test",
                "role": 0,
                "status": 1,
                "expire_time": "2025-12-31 23:59:59",
                "create_time": "2024-01-01 00:00:00"
            }
        ]
    }
}
```

#### 1.2 创建用户

**POST** `/admin/users`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |
| role | int | 否 | 角色，默认 0 |
| status | int | 否 | 状态，默认 1 |
| expire_time | string | 否 | 到期时间 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "user_id": 2
    }
}
```

#### 1.3 更新用户

**PUT** `/admin/users/{id}`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 用户名 |
| password | string | 否 | 密码 |
| role | int | 否 | 角色 |
| status | int | 否 | 状态 |
| expire_time | string | 否 | 到期时间 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

#### 1.4 删除用户

**DELETE** `/admin/users/{id}`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

### 2. 脚本管理

#### 2.1 获取脚本列表

**GET** `/admin/scripts`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "scripts": [
            {
                "script_id": 1,
                "script_name": "抖音自动观看",
                "script_desc": "自动观看抖音视频",
                "target_package": "com.ss.android.ugc.aweme",
                "version": "1.0.0",
                "status": 1,
                "priority": 2,
                "create_time": "2024-01-01 00:00:00"
            }
        ]
    }
}
```

#### 2.2 创建脚本

**POST** `/admin/scripts`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| script_name | string | 是 | 脚本名称 |
| script_desc | string | 否 | 脚本描述 |
| target_package | string | 是 | 目标包名 |
| version | string | 否 | 版本号 |
| status | int | 否 | 状态，默认 0 |
| priority | int | 否 | 优先级，默认 2 |
| config | object | 是 | 脚本配置 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "script_id": 1
    }
}
```

#### 2.3 更新脚本

**PUT** `/admin/scripts/{id}`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

#### 2.4 删除脚本

**DELETE** `/admin/scripts/{id}`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

### 3. 执行命令

#### 3.1 发送执行命令

**POST** `/admin/execute`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | 是 | 用户 ID |
| script_ids | array | 是 | 脚本 ID 数组 |

**请求示例：**

```json
{
    "user_id": 1,
    "script_ids": [1, 2, 3]
}
```

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "command_id": 1
    }
}
```

#### 3.2 执行单个脚本

**POST** `/admin/scripts/{id}/execute`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | 是 | 用户 ID |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": null
}
```

### 4. 执行记录

#### 4.1 获取记录列表

**GET** `/admin/records`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| page_size | int | 否 | 每页数量 |
| user_id | int | 否 | 用户 ID |
| script_id | int | 否 | 脚本 ID |
| status | int | 否 | 状态 |

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "total": 100,
        "records": [
            {
                "record_id": 1,
                "user_id": 1,
                "username": "test",
                "script_id": 1,
                "script_name": "抖音自动观看",
                "start_time": "2024-12-20 10:00:00",
                "end_time": "2024-12-20 10:10:00",
                "status": 1
            }
        ]
    }
}
```

#### 4.2 获取记录详情

**GET** `/admin/records/{id}`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "record_id": 1,
        "user_id": 1,
        "script_id": 1,
        "start_time": "2024-12-20 10:00:00",
        "end_time": "2024-12-20 10:10:00",
        "status": 1,
        "error_message": null,
        "logs": [
            {
                "log_type": "OPERATION",
                "log_time": "2024-12-20 10:00:00",
                "log_content": "点击搜索按钮",
                "detail": "..."
            }
        ]
    }
}
```

#### 4.3 导出记录

**GET** `/admin/records/export`

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| start_date | string | 否 | 开始日期 |
| end_date | string | 否 | 结束日期 |
| user_id | int | 否 | 用户 ID |

**响应：**

CSV 文件下载

### 5. 在线用户

#### 5.1 获取在线用户

**GET** `/admin/online-users`

**响应示例：**

```json
{
    "code": 0,
    "msg": "success",
    "data": {
        "users": [
            {
                "user_id": 1,
                "username": "test",
                "device_id": "device-001",
                "last_heartbeat": "2024-12-20 10:00:00",
                "status": "idle",
                "current_script": null
            }
        ]
    }
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001 | 参数错误 |
| 1002 | 认证失败 |
| 1003 | Token 过期 |
| 1004 | 权限不足 |
| 1005 | 用户不存在 |
| 1006 | 密码错误 |
| 1007 | 用户已存在 |
| 1008 | 用户已禁用 |
| 1009 | 用户已过期 |
| 2001 | 脚本不存在 |
| 2002 | 脚本未上线 |
| 3001 | 设备未绑定 |
| 5000 | 服务器错误 |

---

## 调用示例

### cURL 示例

```bash
# 登录
curl -X POST https://your-domain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"e10adc3949ba59abbe56e057f20f883e","device_id":"device-001"}'

# 获取脚本列表
curl -X GET https://your-domain.com/api/config/scripts \
  -H "Authorization: Bearer your_token_here"

# 发送心跳
curl -X POST https://your-domain.com/api/heartbeat \
  -H "Authorization: Bearer your_token_here" \
  -H "Content-Type: application/json" \
  -d '{"device_id":"device-001","status":"idle"}'
```

### JavaScript 示例

```javascript
// 登录
const login = async () => {
    const response = await fetch('https://your-domain.com/api/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            username: 'test',
            password: 'e10adc3949ba59abbe56e057f20f883e',
            device_id: 'device-001'
        })
    });
    const data = await response.json();
    return data;
};

// 获取脚本列表
const getScripts = async (token) => {
    const response = await fetch('https://your-domain.com/api/config/scripts', {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });
    const data = await response.json();
    return data;
};
```

### Kotlin 示例

```kotlin
// 使用 Retrofit
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<LoginResponse>>
    
    @GET("config/scripts")
    suspend fun getScripts(@Header("Authorization") token: String): Response<BaseResponse<ScriptsResponse>>
    
    @POST("heartbeat")
    suspend fun heartbeat(
        @Header("Authorization") token: String,
        @Body request: HeartbeatRequest
    ): Response<BaseResponse<HeartbeatResponse>>
}
```

---

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| 1.0.0 | 2024-12-20 | 初始版本 |
