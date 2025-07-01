package ru.practicum.stat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStatsDto {
    String app;
    String uri;
    Long hits;
}