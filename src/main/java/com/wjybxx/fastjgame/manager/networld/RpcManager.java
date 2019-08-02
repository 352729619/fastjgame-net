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

package com.wjybxx.fastjgame.manager.networld;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.GlobalEventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.concurrent.Promise;
import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import com.wjybxx.fastjgame.manager.logicworld.HttpDispatchManager;
import com.wjybxx.fastjgame.manager.logicworld.MessageDispatchManager;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.RoleType;
import com.wjybxx.fastjgame.net.RpcResponse;
import com.wjybxx.fastjgame.net.RpcResultCode;
import com.wjybxx.fastjgame.net.S2CSession;
import com.wjybxx.fastjgame.net.SessionLifecycleAware;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import com.wjybxx.fastjgame.net.rpc.CallImp;
import com.wjybxx.fastjgame.net.rpc.RpcCallback;
import com.wjybxx.fastjgame.net.rpc.RpcSucceedCallback;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import com.wjybxx.fastjgame.world.LogicWorld;
import com.wjybxx.fastjgame.world.NetWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link NetWorld}上用于管理rpc的管理器。实现为线程安全。
 * 负责所有的通信管理。
 * LogicWorld的代码最好不要直接使用该管理器，应该根据自己的角色提供相应的门面控制器。
 * 使用该控制的方法之前必须先进行了注册{@link LogicWorldManager#registerLogicWorld(LogicWorld, HttpDispatchManager, MessageDispatchManager)}。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class RpcManager {

	private static final Logger logger = LoggerFactory.getLogger(RpcManager.class);

	private final GameEventLoopManager gameEventLoopManager;
	private final S2CSessionManager s2CSessionManager;
	private final C2SSessionManager c2SSessionManager;
	private final HttpSessionManager httpSessionManager;
	private final LogicWorldManager logicWorldManager;
	private final NetConfigManager netConfigManager;

	@Inject
	public RpcManager(GameEventLoopManager gameEventLoopManager, S2CSessionManager s2CSessionManager,
					  C2SSessionManager c2SSessionManager, HttpSessionManager httpSessionManager,
					  LogicWorldManager logicWorldManager, NetConfigManager netConfigManager) {
		this.gameEventLoopManager = gameEventLoopManager;
		this.s2CSessionManager = s2CSessionManager;
		this.c2SSessionManager = c2SSessionManager;
		this.httpSessionManager = httpSessionManager;
		this.logicWorldManager = logicWorldManager;
		this.netConfigManager = netConfigManager;
	}

	private void ensureRegistered(long logicWorldGuid) {
		logicWorldManager.ensureRegistered(logicWorldGuid);
	}

	/**
	 * 尝试绑定到指定端口，若端口已被占用，则会失败。
	 * 建议使用{@link #bindRange(long, boolean, PortRange, ChannelInitializerSupplier, SessionLifecycleAware)}
	 *
	 * @param worldGuid logicWorld的guid
	 * @param outer 是否外网端口
	 * @param port 端口号
	 * @param initializerSupplier initializer提供器
	 * @param sessionLifecycleAware 监听的端口上的会话通知器
	 * @return 绑定的地址信息
	 */
	public ListenableFuture<HostAndPort> bind(long worldGuid, boolean outer, int port,
											  ChannelInitializerSupplier initializerSupplier,
											  SessionLifecycleAware<S2CSession> sessionLifecycleAware) {
		return EventLoopUtils.submitOrRun(gameEventLoopManager.eventLoop(), () -> {
			ensureRegistered(worldGuid);
			return s2CSessionManager.bind(worldGuid, outer, port, initializerSupplier, sessionLifecycleAware);
		});
	}

	/**
	 * 尝试绑定到端口区间的某一个端口。
	 *
	 * @param worldGuid logicWorld的guid
	 * @param outer 是否外网端口
	 * @param portRange 端口号范围
	 * @param initializerSupplier initializer提供器
	 * @param sessionLifecycleAware 监听的端口上的会话通知器
	 * @return 绑定的地址信息
	 */
	public ListenableFuture<HostAndPort> bindRange(long worldGuid, boolean outer, PortRange portRange,
												   ChannelInitializerSupplier initializerSupplier,
												   SessionLifecycleAware<S2CSession> sessionLifecycleAware) {
		return EventLoopUtils.submitOrRun(gameEventLoopManager.eventLoop(), () -> {
			ensureRegistered(worldGuid);
			return s2CSessionManager.bindRange(worldGuid, outer, portRange, initializerSupplier, sessionLifecycleAware);
		});
	}

	public void connect(long worldGuid, RoleType worldType) {
		ensureRegistered(worldGuid);
	}

	/**
	 * 向远程发送一个单向消息
	 * @param worldGuid 我的标识(请求方标识)
	 * @param remoteWorldGuid 远程world标识
	 * @param message 发送的消息内容
	 */
	public void send(long worldGuid, long remoteWorldGuid, @Nonnull Object message) {
		EventLoopUtils.executeOrRun(gameEventLoopManager.eventLoop(), () -> {
			send(worldGuid, remoteWorldGuid, message);
		});
	}

	private void sendImp(long worldGuid, long remoteWorldGuid, @Nonnull Object message) {
		assert gameEventLoopManager.inEventLoop();
		// 会话是我发起的
		if (c2SSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			c2SSessionManager.send(worldGuid, remoteWorldGuid, message);
			return;
		}
		// 会话是对方发起的
		if (s2CSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			s2CSessionManager.send(worldGuid, remoteWorldGuid, message);
			return;
		}
		logger.warn("session {}-{} is not exist! But try send message {}.", worldGuid, remoteWorldGuid, message.getClass().getSimpleName());
	}

	/**
	 *
	 * 发送异步rpc请求。
	 * (一个快捷接口，可以使用lambda表达式监听)
	 *
	 * @param worldGuid 我的标识(请求方标识)
	 * @param remoteWorldGuid 远程world标识
	 * @param request rpc请求内容
	 * @param rpcCallback rpc回调，可以使用lambda表达式
	 */
	public void rpc(long worldGuid, long remoteWorldGuid, @Nonnull Object request, RpcSucceedCallback rpcCallback) {
		this.rpc(worldGuid, remoteWorldGuid, request, (RpcCallback)rpcCallback);
	}

	/**
	 * 发送异步rpc请求
	 * @param worldGuid 我的标识(请求方标识)
	 * @param remoteWorldGuid 远程world标识
	 * @param request rpc请求内容
	 */
	public void rpc(long worldGuid, long remoteWorldGuid, @Nonnull Object request, RpcCallback rpcCallback) {
		// 不会阻塞调用，谁创建的promise都可以
		Promise<RpcResponse> responsePromise = gameEventLoopManager.eventLoop().newPromise();
		EventLoopUtils.executeOrRun(gameEventLoopManager.eventLoop(), () -> {
			ensureRegistered(worldGuid);
			rpcImp(worldGuid, remoteWorldGuid, request, responsePromise);
		});

		responsePromise.addListener(future -> {
			RpcResponse rpcResponse = future.tryGet();
			if (null != rpcResponse) {
				rpcCallback.onComplete(new CallImp(remoteWorldGuid, request), rpcResponse);
			} else {
				rpcCallback.onComplete(new CallImp(remoteWorldGuid, request), new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, future.cause()));
			}
		});

	}

	/**
	 * 发起同步rpc请求
	 * @param worldGuid 我的标识(请求方标识)
	 * @param remoteWorldGuid 远程world标识
	 * @param request rpc请求内容
	 * @return rpc调用结果
	 */
	public RpcResponse syncRpc(long worldGuid, long remoteWorldGuid, Object request) {
		// 不可以在logicWorld创建promise上等待，否则可能死锁
		// 此外，如果logicWorld和netWorld在同一个线程上，也很危险
		final Promise<RpcResponse> responsePromise;
		if (gameEventLoopManager.inEventLoop()) {
			responsePromise = GlobalEventLoop.INSTANCE.newPromise();
		} else {
			responsePromise = gameEventLoopManager.eventLoop().newPromise();
		}

		EventLoopUtils.executeOrRun(gameEventLoopManager.eventLoop(), () -> {
			ensureRegistered(worldGuid);
			rpcImp(worldGuid, remoteWorldGuid, request, responsePromise);
		});

		try {
			return responsePromise.get(netConfigManager.syncRpcTimeoutMs(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException e) {
			return new RpcResponse(RpcResultCode.LOCAL_EXCEPTION, e);
		} catch (TimeoutException e) {
			return new RpcResponse(RpcResultCode.TIMEOUT, null);
		}
	}

	/** rpc的真正实现 */
	private void rpcImp(long worldGuid, long remoteWorldGuid, @Nonnull Object request, Promise<RpcResponse> responsePromise) {
		assert gameEventLoopManager.inEventLoop();
		// 会话是我发起的
		if (c2SSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			c2SSessionManager.rpc(worldGuid, remoteWorldGuid, request, responsePromise);
			return;
		}
		// 会话是对方发起的
		if (s2CSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			s2CSessionManager.rpc(worldGuid, remoteWorldGuid, request, responsePromise);
			return;
		}
		responsePromise.trySuccess(new RpcResponse(RpcResultCode.SESSION_NOT_EXIST, null));
	}

	/**
	 * 发送rpc响应
	 * @param worldGuid 我的标识(请求方标识)
	 * @param remoteWorldGuid 远程world标识
	 * @param requestGuid 唯一请求id
	 * @param response rpc结果
	 */
	public void senRpcResponse(long worldGuid, long remoteWorldGuid, long requestGuid, RpcResponse response) {
		EventLoopUtils.executeOrRun(gameEventLoopManager.eventLoop(), () -> {
			sendRpcResponseImp(worldGuid, remoteWorldGuid, requestGuid, response);
		});
	}

	private void sendRpcResponseImp(long worldGuid, long remoteWorldGuid, long requestGuid, RpcResponse response) {
		assert gameEventLoopManager.inEventLoop();
		// 会话是我发起的
		if (c2SSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			c2SSessionManager.sendRpcResponse(worldGuid, remoteWorldGuid, requestGuid, response);
			return;
		}
		// 会话是对方发起的
		if (s2CSessionManager.containsSession(worldGuid, remoteWorldGuid)) {
			s2CSessionManager.sendRpcResponse(worldGuid, remoteWorldGuid, requestGuid, response);
			return;
		}
		logger.warn("session {}-{} is not exist! But try send rpc response.", worldGuid, remoteWorldGuid);
	}
}
