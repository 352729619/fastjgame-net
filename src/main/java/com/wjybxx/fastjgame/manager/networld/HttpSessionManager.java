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
import com.wjybxx.fastjgame.configwrapper.ConfigWrapper;
import com.wjybxx.fastjgame.manager.NetConfigManager;
import com.wjybxx.fastjgame.manager.NetTriggerManager;
import com.wjybxx.fastjgame.manager.NetTimeManager;
import com.wjybxx.fastjgame.misc.HttpResponseHelper;
import com.wjybxx.fastjgame.misc.LogicWorldInNetWorldInfo;
import com.wjybxx.fastjgame.net.HttpRequestEventParam;
import com.wjybxx.fastjgame.net.HttpSession;
import com.wjybxx.fastjgame.trigger.Timer;
import com.wjybxx.fastjgame.utils.CollectionUtils;
import com.wjybxx.fastjgame.utils.EventLoopUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * HttpSession管理器，NetWorld中的控制器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
@NotThreadSafe
public class HttpSessionManager {

	private final NetConfigManager netConfigManager;
	private final NetTimeManager netTimeManager;
	private final LogicWorldManager logicWorldManager;
	/**
	 * channel->session
	 */
	private final Map<Channel, HttpSessionWrapper> sessionWrapperMap = new IdentityHashMap<>();

	@Inject
	public HttpSessionManager(NetTriggerManager netTriggerManager, NetConfigManager netConfigManager, NetTimeManager netTimeManager, LogicWorldManager logicWorldManager) {
		this.netConfigManager = netConfigManager;
		this.netTimeManager = netTimeManager;
		this.logicWorldManager = logicWorldManager;
		Timer timer = new Timer(this.netConfigManager.httpSessionTimeout()*1000,Integer.MAX_VALUE,this::checkSessionTimeout);
		netTriggerManager.addTimer(timer);
	}

	/**
	 * 检查session超时
	 */
	private void checkSessionTimeout(Timer timer){
		// 如果logicWorld持有了httpSession的引用，长时间没有完成响应的话，这里关闭可能导致一些错误
		CollectionUtils.removeIfAndThen(sessionWrapperMap,
				(channel, sessionWrapper) -> netTimeManager.getSystemSecTime() > sessionWrapper.getSessionTimeout(),
				(channel, sessionWrapper) -> NetUtils.closeQuietly(channel));
	}

	/**
	 * 当收到http请求时
	 * @param requestEventParam 请参数
	 */
	public void onRcvHttpRequest(HttpRequestEventParam requestEventParam){
		final Channel channel = requestEventParam.channel();
		// 保存session - 保持一段时间的活性
		HttpSessionWrapper sessionWrapper = sessionWrapperMap.computeIfAbsent(channel, k -> new HttpSessionWrapper(new HttpSession(requestEventParam.logicWorldGuid(), channel)));
		sessionWrapper.setSessionTimeout(netConfigManager.httpSessionTimeout() + netTimeManager.getSystemSecTime());

		HttpSession httpSession = sessionWrapper.session;
		// 分发事件
		LogicWorldInNetWorldInfo logicWorldInfo = logicWorldManager.getLogicWorldInfo(requestEventParam.logicWorldGuid());
		if (null == logicWorldInfo) {
			// 不存在对应的logicWorld
			httpSession.writeAndFlush(HttpResponseHelper.newNotFoundResponse());
		} else {
			final String path = requestEventParam.getHttpRequestTO().getPath();
			final ConfigWrapper param = requestEventParam.getHttpRequestTO().getParams();
			EventLoopUtils.executeOrRun(logicWorldInfo.getEventLoop(), () -> {
				logicWorldInfo.getHttpDispatchManager().handleRequest(httpSession, path, param);
			});
		}
	}

	public void onLogicWorldShutdown(long logicWorldGuid) {
		// 如果logicWorld持有了httpSession的引用，长时间没有完成响应的话，这里关闭可能导致一些错误
		CollectionUtils.removeIfAndThen(sessionWrapperMap,
				(channel, httpSession) -> httpSession.session.getLogicWorldGuid() == logicWorldGuid,
				(channel, httpSession) -> NetUtils.closeQuietly(channel));
	}

	private static class HttpSessionWrapper {

		private final HttpSession session;
		/**
		 * 会话超时时间 - 避免对外，线程安全问题
		 */
		private int sessionTimeout;

		private HttpSessionWrapper(HttpSession session) {
			this.session = session;
		}

		public HttpSession getSession() {
			return session;
		}

		public int getSessionTimeout() {
			return sessionTimeout;
		}

		public void setSessionTimeout(int sessionTimeout) {
			this.sessionTimeout = sessionTimeout;
		}
	}
}
