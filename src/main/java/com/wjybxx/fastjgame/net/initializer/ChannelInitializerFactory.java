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

package com.wjybxx.fastjgame.net.initializer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import javax.annotation.Nonnull;

/**
 * ChannelInitializer工厂
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/1
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface ChannelInitializerFactory extends ChannelInitializerSupplier{

	@Override
	default ChannelInitializer<SocketChannel> get() {
		return newInitializer();
	}

	/**
	 * 返回一个新的initializer用于初始化channel
	 */
	@Nonnull
	ChannelInitializer<SocketChannel> newInitializer();
}
