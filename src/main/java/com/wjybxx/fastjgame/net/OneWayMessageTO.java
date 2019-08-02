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
 * 单向消息传输对象
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@Immutable
public class OneWayMessageTO extends MessageTO{

	/** 消息内容，必须是不可变对象 */
	private final Object message;

	public OneWayMessageTO(long ack, long sequence, Object message) {
		super(ack, sequence);
		this.message = message;
	}

	public Object getMessage() {
		return message;
	}
}
