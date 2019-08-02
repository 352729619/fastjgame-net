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

package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * 网络事件类型
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
public enum NetEventType implements NumberEnum {

	// -----------------------客户端(连接的发起方)请求与远程(服务端)建立连接------------------
	/**
	 * 客户端请求建立链接(验证TOKEN)
	 */
	CONNECT_REQUEST(1),
	/**
	 * 服务器通知建立链接结果(TOKEN验证结果)
	 */
	CONNECT_RESPONSE(2),

	// ------------------------- 连接的客户端方发起的Rpc ------------------------------
	/**
	 * Rpc请求包，必须有一个响应。
	 */
	C2S_RPC_REQUEST(3),
	/**
	 * Rpc响应包。
	 */
	C2S_RPC_RESPONSE(4),

	// ------------------- 连接的服务端方发起的Rpc (Rpc允许服务器之间双向调用) ----------------------
	/**
	 * Rpc请求包，必须有一个响应。
	 */
	S2C_RPC_REQUEST(5),
	/**
	 * Rpc响应包。
	 */
	S2C_RPC_RESPONSE(6),

	// ------------------------- 服务器与服务器、服务器与玩家之间的单向调用 -----------------------
	/**
	 * 作为连接的客户端方发来的单向消息。 --- 支持自定义编码格式
	 */
	C2S_ONE_WAY_MESSAGE(7),
	/**
	 * 作为连接的服务器方发来的单向消息。 --- 支持自定义编码格式
	 */
	S2C_ONE_WAY_MESSAGE(8),

	// -------------------------------- 连接之间的心跳 ------------------------------------
	/**
	 * 客户端发送给服务器的ack-ping包(必定会返回一条ack-pong消息)
	 */
	ACK_PING(9),
	/**
	 * 服务器返回给客户端的ack-pong包，对ack-ping包的响应
	 */
	ACK_PONG(10),

	/**
	 * http请求事件
	 */
	HTTP_REQUEST(11),

	;

	/** 枚举对应的唯一数字 */
	private final int number;

	NetEventType(int number) {
		this.number = number;
	}

	@Override
	public int getNumber() {
		return number;
	}

	/**
	 * 排序号的枚举数组，方便查找
	 */
	private static final NumberEnumMapper<NetEventType> mapper = EnumUtils.indexNumberEnum(values());

	/**
	 * 通过网络包中的pkgType找到对应的枚举。
	 * @param number 事件对于的数字
	 * @return 事件类型
	 */
	public static NetEventType forNumber(int number){
		return mapper.forNumber(number);
	}
}
