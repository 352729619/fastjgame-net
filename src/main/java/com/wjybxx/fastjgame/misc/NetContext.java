package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.eventloop.NetEventLoop;
import com.wjybxx.fastjgame.net.*;
import com.wjybxx.fastjgame.net.initializer.ChannelInitializerSupplier;

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
	 * 从注册的NetEventLoop上取消注册，会关闭该context关联的所有会话。
	 */
	void unregister();


	// tcp/ws支持

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param port 指定端口号
	 * @param initializerSupplier 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(boolean outer, int port,
									   ChannelInitializerSupplier initializerSupplier,
									   SessionLifecycleAware<S2CSession> lifecycleAware,
									   MessageHandler messageHandler) {
		return this.bindRange(outer, new PortRange(port, port), initializerSupplier, lifecycleAware, messageHandler);
	}

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param portRange 端口范围
	 * @param initializerSupplier 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange,
											ChannelInitializerSupplier initializerSupplier,
											SessionLifecycleAware<S2CSession> lifecycleAware,
											MessageHandler messageHandler);

	/**
	 * 监听某个端口
	 * @param remoteGuid 远程角色guid
	 * @param remoteRole 远程角色类型
	 * @param remoteAddress 远程地址
	 * @param initializerSupplier 如何初始化channel
	 * @param lifecycleAware 生命周期监听器
	 * @param messageHandler 消息处理器
	 * @return future，future 可以等待连接完成。
	 */
	ListenableFuture<?> connect(long remoteGuid, RoleType remoteRole,
										  HostAndPort remoteAddress,
										  ChannelInitializerSupplier initializerSupplier,
										  SessionLifecycleAware<C2SSession> lifecycleAware,
										  MessageHandler messageHandler);

	// http

	/**
	 * 监听某个端口
	 * @param outer 是否是外网断开
	 * @param port 指定端口号
	 * @param initializerSupplier 如何初始化channel
	 * @param httpRequestHandler http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	default ListenableFuture<HostAndPort> bind(boolean outer, int port,
							 ChannelInitializerSupplier initializerSupplier,
							 HttpRequestHandler httpRequestHandler) {
		return this.bindRange(outer, new PortRange(port, port), initializerSupplier, httpRequestHandler);
	}

	/**
	 * 在指定端口范围内监听某一个端口。
	 *
	 * @param outer 是否是外网断开
	 * @param portRange 端口范围
	 * @param initializerSupplier 如何初始化channel
	 * @param httpRequestHandler http请求处理器
	 * @return future 可以等待绑定完成。
	 */
	ListenableFuture<HostAndPort> bindRange(boolean outer, PortRange portRange,
								  ChannelInitializerSupplier initializerSupplier,
								  HttpRequestHandler httpRequestHandler);
}
