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

import com.google.protobuf.Message;
import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Rpc通道，可以用它来返回rpc请求的结果。
 * 注意：它是一个一次性的channel，一旦调用{@link #write(RpcResponse)}方法，就不可以再次使用。
 *
 * @apiNote
 * 子类实现必须是线程安全的。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public interface RpcChannel {

    /**
     * 返回rpc调用结果。
     * 注意：该方法只可以调用一次
     * @param response rpc响应
     */
    void write(@Nonnull RpcResponse response);

}
