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

import com.wjybxx.fastjgame.concurrent.CompleteFuture;
import com.wjybxx.fastjgame.concurrent.EventLoop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 已完成的Rpc调用，在它上面的任何监听都将立即执行。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public final class CompletedRpcFuture extends CompleteFuture<RpcResponse> implements RpcFuture{

	/**
	 * rpc结果
	 */
	private final RpcResponse rpcResponse;

	/**
	 * @param userEventLoop 用户所在EventLoop,为什么可以只使用用户线程？因为不会阻塞。
	 * @param rpcResponse rpc结果
	 */
	public CompletedRpcFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse) {
		super(userEventLoop);
		this.rpcResponse = rpcResponse;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public RpcResponse tryGet() {
		return rpcResponse;
	}

	@Nullable
	@Override
	public Throwable cause() {
		return null;
	}

	@Override
	public void addCallback(RpcCallback rpcCallback) {
		addCallback(rpcCallback, executor());
	}

	@Override
	public void addCallback(RpcCallback rpcCallback, EventLoop eventLoop) {
		addListener(future -> {
			rpcCallback.onComplete(future.tryGet());
		}, eventLoop);
	}
}
