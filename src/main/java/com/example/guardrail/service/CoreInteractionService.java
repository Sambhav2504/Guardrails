package com.example.guardrail.service;

import com.example.guardrail.dto.CommentRequest;
import com.example.guardrail.dto.PostCreateRequest;
import com.example.guardrail.entity.Comment;
import com.example.guardrail.entity.Post;
import com.example.guardrail.enums.AuthorType;
import com.example.guardrail.repository.CommentRepository;
import com.example.guardrail.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CoreInteractionService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ViralityEngineService viralityEngineService;
    private final GuardrailService guardrailService;
    private final NotificationEngineService notificationEngineService;

    @Transactional
    public Post createPost(PostCreateRequest request) {
        Post post = Post.builder()
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .build();
        return postRepository.save(post);
    }

    @Transactional
    public Comment addComment(Long postId, CommentRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        Comment parent = null;
        int depthLevel = 1;
        Long targetAuthorId = post.getAuthorId();
        AuthorType targetAuthorType = post.getAuthorType();

        if (request.getParentCommentId() != null) {
            parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));
            depthLevel = parent.getDepthLevel() + 1;
            targetAuthorId = parent.getAuthorId();
            targetAuthorType = parent.getAuthorType();
        }

        // GUARDRAILS
        if (request.getAuthorType() == AuthorType.BOT) {
            guardrailService.checkHorizontalCap(postId);
            
            if (targetAuthorType == AuthorType.USER) {
                guardrailService.checkCooldownCap(request.getAuthorId(), targetAuthorId);
            }
        }
        guardrailService.checkVerticalCap(depthLevel);

        // SAVE
        Comment comment = Comment.builder()
                .post(post)
                .parent(parent)
                .authorId(request.getAuthorId())
                .authorType(request.getAuthorType())
                .content(request.getContent())
                .depthLevel(depthLevel)
                .build();
        commentRepository.save(comment);

        // VIRALITY & NOTIFICATIONS
        if (request.getAuthorType() == AuthorType.BOT) {
            viralityEngineService.incrementBotReply(postId);
            if (targetAuthorType == AuthorType.USER) {
                notificationEngineService.notifyBotInteraction(targetAuthorId, request.getAuthorId());
            }
        } else {
            viralityEngineService.incrementHumanComment(postId);
        }

        return comment;
    }

    public void likePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        // Assuming user like as per problem statement "Human Like = +20"
        viralityEngineService.incrementHumanLike(postId);
    }
}
