# m3u8sync Sync m3u8 files by nginx 
* 需要把m3u8文件挂到nginx下，以提供对外下载（可以在防火墙配IP白名单）
* 可以开启nginx gzip压缩

##m3u8sync主服务
* 基于nginx下载的m3u8同步服务，可用于跨地域同步，网络差的情况
* 支持nginx的gzip
* 支持按单个m3u8进行同步
* 支持m3u8的所有ts全部下载完成后回调服务，回调失败可多次回调。

##m3u8sync-call-demo调用服务示例（支持回调）
调用示例demo