package com.example.guardrail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GuardrailService {

    private final StringRedisTemplate redisTemplate;

    public void checkHorizontalCap(Long postId) {
        Long count = redisTemplate.opsForValue().increment("post:" + postId + ":bot_count");
        if (count != null && count > 100) {
            // We could optionally decrement it back to 100, but it strictly just prevents > 100 successful operations.
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot comment limit reached for this post");
        }
    }

    public void checkVerticalCap(Integer depthLevel) {
        if (depthLevel != null && depthLevel > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment thread cannot go deeper than 20 levels");
        }
    }

    public void checkCooldownCap(Long botId, Long targetUserId) {
        String key = "cooldown:bot_" + botId + ":human_" + targetUserId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        
        if (Boolean.FALSE.equals(acquired)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Bot is in cooldown for interacting with this human");
        }
    }
}
