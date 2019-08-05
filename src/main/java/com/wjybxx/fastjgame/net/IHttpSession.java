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

import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpResponseBuilder;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpResponse;

import javax.annotation.Nonnull;

/**
 * HttpSession抽象接口
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface IHttpSession {

	/**
	 * 监听端口的本地对象的guid
	 */
	long localGuid();

	/**
	 * 监听端口的本地对象的类型
	 */
	RoleType localRole();

	/**
	 * 绑定的本地端口
	 */
	@Nonnull
	HostAndPort localAddress();

	/**
	 * 关闭session。
	 * 子类实现必须线程安全。
	 */
	ListenableFuture<?> close();

	/**
	 * 发送一个响应
	 * @param response 响应内容
	 * @return 注意相同的警告，建议使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()}
	 * 代替{@link ChannelFuture#sync()}
	 */
	ChannelFuture writeAndFlush(HttpResponse response);

	/**
	 * 发送一个http结果对象
	 * @param <T> builder自身
	 * @param builder 建造者
	 * @return  注意相同的警告，建议使用{@link ChannelFuture#await()} 和{@link ChannelFuture#isSuccess()}
	 * 			代替{@link ChannelFuture#sync()}
	 */
	<T extends HttpResponseBuilder<T>> ChannelFuture writeAndFlush(HttpResponseBuilder<T> builder);
}
