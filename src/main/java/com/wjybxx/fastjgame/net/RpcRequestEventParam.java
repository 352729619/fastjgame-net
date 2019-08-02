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

import io.netty.channel.Channel;

/**
 * RPC请求事件参数。 {@link NetPackageType#RPC_REQUEST}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/30
 * github - https://github.com/hl845740757
 */
public class RpcRequestEventParam extends MessageEventParam{
	/**
	 * 会话唯一标识，对方唯一标识。
	 * channel上取出来的。
	 */
	private final long clientGuid;
	/**
	 * Rpc请求信息
	 */
	private final RpcRequestMessageTO requestMessageTO;

	public RpcRequestEventParam(Channel channel, long logicWorldGuid, long clientGuid, RpcRequestMessageTO requestMessageTO) {
		super(logicWorldGuid, channel);
		this.clientGuid = clientGuid;
		this.requestMessageTO = requestMessageTO;
	}

	@Override
	public RpcRequestMessageTO messageTO() {
		return requestMessageTO;
	}

	@Override
	public long remoteLogicWorldGuid() {
		return clientGuid;
	}
}
