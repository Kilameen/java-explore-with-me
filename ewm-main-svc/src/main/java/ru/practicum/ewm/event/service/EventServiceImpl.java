package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.location.LocationRepository;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

import ru.practicum.ewm.utils.enums.EventState;
import ru.practicum.ewm.utils.enums.StateAction;
import ru.practicum.stat.StatisticsClient;
import ru.practicum.stat.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static int MIN_HOURS_BEFORE_EVENT = 2;
    static int MIN_HOURS_BEFORE_PUBLISH = 1;
    static int DEFAULT_VIEWS = 0;
    static int DEFAULT_CONFIRMED_REQUESTS = 0;

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    LocationRepository locationRepository;
    UserRepository userRepository;
    StatisticsClient statClient;

    EventMapper eventMapper;
    CategoryMapper categoryMapper;
    LocationMapper locationMapper;
    UserMapper userMapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());
        LocalDateTime createdOn = LocalDateTime.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID " + newEventDto.getCategory() + " не найдена"));

        Event event = eventMapper.toEvent(newEventDto);
        event.setCreatedOn(createdOn);
        event.setCategory(category);
        event.setInitiator(user);
        event.setState(EventState.PENDING);

        Location location = locationMapper.toLocationFromDto(newEventDto.getLocation());
        Location savedLocation = locationRepository.save(location);
        event.setLocation(savedLocation);

        Event eventSaved = eventRepository.save(event);

        EventFullDto eventFullDto = eventMapper.toEventFullDto(eventSaved);
        eventFullDto.setViews(DEFAULT_VIEWS);
        eventFullDto.setConfirmedRequests(DEFAULT_CONFIRMED_REQUESTS);
        return eventFullDto;
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        LocalDateTime eventDate = adminRequest.getEventDate() == null ? event.getEventDate() : adminRequest.getEventDate();

        validateEventDateForAdmin(eventDate, adminRequest.getStateAction());
        validateStatusForAdmin(event.getState(), adminRequest.getStateAction());

        if (adminRequest.getAnnotation() != null) {
            event.setAnnotation(adminRequest.getAnnotation());
        }
        if (adminRequest.getDescription() != null) {
            event.setDescription(adminRequest.getDescription());
        }
        event.setEventDate(eventDate);
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }
        if (adminRequest.getTitle() != null) {
            event.setTitle(adminRequest.getTitle());
        }

        if (adminRequest.getCategory() != null) {
            Category category = categoryRepository.findById(adminRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + adminRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (adminRequest.getLocation() != null) {
            event.setLocation(adminRequest.getLocation());
        }

        if (adminRequest.getStateAction() != null) {
            switch (adminRequest.getStateAction()) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                    event.setState(EventState.CANCELED);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        updatedEvent.setViews(getViews(eventId, updatedEvent.getCreatedOn()));
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        validateUser(event.getInitiator(), user);
        LocalDateTime newEventDate = eventUserRequest.getEventDate() == null ? event.getEventDate() : eventUserRequest.getEventDate();

        validateEventDate(newEventDate);

        if (eventUserRequest.getAnnotation() != null) {
            event.setAnnotation(eventUserRequest.getAnnotation());
        }
        if (eventUserRequest.getDescription() != null) {
            event.setDescription(eventUserRequest.getDescription());
        }
        event.setEventDate(newEventDate);
        if (eventUserRequest.getPaid() != null) {
            event.setPaid(eventUserRequest.getPaid());
        }
        if (eventUserRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(eventUserRequest.getParticipantLimit());
        }
        if (eventUserRequest.getRequestModeration() != null) {
            event.setRequestModeration(eventUserRequest.getRequestModeration());
        }
        if (eventUserRequest.getTitle() != null) {
            event.setTitle(eventUserRequest.getTitle());
        }
        if (eventUserRequest.getStateAction() != null) {
            event.setState(EventState.PENDING);
        }

        if (eventUserRequest.getCategory() != null) {
            Category category = categoryRepository.findById(eventUserRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + eventUserRequest.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        if (eventUserRequest.getLocation() != null) {
            event.setLocation(eventUserRequest.getLocation());
        }

        Event updatedEvent = eventRepository.save(event);
        updatedEvent.setViews(getViews(eventId, updatedEvent.getCreatedOn()));
        return eventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EventShortDto> findAllByPublic(String text, List<Long> categories, Boolean paid, String rangeStart, String rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && LocalDateTime.parse(rangeStart, formatter).isAfter(LocalDateTime.parse(rangeEnd, formatter))) {
            throw new IncorrectRequestException("RangeStart is after Range End");
        }

        if (sort != null && !sort.equals("EVENT_DATE") && !sort.equals("VIEWS")) {
            throw new IncorrectRequestException("Unknown sort type");
        }

        statClient.create(request);

        List<Event> events = eventRepository.findAllByPublic(
                text,
                categories,
                paid,
                rangeStart == null ? null : LocalDateTime.parse(rangeStart, formatter),
                rangeEnd == null ? null : LocalDateTime.parse(rangeEnd, formatter),
                onlyAvailable,
                PageRequest.of(from / size, size) // from / size для правильного расчета страницы
        );

        Map<Long, Long> views = getViewsAllEvents(events);

        List<EventShortDto> eventShortDtos = events.stream()
                .map(event -> {
                    Long eventId = event.getId();
                    Long viewCount = views.getOrDefault(eventId, 0L); // Получаем просмотры или 0, если нет данных
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(Math.toIntExact(viewCount));
                    return eventShortDto;
                })
                .collect(Collectors.toList());

        if (sort == null || sort.equals("EVENT_DATE")) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        } else {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews));
        }

        return eventShortDtos;
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));

        Pageable pageable = PageRequest.of(from, size);
        Collection<Event> events = eventRepository.findAllByInitiatorId(user.getId(), pageable);

        return events.stream()
                .map(event -> {
                    CategoryDto categoryDto = categoryMapper.toCategoryDto(event.getCategory());
                    UserShortDto userShortDto = userMapper.toUserShortDto(event.getInitiator());
                    long views = getViews(event.getId(), event.getCreatedOn());

                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setCategory(categoryDto);
                    eventShortDto.setInitiator(userShortDto);
                    eventShortDto.setViews((int) views);

                    return eventShortDto;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<EventFullDto> findAllByAdmin(List<Long> users, List<String> states, List<Long> categories, String rangeStart, String rangeEnd, Integer from, Integer size) {
        LocalDateTime start = (rangeStart == null) ? null : LocalDateTime.parse(rangeStart, formatter);
        LocalDateTime end = (rangeEnd == null) ? null : LocalDateTime.parse(rangeEnd, formatter);

        Pageable pageable = PageRequest.of(from, size);

        Collection<Event> events = eventRepository.findAllByAdmin(users, states, categories, start, end, pageable);
        return events.stream()
                .map(event -> {
                    CategoryDto categoryDto = categoryMapper.toCategoryDto(event.getCategory());
                    LocationDto locationDto = locationMapper.toLocationDto(event.getLocation());
                    UserShortDto userShortDto = userMapper.toUserShortDto(event.getInitiator());

                    long views = getViews(event.getId(), event.getCreatedOn());

                    EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
                    eventFullDto.setCategory(categoryDto);
                    eventFullDto.setLocation(locationDto);
                    eventFullDto.setInitiator(userShortDto);
                    eventFullDto.setViews((int) views);

                    return eventFullDto;
                })
                .collect(Collectors.toList());
    }

@Transactional(readOnly = true)
@Override
public EventFullDto findEventById(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с ID = " + eventId + " не опубликовано");
        }
        statClient.create(request);
        EventFullDto eventFullDto = eventMapper.toEventFullDto(event);
        Map<Long, Long> viewStatsMap = getViewsAllEvents(List.of(event));
        Long views = viewStatsMap.getOrDefault(event.getId(), 0L);
        eventFullDto.setViews(Math.toIntExact(views));
        return eventFullDto;
    }

    @Override
    public void updateEventConfirmedRequests(Long eventId, int confirmedRequests) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));

        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата мероприятия должна быть не ранее, чем через 2 часа от текущего момента. " +
                    "Указанная дата: " + eventDate.format(formatter) + ", Текущее время + 2 часа: " + LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT).format(formatter));
        }
    }

    private void validateEventDateForAdmin(LocalDateTime eventDate, StateAction stateAction) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата мероприятия должна быть на " + MIN_HOURS_BEFORE_EVENT +  "часа раньше текущего момента");
        }
        if (stateAction != null && stateAction.equals(StateAction.PUBLISH_EVENT) && eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLISH))) {
            throw new ValidationException("Дата события должна быть на " + MIN_HOURS_BEFORE_PUBLISH +" час раньше момента публикации");
        }
    }

    private void validateStatusForAdmin(EventState state, StateAction stateAction) {
        if (stateAction != null && !stateAction.equals(StateAction.REJECT_EVENT) && !stateAction.equals(StateAction.PUBLISH_EVENT)) {
            throw new ForbiddenException("Неизвестный state action");
        }
        if (!state.equals(EventState.PENDING) && stateAction.equals(StateAction.PUBLISH_EVENT)) {
            throw new ConflictException("\n" +
                    "Не удается опубликовать незавершенное событие");
        }
        if (state.equals(EventState.PUBLISHED) && stateAction.equals(StateAction.REJECT_EVENT)) {
            throw new ConflictException("Невозможно отклонить уже опубликованное событие");
        }
    }

    private void validateUser(User user, User initiator) {
        if (!initiator.getId().equals(user.getId())) {
            throw new NotFoundException("Попытка изменить информацию не от инициатора события");
        }
    }

    private Integer getViews(Long eventId, LocalDateTime createdOn) {
        LocalDateTime start = createdOn;
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/events/" + eventId);
        Boolean unique = true;

        ResponseEntity<Object> statsResponse = statClient.getStats(start, end, uris, unique);

        if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.getBody() != null) {
            List<Map<String, Object>> stats = (List<Map<String, Object>>) statsResponse.getBody();
            if (!stats.isEmpty()) {
                Map<String, Object> stat = stats.getFirst();
                Long hits = (Long) stat.get("hits");
                return Math.toIntExact(hits);
            }
        }
        return DEFAULT_VIEWS;
    }

private Map<Long, Long> getViewsAllEvents(List<Event> events) {
    LocalDateTime start = LocalDateTime.now().minusYears(1);
    LocalDateTime end = LocalDateTime.now().plusYears(1);
    List<String> uris = events.stream()
            .map(event -> "/events/" + event.getId())
            .collect(Collectors.toList());
    ResponseEntity<Object> response = statClient.getStats(start, end, uris, true);


    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        List<ViewStatsDto> viewStatsList = Arrays.asList(new ObjectMapper().convertValue(response.getBody(), ViewStatsDto[].class));
        return viewStatsList.stream()
                .collect(Collectors.toMap(
                        viewStats -> Long.parseLong(viewStats.getUri().substring("/events/".length())),
                        ViewStatsDto::getHits
                ));
    } else {
        log.warn("Не удалось получить статистику просмотров. Код ответа: {}", response.getStatusCodeValue());
        return Collections.emptyMap();
    }
}
}
