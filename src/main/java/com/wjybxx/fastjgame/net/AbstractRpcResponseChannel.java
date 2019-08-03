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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * rpc返回写入结果的模板实现
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public abstract class AbstractRpcResponseChannel implements RpcResponseChannel{

	private final AtomicBoolean writable = new AtomicBoolean(true);

	/** 是否是同步调用 */
	private final boolean sync;
	/** 该结果对应的请求id */
	private final long requestGuid;

	protected AbstractRpcResponseChannel(boolean sync, long requestGuid) {
		this.sync = sync;
		this.requestGuid = requestGuid;
	}

	@Override
	public final void write(RpcResponse rpcResponse) {
		write0(sync, requestGuid, rpcResponse);
	}

	private void write0(boolean sync, long requestGuid, RpcResponse rpcResponse) {
		if (writable.compareAndSet(true, false)) {
			doWrite(sync, requestGuid, rpcResponse);
		} else {
			throw new IllegalStateException("ResponseChannel can't be reused!");
		}
	}

	protected abstract void doWrite(boolean sync, long requestGuid, RpcResponse rpcResponse);
}