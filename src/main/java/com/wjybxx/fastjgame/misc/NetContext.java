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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.manager.NetEventManager;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

/**
 * 逻辑层使用的网络上下文
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface NetContext {

	// --- 注册到网络模块时的信息 ---

	/**
	 * 注册的本地guid
	 */
	long localGuid();

	/**
	 * 注册的本地角色
	 */
	RoleType localRole();

	/**
	 * 本地角色的运行环境，用于实现线程安全，
	 * 可以保证{@link MessageHandler}运行在该{@link EventLoop}上。
	 * (也就是说所有的网络事件处理最终都会运行在该EventLoop上)
	 */
	EventLoop localEventLoop();

	/**
	 * 该context绑定到的NetEventLoop。
	 * （可实现多个NetContext绑定到相同的NetEventLoop，可消除不必要的同步）
	 */
	NetEventLoop netEventLoop();

	/**
	 * 获取网络事件管理器组件。
	 * 创建{@link ChannelInitializerSupplier}需要使用。
	 */
	NetEventManager netEventManager();

	/**
	 * 从注册的NetEventLoop上取消注册，会关闭该context关联的所有会话。
	 */
	ListenableFuture<?> unregister();

	// ----------------------------------- tcp/ws支持 ---------------------------------------

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param port 指定端口号
	 * @param initializer 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(boolean outer, int port, ChannelInitializer<SocketChannel> initializer,
											   SessionLifecycleAware<S2CSession> lifecycleAware,
											   MessageHandler messageHandler) {
		return this.bindRange(outer, new PortRange(port, port), initializer, lifecycleAware, messageHandler);
	}

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param portRange 端口范围
	 * @param initializer 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializer<SocketChannel> initializer,
											SessionLifecycleAware<S2CSession> lifecycleAware,
											MessageHandler messageHandler);

	/**
	 * 监听某个端口
	 * @param remoteGuid 远程角色guid
	 * @param remoteRole 远程角色类型
	 * @param remoteAddress 远程地址
	 * @param initializerSupplier 如何初始化channel，supplier是因为断线重连可能需要新的initializer。
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future，future 可以等待连接完成。
	 */
	ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole, HostAndPort remoteAddress, ChannelInitializerSupplier initializerSupplier,
								SessionLifecycleAware<C2SSession> lifecycleAware,
								MessageHandler messageHandler);

	//  --------------------------------------- http支持 -----------------------------------------

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param port 指定端口号
	 * @param initializer 如何初始化channel
	 * @param httpRequestHandler http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(boolean outer, int port, ChannelInitializer<SocketChannel> initializer,
							 HttpRequestHandler httpRequestHandler) {
		return this.bindRange(outer, new PortRange(port, port), initializer, httpRequestHandler);
	}

	/**
	 * 在指定端口范围内监听某一个端口。
	 *
	 * @param outer 是否是外网断开
	 * @param portRange 端口范围
	 * @param initializer 如何初始化channel
	 * @param httpRequestHandler http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange, ChannelInitializer<SocketChannel> initializer,
											HttpRequestHandler httpRequestHandler);

	/**
	 * 同步get请求
	 * @param url url
	 * @param params get参数
	 */
	Response syncGet(String url, Map<String,String> params) throws IOException;
	/**
	 * 异步get请求
	 * @param url url
	 * @param params get参数
	 * @param okHttpCallback 回调
	 */
	void asyncGet(String url, Map<String,String> params, OkHttpCallback okHttpCallback);

	/**
	 * 同步post请求
	 * @param url url
	 * @param params post参数
	 */
	Response syncPost(String url, Map<String,String> params) throws IOException;

	/**
	 * 异步post请求
	 * @param url url
	 * @param params post参数
	 * @param okHttpCallback 回调
	 */
	void asyncPost(String url, Map<String,String> params, OkHttpCallback okHttpCallback);
}
