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

import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;

/**
 * 游戏世界事件循环组，它包含一个或多个{@link }
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/28
 * github - https://github.com/hl845740757
 */
public interface GameEventLoopGroup extends EventLoopGroup {

	/**
	 * 分配一个游戏世界事件循环，用作后面的调度
	 * @return GameEventLoop
	 */
	@Nonnull
	@Override
	GameEventLoop next();

	/**
	 * 注册一个游戏世界到某个{@link GameEventLoop}上。
	 * 可以保证的是：registerWorld happens-before {@link World#onStartUp(GameEventLoop)}。
	 *
	 * @param world 游戏世界，基本的信息需要提前初始化
	 * @return Future，可以通过该future查询是否完成，以及进行监听。
	 * 			其实这里完全可以是同步的，和netty的channel注册还是很有区别的，调用量很少，而且基本是启动的时候调用。
	 */
	@Nonnull
	ListenableFuture<?> registerWorld(@Nonnull LogicWorld world);
}
