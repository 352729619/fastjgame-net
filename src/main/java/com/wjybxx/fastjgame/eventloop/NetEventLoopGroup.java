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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.NetContext;
import com.wjybxx.fastjgame.net.RoleType;

import javax.annotation.Nonnull;

/**
 * 网络事件循环组
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
	 * 当用户不再使用NetEventLoop时，避免内存泄漏，必须调用
	 * {@link NetContext#deregister()} 或 {@link NetEventLoop#deregisterContext(long)}取消注册。
	 *
	 * 注意：一个localGuid表示一个用户，在同一个NetEventLoop下只能创建一个Context，必须在取消注册成功之后才能再次注册。
	 *
	 * @param localGuid context绑定到的角色guid
	 * @param localRole context绑定到的角色类型
	 * @param localEventLoop 方法的调用者所在的eventLoop
	 * @return NetContext
	 */
	ListenableFuture<NetContext> createContext(long localGuid, RoleType localRole, EventLoop localEventLoop);

}
