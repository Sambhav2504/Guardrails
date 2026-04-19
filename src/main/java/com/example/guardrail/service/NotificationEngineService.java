package com.example.guardrail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEngineService {

    private final StringRedisTemplate redisTemplate;

    public void notifyBotInteraction(Long targetUserId, Long botId) {
        String cooldownKey = "user:" + targetUserId + ":notif_cooldown";
        String pendingListKey = "user:" + targetUserId + ":pending_notifs";
        String pendingUsersSet = "pending_notif_users";

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(hasCooldown)) {
            // Push the bot ID to pending
            redisTemplate.opsForList().rightPush(pendingListKey, String.valueOf(botId));
            // Add user to the set of users with pending notifications
            redisTemplate.opsForSet().add(pendingUsersSet, String.valueOf(targetUserId));
        } else {
            log.info("Push Notification Sent to User: Bot {} interacted with your post", botId);
            redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofMinutes(15));
        }
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void sweepPendingNotifications() {
        String pendingUsersSet = "pending_notif_users";
        Set<String> users = redisTemplate.opsForSet().members(pendingUsersSet);
        if (users == null || users.isEmpty()) {
            return;
        }

        for (String userId : users) {
            String pendingListKey = "user:" + userId + ":pending_notifs";
            String processingKey = pendingListKey + ":processing";
            
            try {
                // Rename the key atomically to avoid data loss from concurrent pushes
                redisTemplate.rename(pendingListKey, processingKey);
                
                List<String> botIds = redisTemplate.opsForList().range(processingKey, 0, -1);
                
                if (botIds != null && !botIds.isEmpty()) {
                    String firstBot = botIds.get(0);
                    int others = botIds.size() - 1;
                    
                    if (others == 0) {
                        log.info("Summarized Push Notification: Bot {} interacted with your posts.", firstBot);
                    } else {
                        log.info("Summarized Push Notification: Bot {} and {} others interacted with your posts.", firstBot, others);
                    }
                }
                
                // Clean up the processing list
                redisTemplate.delete(processingKey);
            } catch (Exception e) {
                // Ignore if the key doesn't exist during rename
                log.debug("No pending notifications found to process for user {}", userId);
            }
            
            // Remove user from the set since we processed this batch
            redisTemplate.opsForSet().remove(pendingUsersSet, userId);
        }
    }
}
