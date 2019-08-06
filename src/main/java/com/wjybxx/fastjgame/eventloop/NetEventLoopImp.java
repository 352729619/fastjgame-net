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

import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.module.NetEventLoopModule;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.FastCollectionsUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/**
 * 网络事件循环
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class NetEventLoopImp extends SingleThreadEventLoop implements NetEventLoop{

	/** 处理任务时，每次最多处理数量 */
	private static final int MAX_BATCH_SIZE = 2048;

	private final NetManagerWrapper managerWrapper;
	private final NetEventLoopManager netEventLoopManager;
	private final NetConfigManager netConfigManager;
	private final NettyThreadManager nettyThreadManager;
	private final HttpClientManager httpClientManager;
	private final S2CSessionManager s2CSessionManager;
	private final C2SSessionManager c2SSessionManager;
	private final HttpSessionManager httpSessionManager;
	private final NetTimeManager netTimeManager;
	private final NetTimerManager netTimerManager;

	/**
	 * 已注册的用户的EventLoop集合，它是一个安全措施，如果用户在退出时如果没有执行取消操作，
	 * 那么当监听到所在的EventLoop进入终止状态时，取消该EventLoop上注册的用户。
	 */
	private final Set<EventLoop> registeredUserEventLoopSet = new HashSet<>();
	/** 已注册的用户集合 */
	private final Long2ObjectMap<NetContextImp> registeredUserMap = new Long2ObjectOpenHashMap<>();

	NetEventLoopImp(@Nonnull NetEventLoopGroup parent,
					@Nonnull ThreadFactory threadFactory,
					@Nonnull RejectedExecutionHandler rejectedExecutionHandler,
					@Nonnull Injector parentInjector) {
		super(parent, threadFactory, rejectedExecutionHandler);

		// 使得新创建的injector可以直接使用全局单例
		Injector injector = parentInjector.createChildInjector(new NetEventLoopModule());
		managerWrapper = injector.getInstance(NetManagerWrapper.class);
		netConfigManager = managerWrapper.getNetConfigManager();
		// 用于发布自己
		netEventLoopManager = managerWrapper.getNetEventLoopManager();
		// session管理
		s2CSessionManager = managerWrapper.getS2CSessionManager();
		c2SSessionManager = managerWrapper.getC2SSessionManager();
		httpSessionManager = managerWrapper.getHttpSessionManager();
		// 资源需要管理
		nettyThreadManager = managerWrapper.getNettyThreadManager();
		httpClientManager = managerWrapper.getHttpClientManager();
		// 时间管理器和timer管理器
		netTimeManager = managerWrapper.getNetTimeManager();
		netTimerManager = managerWrapper.getNetTimerManager();
		// 解决循环依赖
		s2CSessionManager.setManagerWrapper(managerWrapper);
		c2SSessionManager.setManagerWrapper(managerWrapper);
		httpSessionManager.setManagerWrapper(managerWrapper);
	}

	/**
	 * NetEventLoop不执行阻塞类型的操作，不使用BlockingQueue
	 */
	@Override
	protected Queue<Runnable> newTaskQueue(int maxTaskNum) {
		return new ConcurrentLinkedQueue<>();
	}

	@Nullable
	@Override
	public NetEventLoopGroup parent() {
		return (NetEventLoopGroup) super.parent();
	}

	@Nonnull
	@Override
	public NetEventLoop next() {
		return (NetEventLoop) super.next();
	}

	@Nonnull
	@Override
	public RpcPromise newRpcPromise(@Nonnull EventLoop userEventLoop) {
		return new DefaultRpcPromise(this, userEventLoop);
	}

	@Nonnull
	@Override
	public RpcFuture newCompletedFuture(@Nonnull EventLoop userEventLoop, @Nonnull RpcResponse rpcResponse) {
		return new CompletedRpcFuture(userEventLoop, rpcResponse);
	}

	@Override
	public ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, EventLoop localEventLoop) {
		if (localEventLoop instanceof NetEventLoop) {
			throw new IllegalArgumentException("Unexpected invoke.");
		}
		// 这里一定是逻辑层，不同线程
		return submit(() -> {
			if (registeredUserMap.containsKey(localGuid)) {
				throw new IllegalArgumentException("user " + localGuid + " is already registered!");
			}
			// 创建context
			NetContextImp netContext = new NetContextImp(localGuid, localRole, localEventLoop, this, managerWrapper);
			registeredUserMap.put(localGuid, netContext);
			// 监听用户线程关闭
			if (registeredUserEventLoopSet.add(localEventLoop)) {
				localEventLoop.terminationFuture().addListener(future -> onUserEventLoopTerminal(localEventLoop), this);
			}
			return netContext;
		});
	}

	@Override
	protected void init() throws Exception {
		super.init();
		// Q:为什么没使用threadLocal？
		// A:本来想使用的，但是如果提供一个全局的接口的话，它也会对逻辑层开放，而逻辑层如果调用了一定会导致错误。使用threadLocal暴露了不该暴露的接口。
		// 发布自身，使得该eventLoop的其它管理器可以方便的获取该对象
		netEventLoopManager.publish(this);
		nettyThreadManager.start();
		httpClientManager.start();
	}

	@Override
	protected void loop() {
		for (;;) {
			// 批量执行任务，避免任务太多时导致session得不到及时更新
			runAllTasks(MAX_BATCH_SIZE);

			// 更新时间
			netTimeManager.update(System.currentTimeMillis());

			// 刷帧
			netTimerManager.tickTrigger();
			s2CSessionManager.tick();
			c2SSessionManager.tick();

			if (confirmShutdown()) {
				break;
			}

			// 每次循环休息一下下，避免CPU占有率过高
			sleepQuietly();
		}
	}

	private void sleepQuietly() {
		LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * netConfigManager.frameInterval());
	}

	@Override
	protected void clean() throws Exception {
		super.clean();

		FastCollectionsUtils.removeIfAndThen(registeredUserMap,
				(k, netContext) -> true,
				(k, netContext) -> netContext.afterRemoved());

		ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
		ConcurrentUtils.safeExecute((Runnable) httpClientManager::shutdown);
	}

	@Nonnull
	@Override
	public ListenableFuture<?> deregisterContext(long localGuid) {
		// 逻辑层调用
		return submit(() -> {
			NetContextImp netContext = registeredUserMap.remove(localGuid);
			if (null == netContext) {
				// 早已取消
				return;
			}
			netContext.afterRemoved();
		});
	}

	private void onUserEventLoopTerminal(EventLoop userEventLoop) {
		// 删除该EventLoop相关的所有context
		FastCollectionsUtils.removeIfAndThen(registeredUserMap,
				(k, netContext) -> netContext.localEventLoop() == userEventLoop,
				(k, netContext) -> netContext.afterRemoved());

		// 更彻底的清理
		managerWrapper.getS2CSessionManager().onUserEventLoopTerminal(userEventLoop);
		managerWrapper.getC2SSessionManager().onUserEventLoopTerminal(userEventLoop);
		managerWrapper.getHttpSessionManager().onUserEventLoopTerminal(userEventLoop);
	}
}
