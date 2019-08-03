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
 * 会话信息，用于获取远程的基本信息，子类可提供更为全面的信息。
 *
 * Q:为何抽象层没有提供address之类的信息？
 * A:因为底层会自动处理断线重连等等，这些信息可能会变化，暂时不提供。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/29
 * github - https://github.com/hl845740757
 */
public interface Session {

    /**
     * 会话关联的本地对象guid
     */
    long localGuid();

    /**
     * 会话管理的本地角色类型
     */
    RoleType localRole();

    /**
     * 远程的guid
     */
    long remoteGuid();

    /**
     * 远程的角色类型
     */
    RoleType remoteRole();
}
