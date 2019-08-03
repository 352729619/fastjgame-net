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
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class S2CRpcResponseChannel extends AbstractRpcResponseChannel{

	private final S2CSession s2CSession;

	public S2CRpcResponseChannel(boolean sync, long requestGuid, S2CSession s2CSession) {
		super(sync, requestGuid);
		this.s2CSession = s2CSession;
	}

	@Override
	protected void doWrite(boolean sync, long requestGuid, RpcResponse rpcResponse) {
		s2CSession.sendRpcResponse(sync, requestGuid, rpcResponse);
	}
}
