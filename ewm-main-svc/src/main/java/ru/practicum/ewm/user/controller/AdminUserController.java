package ru.practicum.ewm.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService service;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UserDto create(@RequestBody @Valid NewUserRequest newUserRequest) {
        log.info("POST запрос на создание пользователя: {}", newUserRequest);
        UserDto userDto = service.create(newUserRequest);
        log.info("Пользователь создан: {}", userDto);
        return userDto;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.info("DELETE запрос на удаление пользователя с id: {}", id);
        service.delete(id);
        log.info("Пользователь с id {} удален", id);
    }

    @GetMapping
    public List<UserDto> getAllUsers(@RequestParam(required = false) List<Long> ids,
                                     @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                     @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET запрос на получение списка пользователей с параметрами: ids={}, from={}, size={}", ids, from, size);
        List<UserDto> users = service.getAllUsers(ids, from, size);
        log.info("Возвращен список пользователей: {}", users);
        return users;
    }
}