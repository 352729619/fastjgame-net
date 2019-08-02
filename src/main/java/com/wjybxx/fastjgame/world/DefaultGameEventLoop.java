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

import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;

/**
 * 抽象的游戏世界循环
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
public class DefaultGameEventLoop extends SingleThreadEventLoop implements GameEventLoop{

	private static final Logger logger = LoggerFactory.getLogger(DefaultGameEventLoop.class);

	/** netWorld是否注册在该EventLoop上 */
	private final NetWorld netWorld;
	/**
	 * 注册在该EventLoop上的logicWorld
	 */
	private final Long2ObjectMap<LogicWorld> logicWorldMap = new Long2ObjectOpenHashMap<>();

	public DefaultGameEventLoop(@Nullable EventLoopGroup parent,
								@Nonnull ThreadFactory threadFactory,
								@Nullable NetWorld netWorld) {
		this(parent, threadFactory, RejectedExecutionHandlers.reject(), netWorld);
	}

	public DefaultGameEventLoop(@Nullable EventLoopGroup parent,
								@Nonnull ThreadFactory threadFactory,
								@Nonnull RejectedExecutionHandler rejectedExecutionHandler,
								@Nullable NetWorld netWorld) {
		super(parent, threadFactory, rejectedExecutionHandler);
		this.netWorld = netWorld;
	}

	@Nullable
	@Override
	public GameEventLoopGroup parent() {
		return (GameEventLoopGroup) super.parent();
	}

	@Nonnull
	@Override
	public GameEventLoop next() {
		return (GameEventLoop) super.next();
	}

	@Override
	protected void loop() {
		for (;;) {

			runAllTasks();

			onWaitEvent();

			if (confirmShutdown()) {
				break;
			}
		}
	}

	private void onWaitEvent() {
		long curTimeMills = System.currentTimeMillis();
		// 网络层tick
		if (null != netWorld) {
			try {
				netWorld.tick(curTimeMills);
			} catch (Exception e){
				logger.warn("netWorld onWaitEventCaught Exception.", e);
			}
		}
		// 逻辑层tick
		for (LogicWorld logicWorld:logicWorldMap.values()) {
			try {
				logicWorld.tick(curTimeMills);
			} catch (Exception e){
				logger.warn("logicWorld {}-{} tick caught Exception.", logicWorld.worldType(), logicWorld.worldGuid(), e);
			}
		}
	}


	@Nonnull
	@Override
	public ListenableFuture<?> registerWorld(@Nonnull LogicWorld world) {
		return EventLoopUtils.submitOrRun(this, () -> {
			registerLogicWorld(world);
			return null;
		});
	}
	
	private void registerLogicWorld(LogicWorld logicWorld) throws IllegalArgumentException {
		if (logicWorldMap.containsKey(logicWorld.worldGuid())) {
			throw new IllegalArgumentException("logicWorld " + logicWorld.worldType() + "-" + logicWorld.worldGuid() + " already registered.");
		}
		try {
			logicWorld.onStartUp(this);
			// 启动成功才放入集合
			logicWorldMap.put(logicWorld.worldGuid(), logicWorld);
		} catch (Exception e) {
			// TODO 这个错误到底怎么处理合适，还是蛮难的。
			logger.error("logicWorld {}-{} onStartUp caught exception.", logicWorld.worldType(), logicWorld.worldGuid(), e);
			try {
				logicWorld.onShutdown();
			} catch (Exception e1) {
				logger.error("logicWorld {}-{} onShutdown caught exception.", logicWorld.worldType(), logicWorld.worldGuid(), e1);
			}finally {
				// 如果logicWorld在shutdown的时候没有对future赋值，那么这里必须处理
				logicWorld.terminateFuture().tryFailure(e);
			}
		}
	}

	private static class WorldWrapper<T extends World> {

		private final T world;
		private final long frameInterval;
		private long nextTickTimeMs;

		private WorldWrapper(T world, long frameInterval) {
			this.world = world;
			this.frameInterval = frameInterval;
		}
	}
}
