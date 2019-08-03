package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;
import com.wjybxx.fastjgame.manager.networld.*;

/**
 * NetEventLoop不是依赖注入的，一个个获取实例实在有点麻烦...
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public class NetManagerWrapper {

	private final NetEventLoopManager netEventLoopManager;
	private final S2CSessionManager s2CSessionManager;
	private final C2SSessionManager c2SSessionManager;
	private final HttpSessionManager httpSessionManager;
	private final NetEventManager netEventManager;
	private final NettyThreadManager nettyThreadManager;

	private final NetConfigManager netConfigManager;
	private final AcceptManager acceptManager;
	private final HttpClientManager httpClientManager;
	private final NetTimeManager netTimeManager;
	private final NetTimerManager netTimerManager;
	private final TokenManager tokenManager;

	@Inject
	public NetManagerWrapper(NetEventLoopManager netEventLoopManager, S2CSessionManager s2CSessionManager, C2SSessionManager c2SSessionManager,
							 HttpSessionManager httpSessionManager, NetEventManager netEventManager,
							 NettyThreadManager nettyThreadManager, NetConfigManager netConfigManager, AcceptManager acceptManager,
							 HttpClientManager httpClientManager, NetTimeManager netTimeManager,
							 NetTimerManager netTimerManager, TokenManager tokenManager) {
		this.netEventLoopManager = netEventLoopManager;
		this.s2CSessionManager = s2CSessionManager;
		this.c2SSessionManager = c2SSessionManager;
		this.httpSessionManager = httpSessionManager;
		this.netEventManager = netEventManager;
		this.nettyThreadManager = nettyThreadManager;
		this.netConfigManager = netConfigManager;
		this.acceptManager = acceptManager;

		this.httpClientManager = httpClientManager;
		this.netTimeManager = netTimeManager;
		this.netTimerManager = netTimerManager;
		this.tokenManager = tokenManager;
	}

	public NetEventLoopManager getNetEventLoopManager() {
		return netEventLoopManager;
	}

	public S2CSessionManager getS2CSessionManager() {
		return s2CSessionManager;
	}

	public C2SSessionManager getC2SSessionManager() {
		return c2SSessionManager;
	}

	public HttpSessionManager getHttpSessionManager() {
		return httpSessionManager;
	}

	public NetEventManager getNetEventManager() {
		return netEventManager;
	}

	public NettyThreadManager getNettyThreadManager() {
		return nettyThreadManager;
	}

	public AcceptManager getAcceptManager() {
		return acceptManager;
	}

	public HttpClientManager getHttpClientManager() {
		return httpClientManager;
	}

	public NetTimeManager getNetTimeManager() {
		return netTimeManager;
	}

	public NetTimerManager getNetTimerManager() {
		return netTimerManager;
	}

	public TokenManager getTokenManager() {
		return tokenManager;
	}

	public NetConfigManager getNetConfigManager() {
		return netConfigManager;
	}
}
