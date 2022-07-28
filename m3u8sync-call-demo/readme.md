#m3u8sync call demo 调用示例demo
## 增加m3u8同步
http://localhost:9291/m3u8Sync/addAsync?roomId=xxxx&format={roomId}/{roomId}.m3u8  
参数说明：  
roomId 必填  
format 可选{roomId}/{roomId}.m3u8或者{roomId}/main.m3u8  
url 可选 ，如果url为空，则会根据format与m3u8sync的nginx-url生成url  
  例如：downup.nginx-url=http://175.178.252.112:81/m3u8/live/ format={roomId}/{roomId}.m3u8  
     则生成的下载url为：http://175.178.252.112:81/m3u8/live/{roomId}/{roomId}.m3u8
## m3u8信息查看
http://localhost:9291/m3u8Sync/m3u8Info?roomId=12344678
