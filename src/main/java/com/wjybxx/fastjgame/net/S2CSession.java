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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务器存储的与客户端建立的会话信息。
 * 只暴露一部分关键信息。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:18
 * github - https://github.com/hl845740757
 */
public class S2CSession implements IS2CSession {

    private static final Logger logger = LoggerFactory.getLogger(S2CSession.class);

    private final NetContext netContext;
    private final NetManagerWrapper netManagerWrapper;

    private final HostAndPort localAddress;
    /**
     * 客户端唯一id，也就是sessionId
     */
    private final long clientGuid;
    /**
     * 客户端类型
     */
    private final RoleType clientType;
    /**
     * 会话在激活的时候才会创建，因此初始的时候是true
     */
    private final AtomicBoolean stateHolder = new AtomicBoolean(true);

    public S2CSession(NetContext netContext, HostAndPort localAddress, NetManagerWrapper netManagerWrapper,
                      long clientGuid, RoleType clientType) {
        this.netContext = netContext;
        this.netManagerWrapper = netManagerWrapper;
        this.localAddress = localAddress;
        this.clientGuid = clientGuid;
        this.clientType = clientType;
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
    public HostAndPort localAddress() {
        return localAddress;
    }

    @Override
    public long remoteGuid() {
        return clientGuid;
    }

    @Override
    public RoleType remoteRole() {
        return clientType;
    }

    public long getClientGuid() {
        return clientGuid;
    }

    public RoleType getClientType() {
        return clientType;
    }

    @Override
    public void sendMessage(Object message) {
        if (!isActive()) {
            logger.info("session is already closed, send message failed.");
            return;
        }
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getS2CSessionManager().send(localGuid(), remoteGuid(), message);
        });
    }

    @Override
    public RpcFuture rpc(Object request) {
        if (!isActive()) {
            return netContext.netEventLoop().newCompletedFuture(netContext.localEventLoop(), RpcResponse.SESSION_CLOSED);
        }
        return rpc(request, netManagerWrapper.getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Override
    public RpcFuture rpc(Object request, long timeoutMs) {
        // 提交执行
        final RpcPromise rpcPromise = netContext.netEventLoop().newRpcPromise(netContext.localEventLoop());
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getS2CSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, false, rpcPromise);
        });
        // 返回给调用者
        return rpcPromise;
    }

    @Override
    public RpcResponse syncRpc(Object request) {
        return this.syncRpc(request, netManagerWrapper.getNetConfigManager().syncRpcTimeoutMs());
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
            netManagerWrapper.getS2CSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, true, rpcResponsePromise);
        });
        // 限时等待
        rpcResponsePromise.awaitUninterruptibly(timeoutMs, TimeUnit.MILLISECONDS);
        // 不论是否真的执行完成了，我们尝试让它变成完成状态，如果它已经进入完成状态，则不会产生任何影响。 不要想着先检查后执行这样的逻辑。
        rpcResponsePromise.trySuccess(RpcResponse.TIMEOUT);
        // 一定有结果
        return rpcResponsePromise.tryGet();
    }

    void sendRpcResponse(boolean sync, long requestGuid, RpcResponse rpcResponse) {
        netContext.netEventLoop().execute(() -> {
            netManagerWrapper.getS2CSessionManager().sendRpcResponse(localGuid(), remoteGuid(), sync, requestGuid, rpcResponse);
        });
    }

    @Override
    public boolean isActive() {
        return stateHolder.get();
    }

    public void setClosed() {
        stateHolder.set(false);
    }

    @Override
    public void close() {
        if (stateHolder.compareAndSet(true, false)) {
            netContext.netEventLoop().execute(() -> {
                netManagerWrapper.getS2CSessionManager().removeSession(localGuid(), remoteGuid(), "close");
            });
        }
        // else 已关闭
    }

    @Override
    public String toString() {
        return "S2CSession{" +
                "localAddress=" + localAddress +
                ", clientGuid=" + clientGuid +
                ", clientType=" + clientType +
                '}';
    }
}
