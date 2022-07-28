#中继模式
调用关系：外部->relay1->relay2->relay3->end->回调接口  
流程图  
<img src="https://opendfl-1259373829.cos.ap-guangzhou.myqcloud.com/doc/m3u8sync/relay2.jpg" width="80%" syt height="80%" />

每一个节点完成，传给下个节点继续
relay1，如果有多个服务，可通过共用redis形成集群
其他节点也类似，可以有各自自已的集群

#使用场景：
小节点，资源不够，又不好直接与主服务器连接，通过中续方式，增加中间场