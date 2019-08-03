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

/**
 * 未发送的rpc结果。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class UnsentRpcResponse implements UnsentMessage{

	private final long requestGuid;
	/** rpc响应结果，网络层不对其做限制 */
	private final RpcResponse rpcResponse;

	public UnsentRpcResponse(long requestGuid, RpcResponse rpcResponse) {
		this.requestGuid = requestGuid;
		this.rpcResponse = rpcResponse;
	}

	@Override
	public NetMessage build(long sequence) {
		return new RpcResponseMessage(sequence, requestGuid, rpcResponse);
	}
}
