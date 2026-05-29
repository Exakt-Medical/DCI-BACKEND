package com.exakt.vvip.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentsRequest {


    private String referenceNumber;
    private String users;
    private String comments;
}