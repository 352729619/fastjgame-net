package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.net.RoleType;

import javax.annotation.Nonnull;

/**
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface NetEventLoopGroup extends EventLoopGroup {


	@Nonnull
	@Override
	NetEventLoop next();

	/**
	 * 注册一个NetEventLoop的用户。
	 * 当用户不再使用NetEventLoop时，避免内存泄漏，必须调用{@link NetContext#unregister()}取消注册。
	 * 注意：一个localGuid表示一个用户，只能创建一个oontext，必须在取消注册成功之后才能再次注册。
	 *
	 * @param localGuid context绑定到的角色guid
	 * @param localRole context绑定到的角色类型
	 * @param localEventLoop 方法的调用者所在的eventLoop
	 * @return NetContext
	 */
	ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, EventLoop localEventLoop);

}
