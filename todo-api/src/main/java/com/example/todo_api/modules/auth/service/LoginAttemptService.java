package com.example.todo_api.modules.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;

/** Khoá đăng nhập theo email sau quá nhiều lần sai mật khẩu: login_fail:{email} -> số lần sai. */
@Service
public class LoginAttemptService {

    private final StringRedisTemplate redis;
    private final AppProperties props;

    public LoginAttemptService(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public void ensureNotLocked(String email) {
        String failures = redis.opsForValue().get(key(email));
        if (failures != null && Integer.parseInt(failures) >= props.login().maxFailures()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED",
                    "Too many failed attempts, please try again in "
                            + props.login().lockDuration().toMinutes() + " minutes");
        }
    }

    /** TTL tính từ lần sai đầu tiên. */
    public void recordFailure(String email) {
        Long count = redis.opsForValue().increment(key(email));
        if (count != null && count == 1) {
            redis.expire(key(email), props.login().lockDuration());
        }
    }

    public void reset(String email) {
        redis.delete(key(email));
    }

    private static String key(String email) {
        return "login_fail:" + email;
    }
}
