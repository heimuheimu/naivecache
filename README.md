# NaiveCache: 简单易用的 Memcached Java 客户端。

## 使用要求
* JDK 版本：1.8+ 
* 依赖类库：
  * [slf4j-log4j12 1.7.5+](https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12)
  * [naivemonitor 1.0+](https://github.com/heimuheimu/naivemonitor)
  * [compress-lzf 1.0.3+](https://github.com/ning/compress)

## Maven 配置
```xml
    <dependency>
        <groupId>com.heimuheimu</groupId>
        <artifactId>naivecache</artifactId>
        <version>1.0</version>
    </dependency>
```
## Log4J 配置
```
log4j.logger.com.heimuheimu.naivecache=WARN, NAIVECACHE
log4j.additivity.com.heimuheimu.naivecache=false
log4j.appender.NAIVECACHE=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECACHE.file=${log.output.directory}/naivecache/naivecache.log
log4j.appender.NAIVECACHE.encoding=UTF-8
log4j.appender.NAIVECACHE.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECACHE.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECACHE.layout.ConversionPattern=%d{ISO8601} %-5p [%F:%L] : %m%n

# Memcached 连接信息日志
log4j.logger.NAIVECACHE_MEMCACHED_CONNECTION_LOG=INFO, NAIVECACHE_MEMCACHED_CONNECTION_LOG
log4j.additivity.NAIVECACHE_MEMCACHED_CONNECTION_LOG=false
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG.file=${log.output.directory}/naivecache/connection.log
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG.encoding=UTF-8
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECACHE_MEMCACHED_CONNECTION_LOG.layout.ConversionPattern=%d{ISO8601} %-5p : %m%n

# Memcached 错误日志，只打印 Key 和 错误原因
log4j.logger.NAIVECACHE_ERROR_LOG=INFO, NAIVECACHE_ERROR_LOG
log4j.additivity.NAIVECACHE_ERROR_LOG=false
log4j.appender.NAIVECACHE_ERROR_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECACHE_ERROR_LOG.file=${log.output.directory}/naivecache/error.log
log4j.appender.NAIVECACHE_ERROR_LOG.encoding=UTF-8
log4j.appender.NAIVECACHE_ERROR_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECACHE_ERROR_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECACHE_ERROR_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n

# Memcached 慢查日志，打印执行时间 > 50ms 的操作
log4j.logger.NAIVECACHE_SLOW_EXECUTION_LOG=INFO, NAIVECACHE_SLOW_EXECUTION_LOG
log4j.additivity.NAIVECACHE_SLOW_EXECUTION_LOG=false
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.file=${log.output.directory}/naivecache/slow_execution.log
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.encoding=UTF-8
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout
log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n
```

## Memcached 客户端

### Spring 配置
```xml
    <!-- Memcached 集群客户端事件监听器配置，在 Memcached 服务不可用时进行实时通知-->
    <bean id="memcachedClusterClientListener" class="com.heimuheimu.naivecache.memcached.listener.NoticeableMemcachedClusterClientListener">
        <constructor-arg index="0" value="your-project-name" /> <!-- 当前项目名称 -->
        <constructor-arg index="1" ref="notifierList"/> <!-- 报警器列表，报警器的信息可查看 naivemonitor 项目 -->
    </bean>
    
    <!-- Memcached 集群客户端配置 -->
    <bean id="memcachedClusterClient" class="com.heimuheimu.naivecache.memcached.cluster.MemcachedClusterClient" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:11211,127.0.0.1:11212,127.0.0.1:11213" /> <!-- Memcached 服务地址列表，使用 "," 分割 -->
        <constructor-arg index="1"><null/></constructor-arg> <!-- Socket 配置，允许为 null -->
        <constructor-arg index="2" value="1000" /> <!-- Memcached 操作超时时间设置，单位：毫秒，默认为 1 秒 -->
        <constructor-arg index="3" value="65536" /> <!-- 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，默认为 64KB -->
        <constructor-arg index="4">
            <bean class="com.heimuheimu.naivecache.memcached.listener.SimpleNaiveMemcachedClientListener" />
        </constructor-arg>
        <constructor-arg index="5" ref="memcachedClusterClientListener"/>
    </bean>
```

### Falcon 监控数据上报 Spring 配置
```xml
    <!-- 监控数据采集器列表 -->
    <util:list id="falconDataCollectorList">
        <!-- Memcached 监控数据采集器 -->
        <bean class="com.heimuheimu.naivecache.memcached.monitor.falcon.CompressionDataCollector"></bean>
        <bean class="com.heimuheimu.naivecache.memcached.monitor.falcon.SocketDataCollector"></bean>
        <bean class="com.heimuheimu.naivecache.memcached.monitor.falcon.ThreadPoolDataCollector"></bean>
        <bean class="com.heimuheimu.naivecache.memcached.monitor.falcon.ExecutionDataCollector"></bean>
    </util:list>
    
    <!-- Falcon 监控数据上报器 -->
    <bean id="falconReporter" class="com.heimuheimu.naivemonitor.falcon.FalconReporter" init-method="init" destroy-method="close">
        <constructor-arg index="0" value="http://127.0.0.1:1988/v1/push" /> <!-- Falcon 监控数据推送地址 -->
        <constructor-arg index="1" ref="falconDataCollectorList" />
    </bean>
```

### Falcon 上报数据项说明（上报周期：30秒）
 * naivecache_key_not_found/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Get 操作未找到 Key 的次数
 * naivecache_timeout/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Memcached 操作发生超时的错误次数
 * naivecache_error/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Memcached 操作发生异常的错误次数
 * naivecache_tps/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒平均执行次数
 * naivecache_peak_tps/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内每秒最大执行次数
 * naivecache_avg_exec_time/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 Memcached 操作平均执行时间
 * naivecache_max_exec_time/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内单次 Memcached 操作最大执行时间
 * naivecache_socket_read_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数
 * naivecache_socket_avg_read_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数
 * naivecache_socket_written_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数
 * naivecache_socket_avg_written_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数
 * naivecache_threadPool_rejected_count/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内所有线程池拒绝执行的任务总数
 * naivecache_threadPool_active_count/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池活跃线程数近似值总和
 * naivecache_threadPool_pool_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 采集时刻所有线程池线程数总和
 * naivecache_threadPool_peak_pool_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池出现过的最大线程数总和
 * naivecache_threadPool_core_pool_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的核心线程数总和
 * naivecache_threadPool_maximum_pool_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 所有线程池配置的最大线程数总和
 * naivecache_compression_reduce_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内压缩操作已节省的字节数
 * naivecache_compression_avg_reduce_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内平均每次压缩操作节省的字节数
 
### Memcached 示例代码
```java
    public class DemoService {
        
        @Resource(name = "memcachedClusterClient")
        private NaiveMemcachedClient naiveMemcachedClient;
        
        public void test() {
            User alice = new User(); //需要存入 Memcached 中的 User 实例，必须是可序列化的（实现 Serializable 接口）
            
            naiveMemcachedClient.set("demo_user_alice", alice, 30); //将 alice 实例存入 Memcached 中，并设置过期时间为 30 秒
            
            User aliceFromCache = naiveMemcachedClient.get("demo_user_alice"); //从 Memcached 中将 alice 实例取回
            
            User lucy = new User(); //需要存入 Memcached 中的 User 实例，必须是可序列化的（实现 Serializable 接口）
            
            naiveMemcachedClient.set("demo_user_lucy", lucy, 60); //将 lucy 实例存入 Memcached 中，并设置过期时间为 60 秒
            
            naiveMemcachedClient.touch("demo_user_alice", 60); //将 alice 实例的缓存过期时间重新设为 60 秒
            
            //使用 multiGet 方法进行批量 Get，可显著提升性能
            Set<String> keySet = new HashSet<>(); //将需要批量获取的 Key 放入 Set 中
            keySet.add("demo_user_alice");
            keySet.add("demo_user_lucy");
            Map<String, User> userMap = naiveMemcachedClient.multiGet(keySet); //执行 Memcached 批量 Get 操作
            User aliceFromCacheMap = userMap.get("demo_user_alice"); //从结果 Map 中获得 alice 实例
            User lucyFromCacheMap = userMap.get("demo_user_lucy"); //从结果 Map 中获得 lucy 实例
            
            naiveMemcachedClient.delete("demo_user_alice"); //在 Memcached 中删除 alice 实例
            naiveMemcachedClient.delete("demo_user_lucy"); //在 Memcached 中删除 lucy 实例
        }
    }
```
 
### 更多 Memcached 客户端
在使用单台 Memcached 服务进行低频访问的场景下，可使用一次性 Memcached 客户端 [OneTimeMemcachedClient](https://github.com/heimuheimu/naivecache/blob/master/src/main/java/com/heimuheimu/naivecache/memcached/advance/OneTimeMemcachedClient.java):
```xml
    <!-- 一次性 Memcached 客户端配置 -->
    <bean id="oneTimeMemcachedClient" class="com.heimuheimu.naivecache.memcached.advance.OneTimeMemcachedClient" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:11211" /> <!-- Memcached 服务地址 -->
    </bean>
```
 
在只有单台 Memcached 服务可用的场景下，可使用自动重连 Memcached 客户端 [AutoReconnectMemcachedClient](https://github.com/heimuheimu/naivecache/blob/master/src/main/java/com/heimuheimu/naivecache/memcached/advance/AutoReconnectMemcachedClient.java):
```xml
    <!-- 自动重连 Memcached 客户端配置 -->
    <bean id="autoReconnectMemcachedClient" class="com.heimuheimu.naivecache.memcached.advance.AutoReconnectMemcachedClient" destroy-method="close">
        <constructor-arg index="0" value="127.0.0.1:11211" /> <!-- Memcached 服务地址 -->
    </bean>
```

支持热部署（动态增加/减少 Memcached 服务地址）的 Memcached 集群客户端 [ReloadableMemcachedClusterClient](https://github.com/heimuheimu/naivecache/blob/master/src/main/java/com/heimuheimu/naivecache/memcached/cluster/ReloadableMemcachedClusterClient.java)
说明：ReloadableMemcachedClusterClient 客户端尚未在生产环境中进行验证。

## 本地缓存客户端

### Spring 配置
```xml
    <bean id="simpleNaiveLocalCacheClient" class="com.heimuheimu.naivecache.localcache.SimpleNaiveLocalCacheClient" init-method="init" destroy-method="close">
        <constructor-arg index="0" value="false"/> <!-- 是否开启序列化存储，如果无法控制从缓存中获取的实例被外部修改，可设置为 true -->
        <constructor-arg index="1" value="1000000"/> <!-- 允许存储的本地缓存最大数量，默认为 1 百万 -->
    </bean>
```

### Falcon 监控数据上报 Spring 配置
```xml
    <!-- 监控数据采集器列表 -->
    <util:list id="falconDataCollectorList">
        <!-- 本地缓存监控数据采集器 -->
        <bean class="com.heimuheimu.naivecache.localcache.monitor.falcon.LocalCacheDataCollector"></bean>
    </util:list>
    
    <!-- Falcon 监控数据上报器 -->
    <bean id="falconReporter" class="com.heimuheimu.naivemonitor.falcon.FalconReporter" init-method="init" destroy-method="close">
        <constructor-arg index="0" value="http://127.0.0.1:1988/v1/push" /> <!-- Falcon 监控数据推送地址 -->
        <constructor-arg index="1" ref="falconDataCollectorList" />
    </bean>
```

### Falcon 上报数据项说明（上报周期：30秒）
 * naivecache_local_error/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存操作出现异常总次数
 * naivecache_local_query/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存 get 操作总次数
 * naivecache_local_query_hit/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存 get 操作命中总次数
 * naivecache_local_added/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存新增 Key 的总数
 * naivecache_local_deleted/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存删除 Key 的总数
 * naivecache_local_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 当前本地缓存 Key 的总数
 
### Memcached 示例代码
```java
    public class DemoService {
        
        @Autowired
        private NaiveLocalCacheClient naiveLocalCacheClient;
        
        public void test() {
            User alice = new User(); //需要存入本地缓存中的 User 实例，如果本地缓存开启了序列化模式，则 User 必须是可序列化的（实现 Serializable 接口）
                        
            naiveLocalCacheClient.set("demo_user_alice", alice, 30); //将 alice 实例存入本地缓存中，并设置过期时间为 30 秒
            
            User aliceFromCache = naiveLocalCacheClient.get("demo_user_alice"); //从本地缓存中将 alice 实例取回，如果本地缓存没有开启序列化模式，则不允许对该实例进行修改操作
            
            naiveLocalCacheClient.delete("demo_user_alice"); //在本地缓存中删除 alice 实例
        }
    }
```

## 更多信息
* [Memcached 官网](https://www.memcached.org)
* [NaiveMonitor 项目主页](https://github.com/heimuheimu/naivemonitor)
* [NaiveCache v1.0 API Doc](https://heimuheimu.github.io/naivecache/api/v1.0/)
* [NaiveCache v1.0 源码下载](https://heimuheimu.github.io/naivecache/download/naivecache-1.0-sources.jar)
* [NaiveCache v1.0 Jar包下载](https://heimuheimu.github.io/naivecache/download/naivecache-1.0.jar)