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

import com.wjybxx.fastjgame.enummapper.NumberEnum;
import com.wjybxx.fastjgame.enummapper.NumberEnumMapper;
import com.wjybxx.fastjgame.utils.EnumUtils;

/**
 * 用于标识会话对方的角色类型
 * @author wjybxx
 * @version 1.0
 * date - 2019/4/27 10:01
 * github - https://github.com/hl845740757
 */
public enum RoleType implements NumberEnum {
    /**
     * 无效的
     */
    INVALID(-1),
    /**
     * 网关服(不使用)
     */
    GATE(0),
    /**
     * 登录服(login server)
     */
    LOGIN(1),
    /**
     * 中心服务器
     */
    CENTER(2),
    /**
     * 场景服
     */
    SCENE(3),
    /**
     * 战区服
     */
    WARZONE(4),
    /**
     * 玩家
     */
    PLAYER(5),
    /**
     * GM后台
     */
    GM(6),
    /**
     * 数据库服务器 (扩展用)
     */
    DB(7),
    /**
     * 场景node服务器 (扩展用)
     */
    NODE(8),

    /**
     * 测试用客户端角色
     */
    TEST_CLIENT(9),

    /**
     * 测试用服务器角色
     */
    TEST_SERVER(10),
    ;

    /**
     * 角色编号
     */
    public final int number;

    RoleType(int roleType) {
        this.number = roleType;
    }

    private static final NumberEnumMapper<RoleType> mapper = EnumUtils.indexNumberEnum(values());

    public static RoleType forNumber(int number){
        RoleType roleType = mapper.forNumber(number);
        assert null!=roleType:"invalid number " + number;
        return roleType;
    }

    @Override
    public int getNumber() {
        return number;
    }
}
