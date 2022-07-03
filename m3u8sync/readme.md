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

##功能特性
* 基于nginx下载的m3u8同步服务，可用于跨地域同步，网络差的情况。
* 支持nginx的gzip，即下载时可走gzip模式，以降低流量。
* 支持按单个m3u8进行同步。
* 支持ts已下载只检查大小一致就不再重新下载，即m3u8下载一半出错，则下次已完成不会重新下载。
* 支持ts下载不全，则删掉重新下载。
* 支持m3u8的所有ts全部下载完成后回调服务，回调失败可多次回调。
* 下载未完成，失败次数超过5次的，不再下载，加入异常队列
* 异常队列每小时重新加入下载队列，以便于重新下载。
* 下载完成后进行回调通知，回调失败也当做异常，加入异常队列
* 直到下载完成，并进行回调通知，才从下载队列删除。