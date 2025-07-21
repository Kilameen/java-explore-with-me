package ru.practicum.ewm.compilation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.service.CompilationService;

@Slf4j
@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
public class CompilationAdminController {

    private final CompilationService compilationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CompilationDto create(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        log.info("POST запрос на добавление подборки событий - ADMIN: {}", newCompilationDto);
        CompilationDto compilationDto = compilationService.create(newCompilationDto);
        log.info("Подборка событий создана: {}", compilationDto);
        return compilationDto;
    }

    @PatchMapping("/{compId}")
    public CompilationDto update(@Valid @RequestBody UpdateCompilationRequest updateCompilation,
                                 @PathVariable Long compId) {
        log.info("PATCH запрос на обновление подборки событий с id={} - ADMIN: {}", compId, updateCompilation);
        CompilationDto compilationDto = compilationService.update(compId, updateCompilation);
        log.info("Подборка событий с id={} обновлена: {}", compId, compilationDto);
        return compilationDto;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.info("DELETE запрос на удаление подборки событий с id={} - ADMIN", id);
        compilationService.delete(id); // Удаляем подборку
        log.info("Подборка событий с id={} удалена", id);
    }
}