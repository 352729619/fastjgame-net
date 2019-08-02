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
 * 网络包类型 -- 7种
 * (严格来说还有一个：http网络包，不过由于不是自定义格式，因此不在这里)
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/24
 * github - https://github.com/hl845740757
 */
public enum NetPackageType implements NumberEnum {

	/**
	 * 客户端请求建立链接(验证TOKEN)
	 */
	CONNECT_REQUEST((byte) 1),
	/**
	 * 服务器通知建立链接结果(TOKEN验证结果)
	 */
	CONNECT_RESPONSE((byte)2),

	/**
	 * Rpc请求包，必须有一个响应。 -- Rpc消息使用protoBuf编解码，内部使用。
	 */

	RPC_REQUEST((byte)3),
	/**
	 * Rpc响应包。
	 */
	RPC_RESPONSE((byte)4),

	/**
	 * 单向消息包，另一方收到消息不一定有响应消息。 --- 支持自定义编码格式
	 */
	ONE_WAY_MESSAGE((byte)5),

	/**
	 * 心跳包 -- 客户端发起
	 */
	ACK_PING((byte)6),
	/**
	 * 心跳包 -- 服务器响应
	 */
	ACK_PONG((byte)7),

	;

	public final byte pkgType;

	NetPackageType(byte pkgType) {
		this.pkgType = pkgType;
	}

	/**
	 * 仅仅用于初始化映射
	 * @return 枚举对应的编号
	 */
	@Deprecated
	@Override
	public int getNumber() {
		return pkgType;
	}

	/**
	 * 排序号的枚举数组，方便查找
	 */
	private static final NumberEnumMapper<NetPackageType> mapper = EnumUtils.indexNumberEnum(values());

	/**
	 * 通过网络包中的pkgType找到对应的枚举。
	 * @param pkgType 包类型
	 * @return 包类型对应的枚举
	 */
	public static NetPackageType forNumber(byte pkgType){
		return mapper.forNumber(pkgType);
	}
}
