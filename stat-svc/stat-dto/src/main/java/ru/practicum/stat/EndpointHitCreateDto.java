package ru.practicum.stat;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EndpointHitCreateDto {

    @NotBlank(message = "Параметр app не должен быть пустым.")
    String app;
    @NotBlank(message = "Параметр uri не должен быть пустым.")
    String uri;
    @NotBlank(message = "Параметр ip не должен быть пустым.")
    String ip;
    @NotBlank(message = "Параметр timestamp не должен быть пустым.")
    String timestamp;
}