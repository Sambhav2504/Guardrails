package com.example.guardrail;

import com.example.guardrail.dto.CommentRequest;
import com.example.guardrail.dto.PostCreateRequest;
import com.example.guardrail.entity.Post;
import com.example.guardrail.enums.AuthorType;
import com.example.guardrail.repository.CommentRepository;
import com.example.guardrail.repository.PostRepository;
import com.example.guardrail.service.CoreInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrencySpamTest {

    @Autowired
    private CoreInteractionService coreInteractionService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Long testPostId;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        
        // Clear redis keys related to tests
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        PostCreateRequest createReq = new PostCreateRequest();
        createReq.setAuthorId(1L);
        createReq.setAuthorType(AuthorType.USER);
        createReq.setContent("Test Post for Concurrency");
        
        Post post = coreInteractionService.createPost(createReq);
        this.testPostId = post.getId();
    }

    @Test
    void testHorizontalCap_200ConcurrentRequests() throws InterruptedException {
        int totalRequests = 200;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successfulComments = new AtomicInteger();
        AtomicInteger failedComments = new AtomicInteger();

        for (int i = 0; i < totalRequests; i++) {
            final Long botId = (long) (i + 1);
            executorService.submit(() -> {
                try {
                    CommentRequest req = new CommentRequest();
                    req.setAuthorId(botId);
                    req.setAuthorType(AuthorType.BOT);
                    req.setContent("Spam comment " + botId);
                    
                    coreInteractionService.addComment(testPostId, req);
                    successfulComments.incrementAndGet();
                } catch (Exception e) {
                    failedComments.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        
        // Verify only 100 succeeded
        assertEquals(100, successfulComments.get(), "Exactly 100 comments should succeed");
        
        // Verify database exact count
        long dbCommentCount = commentRepository.count();
        assertEquals(100, dbCommentCount, "Database should exactly have 100 comments");
    }
}
