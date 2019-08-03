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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.net.RpcFuture;
import com.wjybxx.fastjgame.net.RpcPromise;
import com.wjybxx.fastjgame.net.RpcResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 单个网络循环。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface NetEventLoop extends NetEventLoopGroup, EventLoop {

	@Nullable
	@Override
	NetEventLoopGroup parent();

	@Nonnull
	@Override
	NetEventLoop next();

	/**
	 * 创建一个RpcPromise
	 * @param userEventLoop 用户所在的EventLoop
	 * @return promise
	 */
	@Nonnull
	RpcPromise newRpcPromise(@Nonnull EventLoop userEventLoop);

	/**
	 * 创建rpcFuture，它关联的rpc操作早已完成。在它上面的监听会立即执行。
	 *
	 * @param userEventLoop 用户所在的EventLoop
	 * @param rpcResponse rpc调用结果
	 * @return rpcFuture
	 */
	@Nonnull
	RpcFuture newCompletedFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse);
}
