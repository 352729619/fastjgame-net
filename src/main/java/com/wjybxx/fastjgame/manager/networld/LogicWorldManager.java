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

package com.wjybxx.fastjgame.manager.networld;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.manager.GameEventLoopManager;
import com.wjybxx.fastjgame.manager.logicworld.HttpDispatchManager;
import com.wjybxx.fastjgame.manager.logicworld.MessageDispatchManager;
import com.wjybxx.fastjgame.misc.LogicWorldInNetWorldInfo;
import com.wjybxx.fastjgame.world.LogicWorld;
import com.wjybxx.fastjgame.world.NetWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * 用于{@link NetWorld}管理注册到它上面的{@link LogicWorld}。
 * LogicWorld在使用NetWorld之前必须先进行注册
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/31
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class LogicWorldManager {

    private static final Logger logger = LoggerFactory.getLogger(LogicWorldManager.class);

    private final Long2ObjectMap<LogicWorldInNetWorldInfo> logicWorldMap = new Long2ObjectOpenHashMap<>();

    private GameEventLoopManager gameEventLoopManager;
    private C2SSessionManager c2SSessionManager;
    private S2CSessionManager s2CSessionManager;
    private HttpSessionManager httpSessionManager;

    @Inject
    public LogicWorldManager() {

    }

    public void handleCycleDependency(GameEventLoopManager gameEventLoopManager, C2SSessionManager c2SSessionManager,
                             S2CSessionManager s2CSessionManager, HttpSessionManager httpSessionManager) {
        this.gameEventLoopManager = gameEventLoopManager;
        this.c2SSessionManager = c2SSessionManager;
        this.s2CSessionManager = s2CSessionManager;
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * logicWorld将自身注册到NetWorld上。
     *
     * @param logicWorld 逻辑服务器
     * @return 建议在future上阻塞直到完成，会更加安全，而且该方法并不是一个频繁调用的方法
     */
    public ListenableFuture<?> registerLogicWorld(LogicWorld logicWorld, HttpDispatchManager httpDispatchManager, MessageDispatchManager messageDispatchManager) {
        // 存为临时变量，避免terminalFuture捕获logicWorld对象
        final long logicWorldGuid = logicWorld.worldGuid();
        // 监听logicWorld关闭事件
        logicWorld.terminateFuture().addListener(future -> {
            removeLogicWorld(logicWorldGuid);
        }, gameEventLoopManager.eventLoop());

        if (gameEventLoopManager.inEventLoop()) {
            // 和netWorld在同一个线程，直接添加
            registerLogicWorldInternal(logicWorld, httpDispatchManager, messageDispatchManager);
            return gameEventLoopManager.eventLoop().newSucceededFuture(null);
        } else {
            return gameEventLoopManager.eventLoop().submit(() -> {
                registerLogicWorldInternal(logicWorld, httpDispatchManager, messageDispatchManager);
            });
        }
    }

    // ---------------------------------------------- 内部实现 --------------------------------------
    /**
     * 注册一个logicWorld到NetWorld中
     * @param logicWorld 逻辑服务器
     */
    private void registerLogicWorldInternal(LogicWorld logicWorld, HttpDispatchManager httpDispatchManager, MessageDispatchManager messageDispatchManager) {
        if (logicWorldMap.containsKey(logicWorld.worldGuid())) {
            throw new IllegalStateException("LogicWorld " + logicWorld.worldType() + "-" + logicWorld.worldGuid() + " already registered.");
        }

        LogicWorldInNetWorldInfo logicWorldInNetWorldInfo = new LogicWorldInNetWorldInfo(logicWorld.worldGuid(),
                logicWorld.worldType(), logicWorld.eventLoop(), httpDispatchManager, messageDispatchManager);
        logicWorldMap.put(logicWorld.worldGuid(), logicWorldInNetWorldInfo);
        // TODO 是否需要logicWorld注册事件？
    }

    /**
     * 移除一个逻辑服务器
     * @param logicWorldGuid 逻辑服务器的标识
     */
    private void removeLogicWorld(long logicWorldGuid) {
        LogicWorldInNetWorldInfo logicWorld = logicWorldMap.remove(logicWorldGuid);
        if (null == logicWorld) {
            return;
        }
        s2CSessionManager.onLogicWorldShutdown(logicWorldGuid);
        c2SSessionManager.onLogicWorldShutdown(logicWorldGuid);
        httpSessionManager.onLogicWorldShutdown(logicWorldGuid);
        // TODO 取消注册事件，进行必要的清理
    }

    // --------------------------------------------- 只允许netWorld特定管理器调用的方法  ----------------------
    /**
     * 获取逻辑服的信息。
     * (不允许外部调用，仅仅允许NetWorld的部分管理器调用)
     * @param logicWorldGuid 逻辑服务器的标识
     * @return LogicWorldInNetWorldInfo
     */
    @Nullable
    LogicWorldInNetWorldInfo getLogicWorldInfo(long logicWorldGuid) {
        return logicWorldMap.get(logicWorldGuid);
    }

    /**
     * 确保逻辑服务器已注册
     * @param logicWorldGuid 逻辑服务器的标识
     */
    void ensureRegistered(long logicWorldGuid) {
        if (!logicWorldMap.containsKey(logicWorldGuid)){
            throw new IllegalArgumentException("logicWorld " + logicWorldGuid  + " is not registered.");
        }
    }
}
