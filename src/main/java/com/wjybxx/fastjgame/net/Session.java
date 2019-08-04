/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.misc.NetContext;

import javax.annotation.Nonnull;

/**
 * 会话信息，用于获取远程的基本信息，子类可提供更为全面的信息。
 *
 * <h3>消息顺序问题(极其重要)</h3>
 * 网络层提供以下保证：
 * 1. 单向消息与单向消息之间，异步rpc与异步rpc之间，单向消息与异步rpc之间都满足先发送的必然先到。
 * 他们本质上都是异步调用，也就是说异步调用与异步调用之间是有顺序保证。
 * 方法{@link #sendMessage(Object)} {@link #rpc(Object)} 。
 *
 * 2. 同步rpc和同步rpc之间，先发的必然先到。也就是 同步调用与同步调用之间有顺序保证。
 *  方法：{@link #syncRpc(Object)}
 *
 * Q: 为什么不没提供同步调用 与 异步调用 之间的顺序保证？
 * A: 基于这样的考虑：同步调用表示一种更迫切的需求，期望更快的处理，更快的返回，而异步调用没有这样的语义。
 *
 * Q: {@link RpcFuture#get()}{@link RpcFuture#await()} 阻塞到结果返回是同步调用吗？
 * A: 不是！！！ {@link #rpc(Object)}是异步调用！即使你可以阻塞到结果返回，它与{@link #syncRpc(Object)}在传输的时候有本质上的区别。
 *    所以，如果你如果不能理解{@link #syncRpc(Object)}的导致的时序问题，那么就不要用它。
 *
 * Q: {@link RpcFuture#get()}{@link RpcFuture#await()}有什么保证？
 * A: 当我从这两个方法返回时，能保证对方按顺序处理了我之前发送的所有消息和rpc请求。
 *
 * Q:为何抽象层没有提供address之类的信息？
 * A:因为底层会自动处理断线重连等等，这些信息可能会变化，暂时不提供。
 *
 * 注意：特定的 localGuid 和 remoteGuid 在同一个NetEventLoop下只能建立一个链接！！！它俩确定唯一的一个session。
 * 并不支持在不同的端口的上以相同的id再建立连接，只能存在于不同于的{@link NetEventLoop}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
public interface Session {

    /**
     * 该session所在的上下文
     */
    NetContext netContext();

    /**
     * 会话关联的本地对象guid
     */
    long localGuid();

    /**
     * 会话管理的本地角色类型
     */
    RoleType localRole();

    /**
     * 远程的guid
     */
    long remoteGuid();

    /**
     * 远程的角色类型
     */
    RoleType remoteRole();

    /**
     * 发送一个单向消息给对方
     * @param message 单向消息
     */
    void sendMessage(Object message);

    /**
     * 发送一个rpc请求给对方，会使用默认的超时时间（配置文件中指定）。
     * @param request rpc请求对象
     * @return rpc结果的future，用户线程可以在上面添加回到
     */
    RpcFuture rpc(@Nonnull Object request);

    /**
     * 发送一个rpc请求给对方
     * @param request rpc请求对象
     * @param timeoutMs 超时时间，毫秒。小于等于0表示不超时。
     * @return rpc结果的future，用户线程可以在上面添加回到
     */
    RpcFuture rpc(@Nonnull Object request, long timeoutMs);

    /**
     * 发送一个rpc请求给对方，并阻塞到返回结果或超时，会使用默认的超时时间（配置文件中指定）。
     * @param request rpc请求对象
     * @return rpc返回结果
     */
    RpcResponse syncRpc(@Nonnull Object request);

    /**
     * 发送一个rpc请求给对方，并阻塞到返回结果或超时。
     * @param request rpc请求对象
     * @param timeoutMs 超时时间，毫秒。小于等于0表示不超时。
     * @return rpc返回结果
     */
    RpcResponse syncRpc(@Nonnull Object request, long timeoutMs);

    /**
     * 当前仅当session已成功和对方建立连接，且未断开的情况下返回true。
     */
    boolean isActive();

    /**
     * 关闭当前session
     *
     * 注意：
     * 逻辑层的校验+网络层的校验并不能保证在session活跃的状态下才有事件！
     * 因为事件会被提交到session所在的executor，因此即使 {@link #isActive() false}，也仍然可能收到该session的消息或事件。
     * 逻辑层必须加以处理，因为网络层并不知道这时候逻辑层到底需不需要这些消息。
     */
    ListenableFuture<?> close();
}
