package com.exakt.vvip.service;

import com.exakt.vvip.dto.CommentsRequest;
import com.exakt.vvip.entity.Comments;
import com.exakt.vvip.repository.CommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentsService {

    private final CommentsRepository commentsRepository;

    public List<Comments> getAll() {
        return commentsRepository.findAll();
    }

    public Comments getById(Long id) {
        return commentsRepository.findById(id).orElse(null);
    }

    public Comments create(CommentsRequest request) {

        Comments comments = Comments.builder()
                .referenceNumber(request.getReferenceNumber())
                .users(request.getUsers())
                .comments(request.getComments())
                .build();

        return commentsRepository.save(comments);
    }

    public Comments update(Long id, CommentsRequest request) {

        Comments comments = commentsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comments not found"));

        comments.setReferenceNumber(request.getReferenceNumber());
        comments.setUsers(request.getUsers());
        comments.setComments(request.getComments());

        return commentsRepository.save(comments);
    }

    public void delete(Long id) {
        commentsRepository.deleteById(id);
    }
}