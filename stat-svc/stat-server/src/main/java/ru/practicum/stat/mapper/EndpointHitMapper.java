package ru.practicum.stat.mapper;

import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.model.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EndpointHitMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static EndpointHit toEndpointHitFromCreateDto(EndpointHitCreateDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(LocalDateTime.parse(dto.getTimestamp(), formatter))
                .build();
    }

    public static EndpointHitDto toEndpointHitDto(EndpointHit endpointHit) {
        return EndpointHitDto.builder()
                .id(endpointHit.getId())
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp().format(formatter))
                .build();
    }
}
