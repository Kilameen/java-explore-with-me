package ru.practicum.ewm.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.EventRequest;

@Component
public class EventRequestMapper {
    public ParticipationRequestDto toRequestDto(EventRequest request) {
        if (request == null) {
            return null;
        }
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }
}
