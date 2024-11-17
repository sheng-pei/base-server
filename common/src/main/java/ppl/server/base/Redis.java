package ppl.server.base;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Redis {
    private static final RedisScript<Boolean> DECR_UP_BOTTOM_SCRIPT = RedisScript.of(
            "local k = KEYS[1]\n" +
                    "local bottom = tonumber(ARGV[1])\n" +
                    "local reply = redis.call('get', k)\n" +
                    "if reply == false then\n" +
                    "  return { err = 'Key: \\'' .. k .. '\\' not exists.' }\n" +
                    "end\n" +
                    "local e = tonumber(reply)\n" +
                    "if e == nil then\n" +
                    "  return { err = 'Non-integer value of key: \\'' .. k .. '\\' is not allowed.' }\n" +
                    "end\n" +
                    "if e > bottom then\n" +
                    "  redis.call('decr', k)\n" +
                    "  return true\n" +
                    "end\n" +
                    "return false", Boolean.class);

    private static final RedisScript<Boolean> INCR_UNDER_TOP_SCRIPT = RedisScript.of(
            "local k = KEYS[1]\n" +
                    "local top = tonumber(ARGV[1])\n" +
                    "local ttl = tonumber(ARGV[2])\n" +
                    "local reply = redis.call('get', k)\n" +
                    "if reply == false then\n" +
                    "  return { err = 'Key: \\'' .. k .. '\\' not exists.' }\n" +
                    "end\n" +
                    "local e = tonumber(reply)\n" +
                    "if e == nil then\n" +
                    "  return { err = 'Non-integer value of key: \\'' .. k .. '\\' is not allowed.' }\n" +
                    "end\n" +
                    "if e < top then\n" +
                    "  redis.call('incr', k)\n" +
                    "  return true\n" +
                    "end\n" +
                    "if ttl then\n" +
                    "  redis.call('expire', k, ttl)\n" +
                    "end\n" +
                    "return false", Boolean.class);

    private static final RedisScript<Void> SET_IF_ABSENT_AND_EXPIRE_SCRIPT = RedisScript.of(
            "local k = KEYS[1]\n" +
                    "local v = ARGV[1]\n" +
                    "local ttl = tonumber(ARGV[2])\n" +
                    "redis.call('set', k, v, 'NX')\n" +
                    "redis.call('expire', k, ttl)\n" +
                    "return { ok = 'OK' }");

    private static final RedisScript<String> GET_AND_DELETE_SCRIPT = RedisScript.of(
            "local k = KEYS[1]\n" +
                    "local reply = redis.call('get', k)\n" +
                    "redis.call('del', k)\n" +
                    "return reply", String.class);

    private final StringRedisTemplate redisTemplate;

    public Redis(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getAndDelete(String key) {
        return redisTemplate.execute(GET_AND_DELETE_SCRIPT, Collections.singletonList(key));
    }

    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        setIfAbsent(key, value, timeout, unit, false);
    }

    public void setIfAbsent(String key, String value, long timeout, TimeUnit unit, boolean refreshExpire) {
        if (refreshExpire) {
            long ttl = unit.toSeconds(timeout);
            redisTemplate.execute(SET_IF_ABSENT_AND_EXPIRE_SCRIPT, Collections.singletonList(key), value, ttl + "");
        } else {
            redisTemplate.opsForValue()
                    .setIfAbsent(key, value, timeout, unit);
        }
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean decrementOverBottom(String key, long bottom) {
        Boolean res = redisTemplate.execute(DECR_UP_BOTTOM_SCRIPT,
                Collections.singletonList(key), bottom + "");
        return res != null && res;
    }

    public boolean incrementUnderTop(String key, long top) {
        return incrementUnderTop(key, top, -1, null);
    }

    public boolean incrementUnderTop(String key, long top, long timeout, TimeUnit unit) {
        List<String> values = new ArrayList<>();
        values.add(top + "");
        if (timeout > 0) {
            values.add(unit.toSeconds(timeout) + "");
        }
        Boolean res = redisTemplate.execute(INCR_UNDER_TOP_SCRIPT,
                Collections.singletonList(key), values.toArray());
        return res != null && res;
    }

}
