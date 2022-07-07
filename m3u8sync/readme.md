# m3u8文件同步服务
一台m3u8服务器开nginx下载服务，支持nginx开启gzip以压缩数据
另一台通过读下m3u8文件下载Nginx下对应的切片的方式同步数据

## 功能特性
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

## 启动命令
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev  
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

## 常用接口
### /downup/add增加新的下载通知
示例：http://localhost:9290/downup/add?roomId=xxx&format=xxx&m3u8Url=xxxx
请求类型：post
参数：
- roomId 必填，房间
- url 可选
- format 可选(为空时取yml配置的downup.format)，如果url为空，则会根据format与yml配置的dowup.nginx-url动态生成url
- callback(baseUrl,paramUrl) 可选，如果为空则用ymal的callback配置，用于下载完成后回调对应接口
  例如1：
  downup.nginx-url=http://175.178.252.112:81/m3u8/live/
  format={roomId}/{roomId}.m3u8  
  roomId=12344678
  则下载的m3u8Url=http://175.178.252.112:81/m3u8/live/12344678/12344678.m3u8
  例如2：
  downup.nginx-url=http://175.178.252.112:81/m3u8/live/
  format={roomId}/index.m3u8  
  roomId=wukong
  则下载的m3u8Url=http://175.178.252.112:81/m3u8/live/wukong/index.m3u8

下载完成后可回调地址：
- baseUrl+paramUrl.replace({roomId},roomId)
- 如果开启回调，则收到回调接口的返回ok，才算成功，否则视为下载异常，加入异常队列，以便于下次重试。


### /downup/one异常修复，之前有失败才能使用
curl "http://localhost:9290/downup/one?roomId=xxxx"
### /downup/recover手动批量异常恢复上传，每小时整点会自动执行这个
curl "http://localhost:9290/downup/recover"
- 无参数
- 会把下载失败次数超过5次的m3u8的异常下载任务从异常队列移回下载队列，以便于继续下载。
### /downup/status上传进度查询
curl "http://localhost:9290/downup/status?type=help"
### /m3u8/m3u8Info显示m3u8信息，抱括时长
curl "http://localhost:9290/m3u8/m3u8Info?roomId=1025050251"
### /m3u8/fileInfo显示文件信息
curl "http://localhost:9290/m3u8/fileInfo?roomId=1025050251"