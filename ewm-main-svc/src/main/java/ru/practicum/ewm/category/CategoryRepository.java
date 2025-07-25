package ru.practicum.ewm.category;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.category.model.Category;

public interface CategoryRepository extends JpaRepository<Category,Long> {

    boolean existsByName(String name);

}
