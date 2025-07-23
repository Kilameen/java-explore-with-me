package ru.practicum.ewm.subscription.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.enums.FriendshipsStatus;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "follower_id")
    User follower;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    User owner;

    @Column(name = "subscribe_time")
    LocalDateTime subscribeTime;

    @Column(name = "unsubscribe_time")
    LocalDateTime unsubscribeTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "friendships_status", nullable = false)
    FriendshipsStatus friendshipsStatus;
}

