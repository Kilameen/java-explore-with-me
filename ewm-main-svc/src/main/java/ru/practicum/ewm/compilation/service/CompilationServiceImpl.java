package ru.practicum.ewm.compilation.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.CompilationRepository;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationServiceImpl implements CompilationService {

    CompilationRepository compilationRepository;
    EventRepository eventRepository;

    @Override
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        log.info("Добавление подборки: {}", newCompilationDto);
        Set<Long> eventIds = Optional.ofNullable(newCompilationDto.getEvents()).orElse(Collections.emptySet());
        List<Event> events = eventRepository.findAllByIdIn(new ArrayList<>(eventIds));

        Compilation compilation = CompilationMapper.toCompilationFromNew(newCompilationDto, new HashSet<>(events));
        compilation.setPinned(Optional.ofNullable(compilation.getPinned()).orElse(false));
        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Подборка сохранена: {}", savedCompilation);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());

        return CompilationMapper.toCompilationDto(savedCompilation, eventShortDtos);
    }

    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest updateCompilationRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка c ID " + compId + " не найдена"));
        log.info("Обновление подборки c {}, на {}", updateCompilationRequest.toString(), compilation.toString());
        if (updateCompilationRequest.getEvents() != null) {
            Set<Long> eventIds = updateCompilationRequest.getEvents();
            List<Event> events = eventRepository.findAllByIdIn(new ArrayList<>(eventIds));
            compilation.setEvents(new HashSet<>(events));
            log.trace("Events = {}", compilation.getEvents());
        }
        compilation.setPinned(Optional.ofNullable(updateCompilationRequest.getPinned()).orElse(false));
        log.trace("Pinned = {}", compilation.getPinned());

        compilation.setTitle(Optional.ofNullable(updateCompilationRequest.getTitle()).orElse(compilation.getTitle()));
        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Подборка обновлена: {}", compilation);
        List<EventShortDto> eventShortDtos = updatedCompilation.getEvents().stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());

        return CompilationMapper.toCompilationDto(updatedCompilation, eventShortDtos);
    }

    @Override
    public void delete(Long id) {
        compilationRepository.deleteById(id);
        log.info("Подборка удалена");
    }

    @Transactional(readOnly = true)
    @Override
    public List<CompilationDto> getAllCompilations(Integer from, Integer size, Boolean pinned) {
        log.info("Получение всех подборок с from={}, size={}, pinned={}", from, size, pinned);
        PageRequest pageRequest = PageRequest.of(from, size);
        List<Compilation> compilations;
        if (pinned != null) {
            log.info("Получение всех подборок с pinned: {}", pinned);
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest);
            log.info("Получены подборки с pinned={}: {}", pinned, compilations);
        } else {
            log.info("Получение всех подборок без фильтрации по pinned");
            compilations = compilationRepository.findAll(pageRequest).getContent();
            log.info("Получены все подборки: {}", compilations);

        }
        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                            .map(EventMapper::toEventShortDto)
                            .collect(Collectors.toList());

                    return CompilationMapper.toCompilationDto(compilation, eventShortDtos);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public CompilationDto findCompilationById(Long compId) {
        log.info("Получение подборки с compId={}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка c ID " + compId + " не найдена"));

        log.info("Подборка найдена: {}", compilation);

        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(EventMapper::toEventShortDto)
                .collect(Collectors.toList());

        return CompilationMapper.toCompilationDto(compilation, eventShortDtos);
    }
}