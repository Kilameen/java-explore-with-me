package ru.practicum.stat;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Data
public class ViewStatsDtoRequest {
    List<String> uris;
    @Builder.Default
    LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    @Builder.Default
    LocalDateTime end = LocalDateTime.now();
    boolean unique;
    String application;
}
