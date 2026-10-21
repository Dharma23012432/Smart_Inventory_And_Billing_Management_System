package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Category entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by exact name ignoring case.
     *
     * @param name Category name
     * @return Optional Category
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Checks if a category with the specified name exists (case-insensitive).
     *
     * @param name Category name
     * @return true if exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if another category already has this name, excluding a specific ID.
     * Used for update conflict validation.
     *
     * @param name Category name
     * @param id   Category ID to exclude
     * @return true if another category exists with this name, false otherwise
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Retrieves all categories sorted alphabetically by name.
     *
     * @return Ordered list of categories
     */
    List<Category> findAllByOrderByNameAsc();
}
