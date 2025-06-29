package ru.practicum.stat.service;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.ViewStatsDto;

import java.util.List;

public interface StatisticsService {
    EndpointHitDto create(EndpointHitCreateDto endpoint);

    @Transactional(readOnly = true)
    List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique);
}
