package com.example.guardrail.controller;

import com.example.guardrail.dto.CommentRequest;
import com.example.guardrail.dto.PostCreateRequest;
import com.example.guardrail.entity.Comment;
import com.example.guardrail.entity.Post;
import com.example.guardrail.service.CoreInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class InteractionController {

    private final CoreInteractionService coreInteractionService;

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostCreateRequest request) {
        Post post = coreInteractionService.createPost(request);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        Comment comment = coreInteractionService.addComment(postId, request);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long postId) {
        coreInteractionService.likePost(postId);
        return ResponseEntity.ok().build();
    }
}
