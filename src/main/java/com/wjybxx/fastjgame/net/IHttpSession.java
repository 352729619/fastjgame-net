package com.wjybxx.fastjgame.net;

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
	void close();

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
