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
     * 测试用
     */
    TEST(0),
    /**
     * 网关服(不使用)
     */
    GATE(1),
    /**
     * 登录服(login server)
     */
    LOGIN(2),
    /**
     * 中心服务器
     */
    CENTER(3),
    /**
     * 场景服
     */
    SCENE(4),
    /**
     * 战区服
     */
    WARZONE(5),
    /**
     * 玩家
     */
    PLAYER(6),
    /**
     * GM后台
     */
    GM(7),
    /**
     * NetWorld
     */
    NET(8)
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
