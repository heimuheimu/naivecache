# NaiveCache: Memcached Java 客户端，简单易用

## 单台 Memcached 服务器
```
  NaiveMemcachedClient client = NaiveMemcachedClientFactory
      .create("localhost:11211", null, 1000, 65535, null);
  client.set("demo_key", "Hello world!");
  System.out.println(String.valueOf(client.get("demo_key")));
```

## 多台 Memcached 服务器
```
  MemcachedClusterClient client = new MemcachedClusterClient(
                new String[]{"localhost:11211", "localhost:21211", "localhost:31211"},
                null, 1000, 65535, null, null);
  client.set("demo_key", "Hello world!");
  System.out.println(String.valueOf(client.get("demo_key")));
```
