package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Setting entities.
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {

    /**
     * Finds setting by its unique setting_key.
     *
     * @param key Setting key
     * @return Optional Setting entity
     */
    Optional<Setting> findByKey(String key);

    /**
     * Checks if a setting exists for the specified key.
     *
     * @param key Setting key
     * @return true if exists, false otherwise
     */
    boolean existsByKey(String key);
}
