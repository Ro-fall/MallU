package com.rofall.mallu.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MallUserRepository extends JpaRepository<MallUser, Long> {
    Optional<MallUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
