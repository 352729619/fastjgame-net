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


import com.wjybxx.fastjgame.concurrent.event.Event;
import com.wjybxx.fastjgame.net.RoleType;

import javax.annotation.Nonnull;

/**
 * 游戏逻辑world，负责游戏业务逻辑处理
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/28
 * github - https://github.com/hl845740757
 */
public interface LogicWorld extends World{

    @Nonnull
    @Override
    LogicWorldConfig config();

    /**
     * world的全局唯一标识。
     *
     * @apiNote 必须实现为线程安全。
     * 建议在{@link GameEventLoopGroup#registerWorld(LogicWorld)}之前初始化，使得对应的属性不必同步。
     *
     * @return Globally Unique Identifier
     */
    long worldGuid();

    /**
     * world的类型。
     * @apiNote 必须实现为线程安全。
     * 建议在{@link GameEventLoopGroup#registerWorld(LogicWorld)}之前初始化，使得对应的属性不必同步。
     *
     * @return roleType
     */
    @Nonnull
    RoleType worldType();

    /**
     * 游戏世界收到一个事件，一般表示一个请求或响应。
     * @param event 事件内容
     * @throws Exception errors
     */
    void onEvent(Event event) throws Exception;

}
