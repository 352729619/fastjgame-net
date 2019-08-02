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

import com.wjybxx.fastjgame.misc.HostAndPort;

/**
 * 客户端到服务器的会话信息
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:00
 * github - https://github.com/hl845740757
 */
public class C2SSession implements Session {

    /**
     * 服务器唯一标识(会话id)
     */
    private final long serverGuid;
    /**
     * 服务器类型
     */
    private final RoleType serverType;
    /**
     * 服务器地址
     */
    private final HostAndPort hostAndPort;

    public C2SSession(long serverGuid, RoleType serverType, HostAndPort hostAndPort) {
        this.serverGuid = serverGuid;
        this.serverType = serverType;
        this.hostAndPort = hostAndPort;
    }

    @Override
    public long remoteGuid() {
        return serverGuid;
    }

    @Override
    public RoleType remoteRole() {
        return serverType;
    }

    public long getServerGuid() {
        return serverGuid;
    }

    public RoleType getServerType() {
        return serverType;
    }

    public HostAndPort getHostAndPort() {
        return hostAndPort;
    }

    @Override
    public String toString() {
        return "C2SSession{" +
                "serverGuid=" + serverGuid +
                ", serverType=" + serverType +
                ", hostAndPort=" + hostAndPort +
                '}';
    }
}
