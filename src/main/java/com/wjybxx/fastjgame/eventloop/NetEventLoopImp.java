package com.wjybxx.fastjgame.eventloop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.wjybxx.fastjgame.concurrent.*;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.module.NetModule;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.TimeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

	private final NetManagerWrapper managerWrapper;
	private final NetConfigManager netConfigManager;
	private final NettyThreadManager nettyThreadManager;
	private final HttpClientManager httpClientManager;
	private final S2CSessionManager s2CSessionManager;
	private final C2SSessionManager c2SSessionManager;
	private final HttpSessionManager httpSessionManager;
	private final NetTimeManager netTimeManager;
	private final NetTimerManager netTimerManager;

	public NetEventLoopImp(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory) {
		this(parent, threadFactory, RejectedExecutionHandlers.reject());
	}

	public NetEventLoopImp(@Nullable EventLoopGroup parent, @Nonnull ThreadFactory threadFactory, @Nonnull RejectedExecutionHandler rejectedExecutionHandler) {
		super(parent, threadFactory, rejectedExecutionHandler);

		Injector injector = Guice.createInjector(new NetModule());
		// 发布自身，使得该eventLoop的其它管理器可以方便的获取该对象
		// Q:为什么没使用threadLocal？
		// A:本来想使用的，但是如果提供一个全局的接口的话，它也会对逻辑层开放，而逻辑层如果调用了一定会导致错误。使用threadLocal暴露了不该暴露的接口。
		injector.getInstance(NetEventLoopManager.class).init(this);
		// 创建其它管理器
		managerWrapper = injector.getInstance(NetManagerWrapper.class);
		netConfigManager = managerWrapper.getNetConfigManager();
		nettyThreadManager = managerWrapper.getNettyThreadManager();
		httpClientManager = managerWrapper.getHttpClientManager();
		s2CSessionManager = managerWrapper.getS2CSessionManager();
		c2SSessionManager = managerWrapper.getC2SSessionManager();
		httpSessionManager = managerWrapper.getHttpSessionManager();
		netTimeManager = managerWrapper.getNetTimeManager();
		netTimerManager = managerWrapper.getNetTimerManager();
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
	public ListenableFuture<NetContext> registerUser(long localGuid, RoleType localRole, EventLoop localEventLoop) {
		if (localEventLoop instanceof NetEventLoop) {
			throw new IllegalArgumentException("Unexpected invoke.");
		}
		return submit(() -> NetContextImp.newInstance(localGuid, localRole, localEventLoop, this, managerWrapper));
	}

	@Override
	protected void init() throws Exception {
		super.init();
		nettyThreadManager.start();
		httpClientManager.start();
	}

	@Override
	protected void loop() {
		for (;;) {
			// 更新时间
			netTimeManager.update(System.currentTimeMillis());
			// 刷帧
			netTimerManager.tickTrigger();
			c2SSessionManager.tick();

			if (confirmShutdown()) {
				break;
			}

			// 每次循环休息一下下，避免CPU占有率过高
			LockSupport.parkNanos(TimeUtils.NANO_PER_MILLISECOND * netConfigManager.frameInterval());
		}
	}

	@Override
	protected void clean() throws Exception {
		super.clean();
		ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
		ConcurrentUtils.safeExecute((Runnable) httpClientManager::shutdown);
	}

}
