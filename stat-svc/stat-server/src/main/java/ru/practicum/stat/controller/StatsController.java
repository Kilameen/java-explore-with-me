package ru.practicum.stat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.service.StatisticsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsService statisticsService;

    @PostMapping("/hit")
    public EndpointHitDto create(@RequestBody @Valid EndpointHitCreateDto endpoint) {
        log.info("POST запрос на создание нового  ");
        return statisticsService.create(endpoint);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start, // Изменил String на LocalDateTime
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,   // Изменил String на LocalDateTime
            @RequestParam(required = false) List<String> uris, // Указал тип List
            @RequestParam(defaultValue = "false") Boolean unique) {
        log.info("GET запрос на получение ");
        return statisticsService.getStats(start, end, uris, unique); // передаем LocalDateTime
    }
}
