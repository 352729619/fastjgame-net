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

package com.wjybxx.fastjgame.world;

import com.wjybxx.fastjgame.concurrent.EventLoop;

import javax.annotation.Nullable;

/**
 * 它是{@link World}的运行环境，它管理一个或多个World。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/28
 * github - https://github.com/hl845740757
 */
public interface GameEventLoop extends GameEventLoopGroup, EventLoop {

	/**
	 * 返回持有GameEventLoop的容器，可能不存在。
	 * @return GameEventLoopGroup
	 */
	@Nullable
	@Override
	GameEventLoopGroup parent();

}
