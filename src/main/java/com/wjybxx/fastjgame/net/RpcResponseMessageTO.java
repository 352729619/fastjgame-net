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
 * Rpc响应结果
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/30
 * github - https://github.com/hl845740757
 */
@Immutable
@TransferObject
public class RpcResponseMessageTO extends MessageTO{

	/** 客户端的哪一个请求 */
	private final long requestGuid;
	/** rpc响应结果 */
	private final RpcResponse rpcResponse;

	public RpcResponseMessageTO(long ack, long sequence, long requestGuid, RpcResponse rpcResponse) {
		super(ack, sequence);
		this.requestGuid = requestGuid;
		this.rpcResponse = rpcResponse;
	}

	public long getRequestGuid() {
		return requestGuid;
	}

	public RpcResponse getRpcResponse() {
		return rpcResponse;
	}

	public RpcResultCode getResultCode() {
		return rpcResponse.getResultCode();
	}

	public Object getBody() {
		return rpcResponse.getBody();
	}
}
