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

package com.wjybxx.fastjgame.world;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.manager.*;
import com.wjybxx.fastjgame.manager.networld.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 游戏世界顶层类(World)
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 23:00
 * github - https://github.com/hl845740757
 */
public class NetWorldImp extends AbstractWorld implements NetWorld{

    private static final Logger logger = LoggerFactory.getLogger(World.class);

    private final NetEventManager netEventManager;
    private final S2CSessionManager s2CSessionManager;
    private final C2SSessionManager c2SSessionManager;
    private final NetConfigManager netConfigManager;
    private final TokenManager tokenManager;
    private final HttpClientManager httpClientManager;
    private final GlobalExecutorManager globalExecutorManager;
    private final NettyThreadManager nettyThreadManager;
    private final CuratorClientManager curatorClientManager;
    private final LogicWorldManager logicWorldManager;
    private final HttpSessionManager httpSessionManager;

    @Inject
    public NetWorldImp(GameEventLoopManager gameEventLoopManager, NetTimeManager netTimeManager, NetTriggerManager netTriggerManager,
                       NetEventManager netEventManager, S2CSessionManager s2CSessionManager, C2SSessionManager c2SSessionManager,
                       NetConfigManager netConfigManager, TokenManager tokenManager, HttpClientManager httpClientManager,
                       GlobalExecutorManager globalExecutorManager, NettyThreadManager nettyThreadManager, CuratorClientManager curatorClientManager, LogicWorldManager logicWorldManager, HttpSessionManager httpSessionManager) {
        super(gameEventLoopManager, netTimeManager, netTriggerManager);
        this.netEventManager = netEventManager;
        this.s2CSessionManager = s2CSessionManager;
        this.c2SSessionManager = c2SSessionManager;
        this.netConfigManager = netConfigManager;
        this.tokenManager = tokenManager;
        this.httpClientManager = httpClientManager;
        this.globalExecutorManager = globalExecutorManager;
        this.nettyThreadManager = nettyThreadManager;
        this.curatorClientManager = curatorClientManager;
        this.logicWorldManager = logicWorldManager;
        this.httpSessionManager = httpSessionManager;

        // 处理环形依赖
        logicWorldManager.handleCycleDependency(gameEventLoopManager, c2SSessionManager,
                s2CSessionManager, httpSessionManager);
    }

    @Nonnull
    @Override
    public NetWorldConfig config() {
        return netConfigManager;
    }

    @Override
    protected void startImp() throws Exception {
        // 启动netty线程
        nettyThreadManager.start();
        // 启动全局服务
        httpClientManager.start();
        curatorClientManager.start();
        globalExecutorManager.start();
    }

    @Override
    protected void shutdownImp() throws Exception {
        // 关闭netty
        ConcurrentUtils.safeExecute((Runnable) nettyThreadManager::shutdown);
        // 关闭全局服务
        ConcurrentUtils.safeExecute((Runnable) httpClientManager::shutdown);
        ConcurrentUtils.safeExecute((Runnable) curatorClientManager::shutdown);
        ConcurrentUtils.safeExecute((Runnable) globalExecutorManager::shutdown);
    }

    /**
     * 游戏世界刷帧
     * @param curMillTime 当前时间戳
     */
    public final void tickImp(long curMillTime){
        c2SSessionManager.tick();
    }
}
