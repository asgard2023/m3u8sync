#m3u8sync for security
## 集成opendfl-mysql
* 增强安全,支持白名单，黑名单
* 支持url的频率限制
* 支持opendfl console
* 也可以把opendfl直接集成到m3u8sync，这样就可以支持@Requency频率限制
详情见：opoendfl开源

## 环境条件
* redis
* mysql
* nginx(从nginx下载文件)

# 启动命令
java -jar .\m3u8sync-security-1.0-SNAPSHOT.jar --spring.profiles.active=dev  
java -jar .\m3u8sync[security-1.0-SNAPSHOT.jar --spring.profiles.active=test

## 运行
http://localhost:9290/index.html  
默认账号：admin/admin

## 调用接口
### /downup/addAsync 添加m3u8下载任务
curl "http://localhost:9290/downup/addAsync?roomId=xxxx"
### /downup/addNginxList nginx开启文件列表显示功能，此接口读取目录列表中的所有m3u8进行处理
curl "http://localhost:9290/downup/addNginxList"
### /downup/retryTask 对于之前已存在的异常任务重试(立即执行，同步模式)
curl "http://localhost:9290/downup/one?roomId=xxxx"
### /downup/remove 用于移除失败的任务
curl "http://localhost:9290/downup/remove?roomId=xxxx"
### /downup/recover 手动批量异常恢复上传，重时时或者每小时整点会自动执行这个
curl "http://localhost:9290/downup/recover"
### /downup/status 上传进度查询
curl "http://localhost:9290/downup/status?type=help"

### /downErrorInfo/downErrorInfo 用于显示任务的异常信息信息
curl "http://localhost:9290/downErrorInfo/downErrorInfo?roomId=xxxxxxxx"
### /downErrorInfo/errorInfos 所有本机在用处理的任务或未完成的异常任务
curl "http://localhost:9290/downErrorInfo/errorInfos"

### /m3u8/m3u8Info 显示m3u8信息，抱括时长
curl "http://localhost:9290/m3u8/m3u8Info?roomId=xxxxxxxx"
### /m3u8/fileInfo 显示文件信息
curl "http://localhost:9290/m3u8/fileInfo?roomId=1025050251"

