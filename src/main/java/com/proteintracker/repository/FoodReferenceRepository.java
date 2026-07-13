package com.proteintracker.repository;

import com.proteintracker.entity.FoodReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodReferenceRepository extends JpaRepository<FoodReference, Long> {
    Optional<FoodReference> findByNameIgnoreCase(String name);
}
