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

/**
 * 会话生命周期观察者
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 11:48
 * github - https://github.com/hl845740757
 */
public interface SessionLifecycleAware<T extends Session> {

    /**
     * 当会话第一次成功建立时调用，表示会话正式可用，只会调用一次
     * 断线重连不会触发这里
     * @param session 注册时的会话信息
     */
    void onSessionConnected(T session);

    /**
     * 当会话彻底断开连接(无法继续断线重连)时会被调用，只会调用一次
     * 只有调用过{@link #onSessionConnected(Session)}方法，才会走到该方法
     * @param session 注册时的会话信息
     */
    void onSessionDisconnected(T session);
}
