package ru.practicum.stat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stat.EndpointHitRepository;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.mapper.EndpointHitMapper;
import ru.practicum.stat.mapper.ViewStatsMapper;
import ru.practicum.stat.model.EndpointHit;
import ru.practicum.stat.model.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StatisticsServiceImpl implements StatisticsService {

    private final EndpointHitRepository endpointHitRepository;

    @Override
    public EndpointHitDto create(EndpointHitCreateDto endpointHitCreateDto) {
        log.info("Создание EndpointHit с данными: {}", endpointHitCreateDto);
        EndpointHit hit = EndpointHitMapper.toEndpointHitFromCreateDto(endpointHitCreateDto);
        EndpointHit createdHit = endpointHitRepository.save(hit);
        return EndpointHitMapper.toEndpointHitDto(createdHit);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Получение статистики с start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        List<ViewStats> viewStats;
        if (unique) {
            if (uris != null && !uris.isEmpty()) {
                viewStats = endpointHitRepository.findStatsUniqueIp(start, end, uris);
            } else {
                viewStats = endpointHitRepository.findStatsUniqueIpAllUris(start, end);
            }
        } else {
            if (uris != null && !uris.isEmpty()) {
                viewStats = endpointHitRepository.findStats(start, end, uris);
            } else {
                viewStats = endpointHitRepository.findStatsAllUris(start, end);
            }
        }
        log.info("Получена статистика: {}", viewStats);

        return viewStats != null ? viewStats.stream()
                .map(ViewStatsMapper::toViewStatsDto)
                .collect(Collectors.toList()) : List.of();
    }
}
