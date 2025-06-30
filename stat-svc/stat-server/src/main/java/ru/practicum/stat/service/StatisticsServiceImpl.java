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
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) { // Принимаем LocalDateTime
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

        return viewStats.stream()
                .map(ViewStatsMapper::toViewStatsDto)
                .collect(Collectors.toList());
    }
}