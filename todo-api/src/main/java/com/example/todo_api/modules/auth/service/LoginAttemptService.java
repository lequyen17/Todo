package com.example.todo_api.modules.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.todo_api.config.AppProperties;
import com.example.todo_api.core.exceptions.ApiException;
import com.example.todo_api.modules.auth.AuthErrorCode;

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
        String failures = redis.opsForValue().get(AuthRedisKeys.loginFailures(email));
        if (failures != null && Integer.parseInt(failures) >= props.login().maxFailures()) {
            throw new ApiException(AuthErrorCode.LOGIN_LOCKED, props.login().lockDuration().toMinutes());
        }
    }

    /** TTL tính từ lần sai đầu tiên. */
    public void recordFailure(String email) {
        Long count = redis.opsForValue().increment(AuthRedisKeys.loginFailures(email));
        if (count != null && count == 1) {
            redis.expire(AuthRedisKeys.loginFailures(email), props.login().lockDuration());
        }
    }

    public void reset(String email) {
        redis.delete(AuthRedisKeys.loginFailures(email));
    }
}
