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

package com.wjybxx.fastjgame.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;
import com.wjybxx.fastjgame.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 网络事件管理器。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetEventManager {

	private static final Logger logger = LoggerFactory.getLogger(NetEventManager.class);

	private final S2CSessionManager s2CSessionManager;
	private final C2SSessionManager c2SSessionManager;
	private final HttpSessionManager httpSessionManager;
	private final NetEventLoopManager netEventLoopManager;

	@Inject
	public NetEventManager(S2CSessionManager s2CSessionManager, C2SSessionManager c2SSessionManager,
						   HttpSessionManager httpSessionManager, NetEventLoopManager netEventLoopManager) {
		this.s2CSessionManager = s2CSessionManager;
		this.c2SSessionManager = c2SSessionManager;
		this.httpSessionManager = httpSessionManager;
		this.netEventLoopManager = netEventLoopManager;
	}

	/**
	 * 发布一个网络事件
	 * @param netEventType 事件类型
	 * @param eventParam 事件参数。类型决定参数
	 */
	public void publishEvent(NetEventType netEventType, NetEventParam eventParam){
		// 一定不在NetEventLoop中，提交的netEventLoop线程
		netEventLoopManager.eventLoop().execute(() -> {
			onNetEvent(netEventType, eventParam);
		});
	}

	/**
	 * 网络事件
	 */
	private void onNetEvent(NetEventType eventType, NetEventParam eventParam){
		switch (eventType){
			// connect request response
			case CONNECT_REQUEST:
				s2CSessionManager.onRcvConnectRequest((ConnectRequestEventParam)eventParam);
				break;
			case CONNECT_RESPONSE:
				c2SSessionManager.onRcvConnectResponse((ConnectResponseEventParam) eventParam);
				break;

			// ping-pong message
			case ACK_PING:
				s2CSessionManager.onRcvClientAckPing((AckPingPongEventParam) eventParam);
				break;
			case ACK_PONG:
				c2SSessionManager.onRevServerAckPong((AckPingPongEventParam) eventParam);
				break;

			// 连接的客户端方发起的rpc
			case C2S_RPC_REQUEST:
				s2CSessionManager.onRcvClientRpcRequest((RpcRequestEventParam) eventParam);
				break;
			case C2S_RPC_RESPONSE:
				c2SSessionManager.onRcvServerRpcResponse((RpcResponseEventParam) eventParam);
				break;

			// 连接的服务端发起的rpc
			case S2C_RPC_REQUEST:
				c2SSessionManager.onRcvServerRpcRequest((RpcRequestEventParam) eventParam);
				break;
			case S2C_RPC_RESPONSE:
				s2CSessionManager.onRcvClientRpcResponce((RpcResponseEventParam) eventParam);
				break;

			// 连接双方的单向消息
			case C2S_ONE_WAY_MESSAGE:
				s2CSessionManager.onRcvClientOneWayMsg((OneWayMessageEventParam) eventParam);
				break;
			case S2C_ONE_WAY_MESSAGE:
				c2SSessionManager.onRevServerOneWayMsg((OneWayMessageEventParam) eventParam);
				break;

			// http request
			case HTTP_REQUEST:
				httpSessionManager.onRcvHttpRequest((HttpRequestEventParam) eventParam);
				break;
			default:
				throw new IllegalArgumentException("unexpected event type " + eventType);
		}
	}
}
