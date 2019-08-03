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

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.NetContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端到服务器的会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:00
 * github - https://github.com/hl845740757
 */
public class C2SSession implements IC2SSession {

    private static final Logger logger = LoggerFactory.getLogger(C2SSession.class);

    /** 未激活状态 */
    private static final int ST_INACTIVE = 0;
    /** 激活状态 */
    private static final int ST_ACTIVE = 1;
    /** 已关闭 */
    private static final int ST_CLOSED = 2;

    private final NetContext netContext;
    private final NetManagerWrapper netManagerWrapper;

    /**
     * 服务器唯一标识(会话id)
     */
    private final long serverGuid;
    /**
     * 服务器类型
     */
    private final RoleType serverType;
    /**
     * 服务器地址
     */
    private final HostAndPort hostAndPort;
    /**
     * 会话是否已激活，客户端会话是预先创建的，因此刚创建的时候是未激活的，当且成功建立连接时，才会激活。
     */
    private final AtomicInteger stateHolder = new AtomicInteger(ST_INACTIVE);

    public C2SSession(NetContext netContext, NetManagerWrapper netManagerWrapper,
                      long serverGuid, RoleType serverType, HostAndPort hostAndPort) {
        this.netContext = netContext;
        this.netManagerWrapper = netManagerWrapper;
        this.serverGuid = serverGuid;
        this.serverType = serverType;
        this.hostAndPort = hostAndPort;
    }

    @Override
    public long localGuid() {
        return netContext.localGuid();
    }

    @Override
    public RoleType localRole() {
        return netContext.localRole();
    }

    @Override
    public long remoteGuid() {
        return serverGuid;
    }

    @Override
    public RoleType remoteRole() {
        return serverType;
    }

    @Override
    public HostAndPort remoteAddress() {
        return hostAndPort;
    }


    public long getServerGuid() {
        return serverGuid;
    }

    public RoleType getServerType() {
        return serverType;
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    /** 标记为已关闭 */
    public void setClosed() {
        stateHolder.set(ST_CLOSED);
    }

    /**
     * 尝试激活会话，由于可能与{@link #close()}存在竞争，这里可能失败
     * @return 如果成功设置为激活状态则返回true，否则表示已关闭(应该激活方法只会调用一次)
     */
    public boolean tryActive() {
        assert netContext.netEventLoop().inEventLoop();
        return stateHolder.compareAndSet(ST_INACTIVE, ST_ACTIVE);
    }

    @Override
    public String toString() {
        return "C2SSession{" +
                "serverGuid=" + serverGuid +
                ", serverType=" + serverType +
                ", hostAndPort=" + hostAndPort +
                '}';
    }


    // --------------------------------------------- 消息发送实现 ----------------------------------------
    // 以下并未严格处理已关闭的情况，因为即使它进入了下一步，未来也会失败。

    @Override
    public void sendMessage(Object message) {
        if (!isActive()) {
            logger.info("session is already closed, send message failed.");
            return;
        }
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getC2SSessionManager().send(localGuid(), remoteGuid(), message);
        });
    }

    @Override
    public RpcFuture rpc(Object request) {
        return rpc(request, netManagerWrapper.getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Override
    public RpcFuture rpc(Object request, long timeoutMs) {
        // 会话已关闭，立即返回结果，在上面的监听会立即返回。
        if (!isActive()) {
            return netContext.netEventLoop().newCompletedFuture(netContext.localEventLoop(), RpcResponse.SESSION_CLOSED);
        }
        // 提交执行
        final RpcPromise rpcPromise = netContext.netEventLoop().newRpcPromise(netContext.localEventLoop());
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getC2SSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, false, rpcPromise);
        });
        // 返回给调用者
        return rpcPromise;
    }

    @Override
    public RpcResponse syncRpc(Object request) {
        return syncRpc(request, netManagerWrapper.getNetConfigManager().syncRpcTimeoutMs());
    }

    @Override
    public RpcResponse syncRpc(Object request, long timeoutMs) {
        // 会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }

        final Promise<RpcResponse> rpcResponsePromise = netContext.netEventLoop().newPromise();
        // 提交执行
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getC2SSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, true, rpcResponsePromise);
        });
        // 限时等待
        rpcResponsePromise.awaitUninterruptibly(timeoutMs, TimeUnit.MILLISECONDS);
        // 不论是否真的执行完成了，我们尝试让它变成完成状态，如果它已经进入完成状态，则不会产生任何影响。 不要想着先检查后执行这样的逻辑。
        rpcResponsePromise.trySuccess(RpcResponse.TIMEOUT);
        // 一定有结果
        return rpcResponsePromise.tryGet();
    }

    void sendRpcResponse(boolean sync, long requestGuid, RpcResponse rpcResponse) {
        if (!isActive()) {
            logger.info("session is already closed, send rpcResponse failed.");
            return;
        }
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getC2SSessionManager().sendRpcResponse(localGuid(), remoteGuid(), sync, requestGuid, rpcResponse);
        });
    }

    @Override
    public boolean isActive() {
        return stateHolder.get() == ST_ACTIVE;
    }

    @Override
    public void close() {
        // 先切换状态
        if (stateHolder.compareAndSet(ST_INACTIVE, ST_CLOSED) || stateHolder.compareAndSet(ST_ACTIVE, ST_CLOSED)) {
            netContext.netEventLoop().execute(() -> {
                netManagerWrapper.getC2SSessionManager().removeSession(localGuid(), remoteGuid(), "close method");
            });
        }
        // else 早已经关闭
    }
}
