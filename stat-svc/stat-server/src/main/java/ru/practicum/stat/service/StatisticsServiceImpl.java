package ru.practicum.stat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.EndpointHitRepository;
import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.mapper.EndpointHitMapper;
import ru.practicum.stat.mapper.ViewStatsMapper;
import ru.practicum.stat.model.EndpointHit;
import ru.practicum.stat.model.ViewStats;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final EndpointHitRepository endpointHitRepository;

    @Override
    public EndpointHitDto create(EndpointHitCreateDto endpoint) {
        EndpointHit hit = EndpointHitMapper.toEndpointHitFromCreateDto(endpoint);
        EndpointHit createHit = endpointHitRepository.save(hit);
        return EndpointHitMapper.toEndpointHitDto(createHit);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start);
        LocalDateTime endTime = LocalDateTime.parse(end);

        List<ViewStats> viewStats;

        if (unique) {
            viewStats = endpointHitRepository.findStatsUniqueIp(startTime, endTime, uris);
        } else {
            viewStats = endpointHitRepository.findStats(startTime, endTime, uris);
        }
        return viewStats.stream()
                .map(ViewStatsMapper::toViewStatsDto)
                .collect(Collectors.toList());
    }
}