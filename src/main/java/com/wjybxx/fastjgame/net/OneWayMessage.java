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
 * 单向消息，用于与玩家通信，或服务器内的单向通知。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class OneWayMessage extends NetMessage{

	/**
	 * 消息内容，必须是不可变对象。
	 * 不要求是protoBuf形式
	 */
	private Object message;

	public OneWayMessage(long sequence, Object message) {
		super(sequence);
		this.message = message;
	}

	@Override
	public OneWayMessageTO build(long ack) {
		return new OneWayMessageTO(ack, getSequence(), message);
	}
}
