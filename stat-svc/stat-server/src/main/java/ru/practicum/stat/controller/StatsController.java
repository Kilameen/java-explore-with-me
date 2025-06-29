package ru.practicum.stat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stat.EndpointHitCreateDto;
import ru.practicum.stat.EndpointHitDto;
import ru.practicum.stat.ViewStatsDto;
import ru.practicum.stat.model.ViewStats;
import ru.practicum.stat.service.StatisticsService;

import java.util.List;

@RestController
@RequestMapping
@Slf4j
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsService statisticsService;

    @PostMapping("/hit")
    public EndpointHitDto create(@RequestBody EndpointHitCreateDto endpoint) {
        log.info("POST запрос на создание нового  ");
        return statisticsService.create(endpoint);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") String start,
                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") String end,
                                       @RequestParam(required = false) List<String> uris,
                                       @RequestParam(defaultValue = "false") Boolean unique) {
        log.info("GET запрос на получение ");
        return statisticsService.getStats(start, end, uris, unique);
    }
}
