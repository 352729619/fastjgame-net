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

	public NetContextImp(long localGuid, RoleType localRole, EventLoop localEventLoop, NetEventLoop netEventLoop, NetManagerWrapper managerWrapper) {
		this.localGuid = localGuid;
		this.localRole = localRole;
		this.localEventLoop = localEventLoop;
		this.netEventLoop = netEventLoop;
		this.managerWrapper = managerWrapper;
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
		// TODO 从可能的地方删除自己
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
		return null;
	}
}
