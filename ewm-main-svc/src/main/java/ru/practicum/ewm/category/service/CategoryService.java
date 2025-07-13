package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.dto.UpdateCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto newCategoryDto);

    void delete(Long id);

    CategoryDto update(Long catId, UpdateCategoryDto updateCategoryDto);

    CategoryDto getCategoryById(Long catId);

    List<CategoryDto> getAllCategories(int from, int size);

}