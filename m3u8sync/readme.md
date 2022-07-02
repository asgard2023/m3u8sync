#m3u8文件同步服务
一台m3u8服务器开nginx下载服务，支持nginx开启gzip以压缩数据
另一台通过读下m3u8文件下载Nginx下对应的切片的方式同步数据

##显示文件信息
http://localhost:8080/m3u8/m3u8Info?roomId=1025050251

##启动命令
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev  
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

##常用接口
###增加新的下载通知
curl "http://localhost:8080/downup/add?roomId=xxx"
###异常修复，之前有失败才能使用
curl "http://localhost:8080/downup/one?roomId=xxxx"
###手动批量异常恢复上传，每小时整点会自动执行这个
curl "http://localhost:8080/downup/recover"
###上传进度查询
curl "http://localhost:8080/downup/status?type=help"