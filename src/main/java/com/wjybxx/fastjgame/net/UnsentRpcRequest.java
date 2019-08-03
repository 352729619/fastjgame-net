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

import javax.annotation.concurrent.Immutable;

/**
 * 未发送的rpc请求体。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
@Immutable
public class UnsentRpcRequest implements UnsentMessage{

	private final long rpcRequestGuid;
	/** 是否是同步rpc调用，加急 */
	private final boolean sync;
	/** Rpc结构体，至于怎么解释它，不限制 */
	private final Object request;

	public UnsentRpcRequest(long rpcRequestGuid, boolean sync, Object request) {
		this.rpcRequestGuid = rpcRequestGuid;
		this.sync = sync;
		this.request = request;
	}

	public long getRpcRequestGuid() {
		return rpcRequestGuid;
	}

	public Object getRequest() {
		return request;
	}

	@Override
	public NetMessage build(long sequence) {
		return new RpcRequestMessage(sequence, sync, rpcRequestGuid, request);
	}
}
