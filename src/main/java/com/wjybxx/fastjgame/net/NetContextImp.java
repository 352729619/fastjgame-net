package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;

import java.net.BindException;

/**
 * NetContext的基本实现
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class NetContextImp implements NetContext {

	private final long localGuid;
	private final RoleType localRole;
	private final EventLoop localEventLoop;
	private final NetEventLoop netEventLoop;
	private final NetManagerWrapper managerWrapper;

	private NetContextImp(long localGuid, RoleType localRole, EventLoop localEventLoop,
						 NetEventLoop netEventLoop, NetManagerWrapper managerWrapper) {
		this.localGuid = localGuid;
		this.localRole = localRole;
		this.localEventLoop = localEventLoop;
		this.netEventLoop = netEventLoop;
		this.managerWrapper = managerWrapper;
	}

	/**
	 * 避免构造的时候发布自己
	 */
	public static NetContext newInstance(long localGuid, RoleType localRole, EventLoop localEventLoop,
										 NetEventLoop netEventLoop, NetManagerWrapper managerWrapper) {
		NetContextImp netContextImp = new NetContextImp(localGuid, localRole, localEventLoop, netEventLoop, managerWrapper);
		localEventLoop.terminationFuture().addListener(future -> {
			netContextImp.onUserEventLoopShutdown();
		}, netEventLoop);
		return netContextImp;
	}

	@Override
	public long localGuid() {
		return localGuid;
	}

	@Override
	public RoleType localRole() {
		return localRole;
	}

	@Override
	public EventLoop localEventLoop() {
		return localEventLoop;
	}

	@Override
	public NetEventLoop netEventLoop() {
		return netEventLoop;
	}

	@Override
	public void unregister() {
		// 从可能的地方删除自己
		netEventLoop.execute(() -> {
			managerWrapper.getS2CSessionManager().removeUserSession(localGuid, "unregister");
			managerWrapper.getC2SSessionManager().removeUserSession(localGuid, "unregister");
			managerWrapper.getHttpSessionManager().removeUserSession(localGuid);
		});
	}

	/**
	 * 当用户所在的EventLoop关闭了
	 */
	private void onUserEventLoopShutdown() {
		assert netEventLoop.inEventLoop();

		managerWrapper.getS2CSessionManager().onUserEventLoopTerminal(localEventLoop);
		managerWrapper.getC2SSessionManager().onUserEventLoopTerminal(localEventLoop);
		managerWrapper.getHttpSessionManager().onUserEventLoopTerminal(localEventLoop);
	}

	@Override
	public ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializerSupplier initializerSupplier, SessionLifecycleAware<S2CSession> lifecycleAware, MessageHandler messageHandler) {
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getS2CSessionManager().bindRange(this, outer, portRange,
						initializerSupplier, lifecycleAware, messageHandler);
			} catch (BindException e){
				ConcurrentUtils.rethrow(e);
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, ChannelInitializerSupplier initializerSupplier, SessionLifecycleAware<C2SSession> lifecycleAware, MessageHandler messageHandler) {
		return netEventLoop.submit(() -> {
			managerWrapper.getC2SSessionManager().connect(this, remoteGuid, remoteRole, remoteAddress,
					initializerSupplier, lifecycleAware, messageHandler);
		});
	}

	@Override
	public ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializerSupplier initializerSupplier, HttpRequestHandler httpRequestHandler) {
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getHttpSessionManager().bindRange(this, outer, portRange,
						initializerSupplier, httpRequestHandler);
			} catch (Exception e){
				ConcurrentUtils.rethrow(e);
				return null;
			}
		});
	}
}
