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

import com.wjybxx.fastjgame.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;

import javax.annotation.Nonnull;

/**
 * RpcPromise基本实现，不论如何，执行结果都是成功，赋值结果必须是RpcResponse对象。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class DefaultRpcPromise extends DefaultPromise<RpcResponse> implements RpcPromise {

	/**
	 * 发起rpc调用的用户所在的EventLoop，rpc回调的默认执行环境。
	 */
	private final EventLoop userEventLoop;

	/**
	 * @param workerEventLoop 创建该promise的EventLoop，为了支持用户调用{@link #await()}系列方法，避免死锁问题。
	 * @param userEventLoop 发起rpc调用的用户所在的EventLoop
	 */
	public DefaultRpcPromise(NetEventLoop workerEventLoop, EventLoop userEventLoop) {
		super(workerEventLoop);
		this.userEventLoop = userEventLoop;
	}

	@Override
	public void setFailure(@Nonnull Throwable cause) {
		super.setSuccess(new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, cause));
	}

	@Override
	public boolean tryFailure(@Nonnull Throwable cause) {
		return super.trySuccess(new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, cause));
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (!isDone()){
			// 检查一次状态，减少异常生成(填充堆栈)
			return tryCompleted(RpcResponse.CANCELLED, true);
		} else {
			return false;
		}
	}

	@Override
	public void addCallback(RpcCallback rpcCallback) {
		addCallback(rpcCallback, userEventLoop);
	}

	@Override
	public void addCallback(RpcCallback rpcCallback, EventLoop eventLoop) {
		addListener(future -> {
			rpcCallback.onComplete(future.tryGet());
		}, eventLoop);
	}

}
