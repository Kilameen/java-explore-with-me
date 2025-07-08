package ru.practicum.ewm.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Collection;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateResult {
    Collection<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
    Collection<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
}
