package ru.practicum.ewm.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.user.model.User;

import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Collection<User> findByIdIn(Collection<Long> ids, Pageable pageable);
}
