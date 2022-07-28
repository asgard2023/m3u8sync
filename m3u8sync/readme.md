# m3u8文件同步服务
一台m3u8服务器开nginx下载服务，支持nginx开启gzip以压缩数据
另一台通过读下m3u8文件下载Nginx下对应的切片的方式同步数据
需要redis用于保存执行状态
支持多机多服务并行执行
## 单体模式
<img src="https://opendfl-1259373829.cos.ap-guangzhou.myqcloud.com/doc/m3u8sync/single.jpg" width="80%" syt height="80%" />

## 中继模式
<img src="https://opendfl-1259373829.cos.ap-guangzhou.myqcloud.com/doc/m3u8sync/relay.jpg" width="80%" syt height="80%" />


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
* 回调通知会在异常失败后再通知3次，分别是5秒，10秒，15秒，如果仍然不成功才视为失败
* 直到下载完成，并进行回调通知，才从下载队列删除。
* 支持nginx开启文件列表时，按列表递归同步所有子目录以及所有件。
* 支持多节点中继模式，即可先同步到中间服务器，中间服务器再传到最终服务器。
* 支持arm服务器(比如树莓派4b)，内存要求小，64内存M也能跑起来

## 启动命令
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev  
java -jar .\m3u8sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

## 常用接口
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