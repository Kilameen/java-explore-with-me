package ru.practicum.ewm.request.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.utils.enums.RequestStatus;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "requests")
public class EventRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_id")
    Long eventId;

    @Column(name = "requester_id")
    Long requesterId;

    @Enumerated(EnumType.STRING)
    RequestStatus status;
    LocalDateTime created;
}

