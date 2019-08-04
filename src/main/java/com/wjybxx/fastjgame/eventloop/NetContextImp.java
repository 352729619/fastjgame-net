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

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import okhttp3.Response;

import java.io.IOException;
import java.net.BindException;
import java.util.Map;

/**
 * NetContext的基本实现
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
class NetContextImp implements NetContext {

	private final long localGuid;
	private final RoleType localRole;
	private final EventLoop localEventLoop;
	private final NetEventLoopImp netEventLoop;
	private final NetManagerWrapper managerWrapper;

	NetContextImp(long localGuid, RoleType localRole, EventLoop localEventLoop,
						  NetEventLoopImp netEventLoop, NetManagerWrapper managerWrapper) {
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
	public NetEventManager netEventManager() {
		return managerWrapper.getNetEventManager();
	}

	@Override
	public ListenableFuture<?> unregister() {
		return netEventLoop.deregisterContext(localGuid);
	}

	void afterRemoved() {
		// 尝试删除自己的痕迹
		managerWrapper.getS2CSessionManager().removeUserSession(localGuid, "unregister");
		managerWrapper.getC2SSessionManager().removeUserSession(localGuid, "unregister");
		managerWrapper.getHttpSessionManager().removeUserSession(localGuid);
	}

	@Override
	public ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializer<SocketChannel> initializer, SessionLifecycleAware<S2CSession> lifecycleAware, MessageHandler messageHandler) {
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getS2CSessionManager().bindRange(this, outer, portRange,
						initializer, lifecycleAware, messageHandler);
			} catch (BindException e){
				ConcurrentUtils.rethrow(e);
				// unreachable
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

	// ------------------------------------------- http 实现 ----------------------------------------

	@Override
	public ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializer<SocketChannel> initializer, HttpRequestHandler httpRequestHandler) {
		return netEventLoop.submit(() -> {
			try {
				return managerWrapper.getHttpSessionManager().bindRange(this, outer, portRange,
						initializer, httpRequestHandler);
			} catch (Exception e){
				ConcurrentUtils.rethrow(e);
				// unreachable
				return null;
			}
		});
	}

	@Override
	public Response syncGet(String url, Map<String, String> params) throws IOException {
		return managerWrapper.getHttpClientManager().syncGet(url, params);
	}

	@Override
	public void asyncGet(String url, Map<String, String> params, OkHttpCallback okHttpCallback) {
		managerWrapper.getHttpClientManager().asyncGet(url, params, localEventLoop, okHttpCallback);
	}

	@Override
	public Response syncPost(String url, Map<String, String> params) throws IOException {
		return managerWrapper.getHttpClientManager().syncPost(url, params);
	}

	@Override
	public void asyncPost(String url, Map<String, String> params, OkHttpCallback okHttpCallback) {
		managerWrapper.getHttpClientManager().asyncPost(url, params, localEventLoop, okHttpCallback);
	}
}
