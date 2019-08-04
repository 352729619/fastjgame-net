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

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Rpc请求消息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
public class RpcRequestMessage extends NetMessage{

	/** 是否是同步rpc调用，是否加急 */
	private boolean sync;
	/** rpc请求编号，用于返回消息 */
	private long requestGuid;
	/** rpc请求内容 */
	private Object request;

	public RpcRequestMessage(long sequence, boolean sync, long requestGuid, Object request) {
		super(sequence);
		this.sync = sync;
		this.requestGuid = requestGuid;
		this.request = request;
	}

	@Override
	public RpcRequestMessageTO build(long ack) {
		return new RpcRequestMessageTO(ack, getSequence(), sync, requestGuid,  request);
	}

	public boolean isSync() {
		return sync;
	}

	public long getRequestGuid() {
		return requestGuid;
	}

	public Object getRequest() {
		return request;
	}
}
