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

package com.wjybxx.fastjgame.eventloop;

import com.google.inject.Inject;

/**
 * NetEventLoop管理器，使得NetModule中的管理器可以获取运行环境。
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class NetEventLoopManager {

	private NetEventLoopImp eventLoop;

	@Inject
	public NetEventLoopManager() {

	}
	/** 不允许外部调用，保证安全性 */
	void publish(NetEventLoopImp eventLoop) {
		this.eventLoop = eventLoop;
	}

	public NetEventLoopImp eventLoop() {
		return eventLoop;
	}

	public boolean inEventLoop() {
		if (null == eventLoop) {
			throw new IllegalStateException();
		}
		return eventLoop.inEventLoop();
	}
}
