package com.exakt.vvip.controller;

import com.exakt.vvip.dto.CommentsRequest;
import com.exakt.vvip.entity.Comments;
import com.exakt.vvip.service.CommentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Comments", description = "Comments Management Endpoints")
public class CommentsController {

    private final CommentsService commentsService;

    @GetMapping
    @Operation(summary = "Get all comments")
    public ResponseEntity<List<Comments>> getAll() {
        return ResponseEntity.ok(commentsService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comments by ID")
    public ResponseEntity<Comments> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commentsService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create comments")
    public ResponseEntity<Comments> create(
            @RequestBody CommentsRequest request) {

        return ResponseEntity.ok(commentsService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update comments")
    public ResponseEntity<Comments> update(
            @PathVariable Long id,
            @RequestBody CommentsRequest request) {

        return ResponseEntity.ok(commentsService.update(id, request));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comments")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        commentsService.delete(id);

        return ResponseEntity.noContent().build();
    }
}