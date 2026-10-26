package com.smartinventory.notificationservice.repository;

import com.smartinventory.notificationservice.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Product entity within Notification Service.
 * Used for eagerly loading product supplier information when dispatching low stock alerts.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"supplier"})
    Optional<Product> findById(@NonNull Long id);
}
