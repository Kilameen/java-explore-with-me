package ru.practicum.ewm.subscription;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.enums.FriendshipsStatus;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.user.model.User;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByFollowerAndOwner(User follower, User owner);

    List<Subscription> findByFollower(User follower);

    List<Subscription> findByOwner(User owner, Pageable pageable);

    long countByOwnerAndFriendshipsStatusIn(User owner, List<FriendshipsStatus> friendshipsStatuses);
}
