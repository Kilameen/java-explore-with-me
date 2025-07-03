package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDto create(NewUserRequest newUserRequest) {
        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new DuplicatedDataException("Email уже зарегистрирован: " + newUserRequest.getEmail());
        }
        log.info("Создание пользователя с данными: {}", newUserRequest);
        User user = UserMapper.toNewUserFromRequest(newUserRequest);
        User createUser = userRepository.save(user);
        return UserMapper.toUserDto(createUser);
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> getAllUsers(List<Long> ids, int from, int size) {
        int page = from > 0 ? from / size : 0;
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").descending());
        return (ids != null) ? userRepository.findByIdIn(ids, pageable)
                .stream().map(UserMapper::toUserDto).collect(Collectors.toList()) : userRepository.findAll(pageable)
                .stream().map(UserMapper::toUserDto).collect(Collectors.toList());
    }
}