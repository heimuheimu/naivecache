# NaiveCache: Memcached Java 客户端，简单易用

## 单台 Memcached 服务器
```
  NaiveMemcachedClient client = new AutoReconnectMemcachedClient("localhost:11211");
  client.set("demo_key", "Hello world!");
  System.out.println(String.valueOf(client.get("demo_key")));
```

## 多台 Memcached 服务器
```
  NaiveMemcachedClient client = new MemcachedClusterClient(
                new String[]{"localhost:11211", "localhost:21211", "localhost:31211"});
  client.set("demo_key", "Hello world!");
  System.out.println(String.valueOf(client.get("demo_key")));
```
