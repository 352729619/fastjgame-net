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

import com.wjybxx.fastjgame.concurrent.DefaultPromise;
import com.wjybxx.fastjgame.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.manager.NetTriggerManager;
import com.wjybxx.fastjgame.manager.NetTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World的骨架实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public abstract class AbstractWorld implements World{

	private static final Logger logger = LoggerFactory.getLogger(AbstractWorld.class);

	private final Promise<?> terminalFuture = new DefaultPromise(GlobalEventLoop.INSTANCE);

	protected final GameEventLoopManager gameEventLoopManager;
	protected final NetTimeManager netTimeManager;
	protected final NetTriggerManager netTriggerManager;

	public AbstractWorld(GameEventLoopManager gameEventLoopManager, NetTimeManager netTimeManager, NetTriggerManager netTriggerManager) {
		this.gameEventLoopManager = gameEventLoopManager;
		this.netTimeManager = netTimeManager;
		this.netTriggerManager = netTriggerManager;
	}

	@Override
	public final GameEventLoop eventLoop() throws IllegalStateException {
		return gameEventLoopManager.eventLoop();
	}

	@Override
	public final void onStartUp(GameEventLoop eventLoop) throws Exception {
		netTimeManager.changeToRealTimeStrategy();
		gameEventLoopManager.setEventLoop(eventLoop);

		startImp();

		// 启动完成之后切换为按帧缓存策略
		netTimeManager.changeToCacheStrategy();
	}

	/**
	 * 子类的真实启动动作
	 */
	protected abstract void startImp() throws Exception;

	@Override
	public final void tick(long curMillTime) {
		// 更新系统时间
		netTimeManager.tick(curMillTime);

		tickCore(curMillTime);
		tickImp(curMillTime);
	}

	/**
	 * 核心逻辑刷帧
	 */
	private void tickCore(long curMillTime) {
		netTriggerManager.tickTrigger();
	}

	/**
	 * 子类细分逻辑刷帧
	 */
	protected abstract void tickImp(long curMillTime);

	@Override
	public final void onShutdown() throws Exception {
		// 关闭时切换为真实时间
		netTimeManager.changeToRealTimeStrategy();

		try {
			shutdownImp();
		} catch (Exception e){
			// 关闭操作和启动操作都是重要操作尽量不要产生异常
			logger.error("onShutdown caught exception",e);
		} finally {
			terminalFuture.setSuccess(null);
		}
	}

	/**
	 * 子类真实的关闭动作
	 * @throws Exception errors
	 */
	protected abstract void shutdownImp() throws Exception;

	@Override
	public Promise<?> terminateFuture() {
		return terminalFuture;
	}
}
