# testy-redis-box

`testy-redis-box` provides extensions to run an embedded [Redis](https://redis.io/) database in your junit 5 tests.

## WithEmbeddedRedis

Through `@ExtendWith` annotation

```java
@ExtendWith(WithEmbeddedRedis.class)
class WithEmbeddedRedisTest {
    @Test
    void should_use_embedded_redis(@RedisPort Integer redisPort, RedisClient client, RedisServer server) {
        // your test code
    }
}
```

or through

```java
class WithEmbeddedRedisBuilderTest {
    @RegisterExtension
    public static WithEmbeddedRedis mockRedis = WithEmbeddedRedis.builder().build();

    @Test
    void should_use_embedded_redis(@RedisPort Integer redisPort, RedisClient client, RedisServer server) {
        // your test code
    }
}
```
