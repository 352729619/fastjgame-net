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

import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;

/**
 * Rpc回调，无论调用成功与否，都会走到。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
public interface RpcCallback {

    /**
     * 当rpc完成时。
     * 注意必须调用{@link RpcResponse#isSuccess()}确认rpc调用是否成功，否则可能导致错误。
     *
     * @param call 调用信息
     * @param response 响应内容，主要是错误码，可能有body
     */
    void onComplete(@Nonnull Call call, @Nonnull RpcResponse response) throws Exception;
}
