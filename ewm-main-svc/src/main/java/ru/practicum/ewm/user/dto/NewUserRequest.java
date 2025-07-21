package ru.practicum.ewm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewUserRequest {
    @NotBlank(message = "Поле name не может быть пустым, заполните поле name от 2 до 250 символов!")
    @Size(min = 2, max = 250, message = "Длина name должна составлять от 2 до 250 символов!")
    String name;

    @NotBlank(message = "Поле email не может быть пустым!")
    @Size(min = 6, max = 254, message = "Длина email должна составлять от 6 до 254 символов!")
    @Email(message = "Email должен быть в подходящем формате!")
    String email;
}
