package ru.practicum.stat.mapper;

import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.model.ViewStats;

public class ViewStatsMapper {

    public static ViewStatsDto toViewStatsDto(ViewStats viewStats) {
        return ViewStatsDto.builder()
                .app(viewStats.getApp())
                .uri(viewStats.getUri())
                .hits(viewStats.getHits())
                .build();
    }
}