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
 * Http事件传输对象
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public class HttpRequestEventParam implements NetEventParam{

	/** 该事件对应哪一个logicWorld */
	private final long logicWorldGuid;

	private final Channel channel;

	private final HttpRequestTO httpRequestTO;

	public HttpRequestEventParam(long logicWorldGuid, Channel channel, HttpRequestTO httpRequestTO) {
		this.logicWorldGuid = logicWorldGuid;
		this.channel = channel;
		this.httpRequestTO = httpRequestTO;
	}

	@Override
	public long logicWorldGuid() {
		return logicWorldGuid;
	}

	@Override
	public long remoteLogicWorldGuid() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Channel channel() {
		return channel;
	}

	public HttpRequestTO getHttpRequestTO() {
		return httpRequestTO;
	}
}
