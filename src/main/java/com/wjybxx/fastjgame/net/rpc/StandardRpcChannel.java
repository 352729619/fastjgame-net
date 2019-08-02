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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.manager.networld.RpcManager;
import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 标准的RpcChannel实现
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
public class StandardRpcChannel implements RpcChannel {

    /** 是否可写入结果，以原子变量保护只能写一次结果 */
    private final AtomicBoolean writable = new AtomicBoolean(true);

    /** 用于确定session */
    private final long logicWorldGuid;
    private final long remoteWorldGuid;
    /** 关联的call对应的请求 */
    private final long requestGuid;
    /** 用于返回rpc结果 */
    private final RpcManager rpcManager;

    public StandardRpcChannel(long logicWorldGuid, long remoteWorldGuid, long requestGuid, RpcManager rpcManager) {
        this.logicWorldGuid = logicWorldGuid;
        this.remoteWorldGuid = remoteWorldGuid;
        this.requestGuid = requestGuid;
        this.rpcManager = rpcManager;
    }

    @Override
    public void write(@Nonnull RpcResponse response) {
        if (writable.compareAndSet(true, false)) {
            rpcManager.senRpcResponse(logicWorldGuid, remoteWorldGuid, requestGuid, response);
        } else {
            throw new IllegalStateException();
        }
    }
}
