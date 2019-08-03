package com.wjybxx.fastjgame.net;

import com.wjybxx.fastjgame.misc.HostAndPort;

/**
 * 连接远程创建的session
 *
 * @author houlei
 * @version 1.0
 * date - 2019/8/3
 */
public interface IC2SSession extends Session{

	/**
	 * 连接的远程端口
	 */
	HostAndPort remoteAddress();
}
