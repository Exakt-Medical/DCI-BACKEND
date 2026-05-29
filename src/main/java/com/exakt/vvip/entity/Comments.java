package com.exakt.vvip.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comments {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 100)
    private String users;

    @Column(length = 1000)
    private String comments;
}