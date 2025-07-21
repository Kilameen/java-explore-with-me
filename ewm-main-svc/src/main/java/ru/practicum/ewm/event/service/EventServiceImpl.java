package ru.practicum.ewm.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.exception.*;
import ru.practicum.ewm.location.LocationRepository;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.request.EventRequestRepository;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.enums.EventState;
import ru.practicum.ewm.enums.RequestStatus;
import ru.practicum.ewm.enums.StateAction;
import ru.practicum.stat.StatisticsClient;
import ru.practicum.stat.ViewStatsDto;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static int MIN_HOURS_BEFORE_EVENT = 2;
    static int MIN_HOURS_BEFORE_PUBLISH = 1;

    EventRepository eventRepository;
    CategoryRepository categoryRepository;
    LocationRepository locationRepository;
    UserRepository userRepository;
    EventRequestRepository eventRequestRepository;

    StatisticsClient statClient;
    ObjectMapper mapper;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        validateEventDate(newEventDto.getEventDate());
        LocalDateTime createdOn = LocalDateTime.now();

        User user = getUserById(userId);
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID " + newEventDto.getCategory() + " не найдена"));

        Event event = EventMapper.toEvent(newEventDto);
        event.setCreatedOn(createdOn);
        event.setCategory(category);
        event.setInitiator(user);

        Location location = LocationMapper.toLocationFromDto(newEventDto.getLocation());
        Location savedLocation = locationRepository.save(location);
        event.setLocation(savedLocation);

        Event eventSaved = eventRepository.save(event);

        return EventMapper.toEventFullDto(eventSaved);
    }

    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = getEventById(eventId);

        LocalDateTime eventDate = (adminRequest.getEventDate() != null) ? adminRequest.getEventDate() : event.getEventDate();
        validateEventDateForAdmin(eventDate, adminRequest.getStateAction());
        validateStatusForAdmin(event.getState(), adminRequest.getStateAction());

        updateEventFields(event, Optional.ofNullable(adminRequest.getAnnotation()), Optional.ofNullable(adminRequest.getDescription()),
                eventDate, Optional.ofNullable(adminRequest.getPaid()), Optional.ofNullable(adminRequest.getParticipantLimit()),
                Optional.ofNullable(adminRequest.getRequestModeration()), Optional.ofNullable(adminRequest.getTitle()),
                adminRequest.getCategory());

        processStateAction(event, adminRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    public EventFullDto updateEventByPrivate(Long userId, Long eventId, UpdateEventUserRequest eventUserRequest) {
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        validateUser(event.getInitiator(), user);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }
        LocalDateTime eventDate = (eventUserRequest.getEventDate() != null) ? eventUserRequest.getEventDate() : event.getEventDate();
        validateEventDate(eventDate);

        updateEventFields(event, Optional.ofNullable(eventUserRequest.getAnnotation()), Optional.ofNullable(eventUserRequest.getDescription()),
                eventDate, Optional.ofNullable(eventUserRequest.getPaid()), Optional.ofNullable(eventUserRequest.getParticipantLimit()),
                Optional.ofNullable(eventUserRequest.getRequestModeration()), Optional.ofNullable(eventUserRequest.getTitle()),
                eventUserRequest.getCategory());

        processStateAction(event, eventUserRequest.getStateAction());

        Event updatedEvent = eventRepository.save(event);
        return EventMapper.toEventFullDto(updatedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEventOfUser(Long userId, Long eventId) {
        log.info("Получение события от пользователя {}", userId);
        User user = getUserById(userId);
        Event event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Пользователь не является инициатором события");
        }
        return EventMapper.toEventFullDto(event);
    }

    @Override
    public Collection<EventShortDto> findAllByPublic(EventSearchParams params, HttpServletRequest request) {

        if (params.getRangeStart() != null && params.getRangeEnd() != null && params.getRangeStart().isAfter(params.getRangeEnd())) {
            throw new IllegalArgumentException("rangeStart должен быть раньше rangeEnd");
        }

        if (params.getSort() != null && !List.of("EVENT_DATE", "VIEWS").contains(params.getSort().toUpperCase())) {
            throw new IncorrectRequestException("Unknown sort type");
        }

        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize());
        Specification<Event> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("state"), EventState.PUBLISHED));


            if (params.getText() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), "%" + params.getText().toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + params.getText().toLowerCase() + "%")
                ));
            }

            if (params.getCategories() != null && !params.getCategories().isEmpty()) {
                predicates.add(root.get("category").get("id").in(params.getCategories()));
            }

            if (params.getPaid() != null) {
                predicates.add(criteriaBuilder.equal(root.get("paid"), params.getPaid()));
            }

            if (params.getRangeStart() == null && params.getRangeEnd() == null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), LocalDateTime.now()));
            } else {
                predicates.add(criteriaBuilder.between(root.get("eventDate"), params.getRangeStart(), params.getRangeEnd()));
            }

            if (Boolean.TRUE.equals(params.getOnlyAvailable())) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("participantLimit"), 0),
                        criteriaBuilder.greaterThan(root.get("participantLimit"), root.get("confirmedRequests"))
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        sendStats(request);

        List<EventShortDto> eventShortDtos = eventPage.getContent().stream()
                .map(event -> {
                    EventShortDto eventDto = EventMapper.toEventShortDto(event);
                    eventDto.setViews(getViews(event.getId(), event.getCreatedOn(), request));
                    eventDto.setConfirmedRequests(eventDto.getConfirmedRequests());
                    return eventDto;
                })
                .collect(Collectors.toList());

        if ("EVENT_DATE".equalsIgnoreCase(params.getSort())) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getEventDate));
        } else if ("VIEWS".equalsIgnoreCase(params.getSort())) {
            eventShortDtos.sort(Comparator.comparing(EventShortDto::getViews));
        }

        return eventShortDtos;
    }

    @Override
    public Collection<EventShortDto> findAllByPrivate(Long userId, Integer from, Integer size, HttpServletRequest request) {

        User user = getUserById(userId);
        Pageable pageable = PageRequest.of(from, size);
        List<Event> events = eventRepository.findAllByInitiatorId(user.getId(), pageable);

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = EventMapper.toEventShortDto(event);
                    eventShortDto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                    eventShortDto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                    eventShortDto.setViews(getViews(event.getId(), event.getCreatedOn(), request));

                    eventShortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));

                    return eventShortDto;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Collection<EventFullDto> findAllByAdmin(EventSearchParams params, HttpServletRequest request) {

        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize());

        List<Event> eventList;
        try {
            eventList = eventRepository.findAllByAdmin(params.getUsers(), params.getStates(), params.getCategories(), params.getRangeStart(), params.getRangeEnd(), pageable);
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса к БД: ", e);
            throw new RuntimeException("Ошибка при получении данных из базы данных", e);
        }

        List<Long> eventIds = eventList.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsForEvents(eventIds);

        return eventList.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.toEventFullDto(event);
                    dto.setViews(getViews(event.getId(), event.getCreatedOn(), request));
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getConfirmedRequestsForEvents(List<Long> eventIds) {
        List<Object[]> results = eventRequestRepository.countByEventIdInAndStatus(eventIds, RequestStatus.CONFIRMED);
        Map<Long, Long> confirmedRequestsMap = new HashMap<>();
        for (Object[] result : results) {
            Long eventId = (Long) result[0];
            Long count = (Long) result[1];
            confirmedRequestsMap.put(eventId, count);
        }
        return confirmedRequestsMap;
    }

    @Override
    public EventFullDto findEventById(Long eventId, HttpServletRequest request) {
        Event event = getEventById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Событие с ID = " + eventId + " не опубликовано");
        }

        sendStats(request);

        EventFullDto eventFullDto = EventMapper.toEventFullDto(event);
        eventFullDto.setViews(getViews(eventId, event.getCreatedOn(), request));
        eventFullDto.setConfirmedRequests(eventRequestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED));
        return eventFullDto;
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime nowPlusMinHours = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT);
        if (eventDate.isBefore(nowPlusMinHours)) {
            String formattedEventDate = eventDate.format(formatter);
            String formattedMinHours = nowPlusMinHours.format(formatter);

            throw new ValidationException("Дата мероприятия должна быть не ранее, чем через " + MIN_HOURS_BEFORE_EVENT + " часа(ов) от текущего момента. " +
                    "Указанная дата: " + formattedEventDate + ", Минимальная допустимая дата: " + formattedMinHours);
        }
    }

    private void validateEventDateForAdmin(LocalDateTime eventDate, StateAction stateAction) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT))) {
            throw new ValidationException("Дата мероприятия должна быть на " + MIN_HOURS_BEFORE_EVENT + "часа раньше текущего момента");
        }
        if (stateAction != null && stateAction.equals(StateAction.PUBLISH_EVENT) && eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_PUBLISH))) {
            throw new ValidationException("Дата события должна быть на " + MIN_HOURS_BEFORE_PUBLISH + " час раньше момента публикации");
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

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c ID " + userId + " не найден"));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие c ID " + eventId + " не найдено"));
    }

    private void updateEventFields(Event event, Optional<String> annotation, Optional<String> description,
                                   LocalDateTime eventDate, Optional<Boolean> paid, Optional<Integer> participantLimit,
                                   Optional<Boolean> requestModeration, Optional<String> title, Long categoryId) {

        annotation.ifPresent(event::setAnnotation);
        description.ifPresent(event::setDescription);
        event.setEventDate(eventDate);
        paid.ifPresent(event::setPaid);
        participantLimit.ifPresent(event::setParticipantLimit);
        requestModeration.ifPresent(event::setRequestModeration);
        title.ifPresent(event::setTitle);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Категория с ID " + categoryId + " не найдена"));
            event.setCategory(category);
        }
    }

    private void processStateAction(Event event, StateAction stateAction) {
        if (stateAction != null) {
            switch (stateAction) {
                case PUBLISH_EVENT:
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case REJECT_EVENT:
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new IllegalArgumentException("Недопустимое действие над событием: " + stateAction);
            }
        }
    }

    private void sendStats(HttpServletRequest request) {
        try {
            statClient.create(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке статистики: {}", e.getMessage());

        }
    }

    private Long getViews(Long eventId, LocalDateTime createdOn, HttpServletRequest request) {
        LocalDateTime end = LocalDateTime.now();
        String uri = request.getRequestURI();
        Boolean unique = true;
        Long defaultViews = 0L;

        try {
            ResponseEntity<Object> statsResponse = statClient.getStats(createdOn, end, List.of(uri), unique);
            log.info("Запрос к statClient: URI={}, from={}, to={}, unique={}", uri, createdOn, end, unique);
            log.info("Ответ от statClient: status={}, body={}", statsResponse.getStatusCode(), statsResponse.getBody());
            if (statsResponse.getStatusCode().is2xxSuccessful() && statsResponse.hasBody()) {
                Object body = statsResponse.getBody();
                if (body != null) {
                    try {
                        ViewStatsDto[] statsArray = mapper.convertValue(body, ViewStatsDto[].class);
                        List<ViewStatsDto> stats = Arrays.asList(statsArray);

                        if (!stats.isEmpty()) {
                            return stats.getLast().getHits();
                        } else {
                            log.info("Нет данных статистики для события {}", eventId);
                        }
                    } catch (Exception e) {
                        log.error("Ошибка преобразования данных статистики для события {}: {}", eventId, e.getMessage());
                        return defaultViews;
                    }
                } else {
                    log.warn("Тело ответа от statClient пустое для события {}", eventId);
                }
            } else {
                log.warn("Неуспешный ответ от statClient для события {}: {}", eventId, statsResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при получении статистики для события {}: {}", eventId, e.getMessage());
        }
        return defaultViews;
    }
}