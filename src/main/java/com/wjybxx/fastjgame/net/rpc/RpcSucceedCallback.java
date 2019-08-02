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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Rpc 成功获得返回时的回调,当Rpc调用失败时，仅仅是打印一条日志。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface RpcSucceedCallback extends RpcCallback{

    Logger logger = LoggerFactory.getLogger(RpcSucceedCallback.class);

    @Override
    default void onComplete(@Nonnull Call call, @Nonnull RpcResponse response) throws Exception {
        if (response.isSuccess()) {
            onResponse(call, response);
        } else {
            logger.warn("call remote {} failure, messageName {}, resultCode {}.",
                    call.remoteGuid(),
                    call.request().getClass().getSimpleName(),
                    response.getResultCode());
        }
    }

    /**
     * 当rpc调用成功时，该方法会被调用
     *
     * @param call rpc调用信息
     * @param response 返回结果
     * @throws Exception errors
     */
    void onResponse(@Nonnull Call call, @Nonnull RpcResponse response) throws Exception;
}
