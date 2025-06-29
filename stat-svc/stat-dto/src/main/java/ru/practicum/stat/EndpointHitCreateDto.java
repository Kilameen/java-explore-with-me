package ru.practicum.stat;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EndpointHitCreateDto {
    String app;
    String uri;
    String ip;
    LocalDateTime timestamp;
}
