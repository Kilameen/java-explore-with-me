package ru.practicum.stat.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "endpoint")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    String app;
    @Column(nullable = false)
    String uri;
    @Column(nullable = false)
    String ip;
    @Column(name = "created", nullable = false)
    LocalDateTime timestamp;
}