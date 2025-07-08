package ru.practicum.ewm.request.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.EventRequest;

@Component
public class EventRequestMapper {
    public ParticipationRequestDto toEventRequest(EventRequest request){
        return ParticipationRequestDto.builder()
                .id(request.getRequesterId())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }
}
