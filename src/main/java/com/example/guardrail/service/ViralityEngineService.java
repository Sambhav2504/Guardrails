package com.example.guardrail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViralityEngineService {

    private final StringRedisTemplate redisTemplate;

    public void incrementBotReply(Long postId) {
        redisTemplate.opsForValue().increment("post:" + postId + ":virality_score", 1);
    }

    public void incrementHumanLike(Long postId) {
        redisTemplate.opsForValue().increment("post:" + postId + ":virality_score", 20);
    }

    public void incrementHumanComment(Long postId) {
        redisTemplate.opsForValue().increment("post:" + postId + ":virality_score", 50);
    }
    
    public Long getViralityScore(Long postId) {
        String score = redisTemplate.opsForValue().get("post:" + postId + ":virality_score");
        return score != null ? Long.parseLong(score) : 0L;
    }
}
