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
 * 服务器存储的与客户端建立的会话信息。
 * 只暴露一部分关键信息。
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:18
 * github - https://github.com/hl845740757
 */
public class S2CSession implements Session {

    /** 会话关联的本地角色guid */
    private final long localGuid;
    /** 会话关联的本地角色类型 */
    private final RoleType localRole;

    /**
     * 客户端唯一id，也就是sessionId
     */
    private final long clientGuid;
    /**
     * 客户端类型
     */
    private final RoleType clientType;

    public S2CSession(long localGuid, RoleType localRole, long clientGuid, RoleType clientType) {
        this.localGuid = localGuid;
        this.localRole = localRole;
        this.clientGuid = clientGuid;
        this.clientType = clientType;
    }

    @Override
    public long localGuid() {
        return localGuid;
    }

    @Override
    public RoleType localRole() {
        return localRole;
    }

    @Override
    public long remoteGuid() {
        return clientGuid;
    }

    @Override
    public RoleType remoteRole() {
        return clientType;
    }

    public long getClientGuid() {
        return clientGuid;
    }

    public RoleType getClientType() {
        return clientType;
    }

    @Override
    public String toString() {
        return "S2CSession{" +
                "clientGuid=" + clientGuid +
                ", clientType=" + clientType +
                '}';
    }

}
