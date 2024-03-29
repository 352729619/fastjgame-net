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
 * Rpc响应事件参数
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/30
 * github - https://github.com/hl845740757
 */
public class RpcResponseEventParam extends MessageEventParam{

	/**
	 * 服务器回话id，从channelHandler上取出来的
	 */
	private final long remoteGuid;
	/**
	 * Rpc响应内容
	 */
	private final RpcResponseMessageTO responseMessageTO;

	public RpcResponseEventParam(Channel channel, long localGuid, long remoteGuid, RpcResponseMessageTO responseMessageTO) {
		super(channel, localGuid);
		this.remoteGuid = remoteGuid;
		this.responseMessageTO = responseMessageTO;
	}

	@Override
	public RpcResponseMessageTO messageTO() {
		return responseMessageTO;
	}

	@Override
	public long remoteGuid() {
		return remoteGuid;
	}
}
