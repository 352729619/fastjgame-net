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

package com.wjybxx.fastjgame.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.wjybxx.fastjgame.eventloop.NetEventLoopManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.manager.*;

/**
 * 网络模块需要的所有类
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/2
 * github - https://github.com/hl845740757
 */
public class NetModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(NetEventLoopManager.class).in(Singleton.class);
		bind(NetConfigManager.class).in(Singleton.class);

		bind(C2SSessionManager.class).in(Singleton.class);
		bind(S2CSessionManager.class).in(Singleton.class);
		bind(HttpSessionManager.class).in(Singleton.class);
		bind(NetEventManager.class).in(Singleton.class);

		bind(NettyThreadManager.class).in(Singleton.class);
		bind(HttpClientManager.class).in(Singleton.class);

		bind(AcceptManager.class).in(Singleton.class);
		bind(TokenManager.class).in(Singleton.class);

		bind(NetTimeManager.class).in(Singleton.class);
		bind(NetTimerManager.class).in(Singleton.class);

		bind(NetManagerWrapper.class).in(Singleton.class);
	}
}
