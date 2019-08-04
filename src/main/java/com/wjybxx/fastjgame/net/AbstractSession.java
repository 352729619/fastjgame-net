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
import com.wjybxx.fastjgame.manager.SessionManager;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/4
 * github - https://github.com/hl845740757
 */
public abstract class AbstractSession implements Session{

    private static final Logger logger = LoggerFactory.getLogger(AbstractSession.class);

    protected abstract NetConfigManager getNetConfigManager();
    protected abstract SessionManager getSessionManager();

    @Override
    public final long localGuid() {
        return netContext().localGuid();
    }

    @Override
    public final RoleType localRole() {
        return netContext().localRole();
    }

    @Override
    public final void sendMessage(Object message) {
        // 逻辑层检测，会话已关闭，立即返回
        if (!isActive()) {
            logger.info("session is already closed, send message failed.");
            return;
        }
        netContext().netEventLoop().execute(() -> {
            getSessionManager().send(localGuid(), remoteGuid(), message);
        });
    }

    @Override
    public final RpcFuture rpc(@Nonnull Object request) {
        return rpc(request, getNetConfigManager().rpcCallbackTimeoutMs());
    }

    @Override
    public final RpcFuture rpc(@Nonnull Object request, long timeoutMs) {
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return netContext().netEventLoop().newCompletedFuture(netContext().localEventLoop(), RpcResponse.SESSION_CLOSED);
        }
        // 提交执行
        final RpcPromise rpcPromise = netContext().netEventLoop().newRpcPromise(netContext().localEventLoop());
        netContext().netEventLoop().execute(() -> {
            getSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, false, rpcPromise);
        });
        // 返回给调用者
        return rpcPromise;
    }

    @Override
    public final RpcResponse syncRpc(@Nonnull Object request) {
        return syncRpc(request, getNetConfigManager().syncRpcTimeoutMs());
    }

    @Override
    public final RpcResponse syncRpc(@Nonnull Object request, long timeoutMs) {
        // 逻辑层校验，会话已关闭，立即返回结果
        if (!isActive()) {
            return RpcResponse.SESSION_CLOSED;
        }
        final Promise<RpcResponse> rpcResponsePromise = netContext().netEventLoop().newPromise();
        // 提交执行
        netContext().netEventLoop().execute(() -> {
            getSessionManager().rpc(localGuid(), remoteGuid(), request, timeoutMs, true, rpcResponsePromise);
        });
        // 限时等待
        rpcResponsePromise.awaitUninterruptibly(timeoutMs, TimeUnit.MILLISECONDS);
        // 不论是否真的执行完成了，我们尝试让它变成完成状态，如果它已经进入完成状态，则不会产生任何影响。 不要想着先检查后执行这样的逻辑。
        rpcResponsePromise.trySuccess(RpcResponse.TIMEOUT);
        // 一定有结果
        return rpcResponsePromise.tryGet();
    }

    /**
     * 返回rpc结果
     * @param sync 是否是同步rpc调用
     * @param requestGuid 请求id
     * @param rpcResponse 响应结果
     */
    final void sendRpcResponse(boolean sync, long requestGuid, @Nonnull RpcResponse rpcResponse) {
        netContext().netEventLoop().execute(() -> {
            getSessionManager().sendRpcResponse(localGuid(), remoteGuid(), sync, requestGuid, rpcResponse);
        });
    }
}
