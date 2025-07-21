package ru.practicum.ewm.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.dto.UpdateCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.DuplicatedDataException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    EventRepository eventRepository;

    @Override
    public CategoryDto create(NewCategoryDto newCategoryDto) {
        log.info("Попытка создать категорию с именем: {}", newCategoryDto.getName());
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new DuplicatedDataException("Название категории уже зарегистрировано: " + newCategoryDto.getName());
        }
        Category category = CategoryMapper.toNewCategoryFromDto(newCategoryDto);
        Category createdCategory = categoryRepository.save(category);
        log.info("Категория успешно создана с ID: {}", createdCategory.getId());
        return CategoryMapper.toCategoryDto(createdCategory);
    }

    @Override
    public void delete(Long id) {
        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("Невозможно удалить категорию, так как с ней связаны события.");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryDto update(Long catId, UpdateCategoryDto updateCategoryDto) {
        log.info("Попытка обновить категорию с ID: {}", catId);
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена!"));

        String newName = updateCategoryDto.getName().trim();
        category.setName(newName);

        categoryRepository.save(category);
        log.info("Категория с ID {} успешно обновлена.", catId);
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с ID " + catId + " не найдена!"));
        return CategoryMapper.toCategoryDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable)
                .stream().map(CategoryMapper::toCategoryDto).collect(Collectors.toList());
    }
}