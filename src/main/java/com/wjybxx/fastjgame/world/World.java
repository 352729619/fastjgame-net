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

import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.concurrent.event.Event;

import javax.annotation.Nonnull;

/**
 * 游戏世界
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/28
 * github - https://github.com/hl845740757
 */
public interface World {

	/**
	 * 游戏世界所属的事件循环。
	 * 必须保证在{@link #onStartUp(GameEventLoop)}之后可获得（需要保证线程安全）。
	 *
	 * @return world注册到的事件循环
	 * @throws IllegalStateException 如果尚未注册，则抛出该异常。
	 */
	GameEventLoop eventLoop() throws IllegalStateException;

	/**
	 * 返回的future将在world成功关闭后收到通知
	 * @return future
	 */
	Promise<?> terminateFuture();

	// ------------------------------------- 以下方法皆在World绑定到的EventLoop线程中执行 ---------------------------
	/**
	 * 游戏世界的配置，world内使用。
	 * @return config
	 */
	@Nonnull
	WorldConfig config();

	/**
	 * 当world注册到{@link #eventLoop()}上时，会启动world。
	 * @param eventLoop 该world注册到的eventLoop
	 */
	void onStartUp(GameEventLoop eventLoop) throws Exception;

	/**
	 * world刷帧。
	 * 会在指定间隔的时候刷帧{@link WorldConfig#framesPerSecond()}
	 *
	 * @param curMillTime 当前系统时间戳
	 */
	void tick(long curMillTime);

	/**
	 * 当world将要从{@link #eventLoop()}上取消注册时，会关闭world。
	 */
	void onShutdown() throws Exception;

}
