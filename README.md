### fastjgame-net

fastjgame-net模块独立，一个高性能的可扩展底层通信框架。支持断线重连(防闪断)，负载均衡，使用简单。提供双向的消息发送，异步rpc调用，同步rpc调用。

### 本模块到底提没提供RPC?
   严格的说，本模块提供了对RPC的支持，但是如何解析RPC包的内容，并调用到指定方法，这个是用户自定义的！没有限定rpc的解析方式，用户可以通过**MessageHandler**实现自定义的单向消息和RPC的解析方式。
   在**fastjgame**项目中 [MessageDispatcherMrg](https://github.com/hl845740757/fastjgame/blob/master/game-core/src/main/java/com/wjybxx/fastjgame/mrg/MessageDispatcherMrg.java)是一个消息结构对应一个唯一的方法调用，但其本质还是rpc。


### 消息顺序问题(极其重要)
 网络层提供以下保证：
 1. 单向消息与单向消息之间，异步rpc与异步rpc之间，单向消息与异步rpc之间都满足先发送的必然先到。
 他们本质上都是异步调用，也就是说异步调用与异步调用之间是有顺序保证。
 方法 {@link #sendMessage(Object)} {@link #rpc(Object)} 。
 
 2. 同步rpc和同步rpc之间，先发的必然先到。也就是 同步调用与同步调用之间有顺序保证。
    方法：{@link #syncRpc(Object)}

### 常见问题 
 Q: 为什么不没提供同步调用 与 异步调用 之间的顺序保证？
 A: 基于这样的考虑：同步调用表示一种更迫切的需求，期望更快的处理，更快的返回，而异步调用没有这样的语义。
 
 Q: {@link RpcFuture#get()}{@link RpcFuture#await()} 阻塞到结果返回是同步调用吗？
 A: 不是！！！ {@link #rpc(Object)}是异步调用！即使你可以阻塞到结果返回，它与{@link #syncRpc(Object)}在传输的时候有本质上的区别。
      所以，如果你如果不能理解{@link #syncRpc(Object)}的导致的时序问题，那么就不要用它。
 
 Q: {@link RpcFuture#get()}{@link RpcFuture#await()}有什么保证？
 A: 当我从这两个方法返回时，能保证对方按顺序处理了我之前发送的所有消息和rpc请求。

### 注意死锁问题
   如果一个会话的双方都使用同步rpc调用，虽然有超时时间，但是也很危险，会大大增加超时失败的几率，尽量只有一方使用同步rpc调用，双向的异步rpc还是可以的。
   此外也尽量少调用RpcFuture上的get，await方法，建议使用回调，也会增加rpc超时的几率。

### 测试用例
   在example下有一个NetEventLoopExample，可以直接启动。 不过你可能需要先下载[game-utils模块](https://github.com/hl845740757/fastjgame-utils.git)，
   下载完成以后，install到本地maven仓库即可，然后就可以启动了。
### Bugs
   刚刚重构完，可能有一些遗漏的地方，会慢慢补全，如果发现bug，欢迎提出。
