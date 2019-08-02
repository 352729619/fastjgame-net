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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.manager.logicworld.HttpDispatchManager;
import com.wjybxx.fastjgame.manager.logicworld.MessageDispatchManager;
import com.wjybxx.fastjgame.net.RoleType;

/**
 * LogicWorld在NetWorld中的信息
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public class LogicWorldInNetWorldInfo {

	/** LogicWorld的唯一标识 */
	private final long worldGuid;
	/**LogicWorld的类型 */
	private final RoleType worldType;
	/** LogicWorld所在的EventLoop */
	private final EventLoop eventLoop;
	/** http请求分发器 */
	private final HttpDispatchManager httpDispatchManager;
	/** rpc和单向消息分发器 */
	private final MessageDispatchManager messageDispatchManager;

	public LogicWorldInNetWorldInfo(long worldGuid, RoleType worldType, EventLoop eventLoop,
									HttpDispatchManager httpDispatchManager,
									MessageDispatchManager messageDispatchManager) {
		this.worldGuid = worldGuid;
		this.worldType = worldType;
		this.eventLoop = eventLoop;
		this.httpDispatchManager = httpDispatchManager;
		this.messageDispatchManager = messageDispatchManager;
	}

	public long getWorldGuid() {
		return worldGuid;
	}

	public RoleType getWorldType() {
		return worldType;
	}

	public EventLoop getEventLoop() {
		return eventLoop;
	}

	public HttpDispatchManager getHttpDispatchManager() {
		return httpDispatchManager;
	}

	public MessageDispatchManager getMessageDispatchManager() {
		return messageDispatchManager;
	}
}
