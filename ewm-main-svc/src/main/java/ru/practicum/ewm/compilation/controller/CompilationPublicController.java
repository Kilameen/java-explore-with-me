package ru.practicum.ewm.compilation.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
public class CompilationPublicController {
    private final CompilationService compilationService;

    @GetMapping
    public List<CompilationDto> getAllCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0")@PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10")@Positive Integer size) {
        log.info("GET запрос на получение подборок событий. pinned={}, from={}, size={}", pinned, from, size);
        List<CompilationDto> compilations = compilationService.getAllCompilations(from, size, pinned);
        log.info("Возвращен список подборок событий: {}", compilations);
        return compilations;
    }

    @GetMapping("/{compId}")
    public CompilationDto findCompilationById(@PathVariable Long compId) {
        log.info("GET запрос на получение подборки событий по id = {}", compId);
        CompilationDto compilationDto = compilationService.findCompilationById(compId);
        log.info("Возвращена подборка событий по id = {}: {}", compId, compilationDto);
        return compilationDto;
    }
}