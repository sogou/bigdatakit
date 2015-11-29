package com.sogou.bigdatakit.common.thread

import java.util.concurrent.Callable

/**
 * Created by Tao Li on 1/9/15.
 */
trait CallableWithKillable[V] extends Callable[V] with Killable