package com.example.guardrail.dto;

import com.example.guardrail.enums.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostCreateRequest {
    @NotNull
    private Long authorId;
    @NotNull
    private AuthorType authorType;
    @NotBlank
    private String content;
}
