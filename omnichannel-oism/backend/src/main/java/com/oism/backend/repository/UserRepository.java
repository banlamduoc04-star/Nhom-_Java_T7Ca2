package com.oism.backend.repository;

import com.oism.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository thao tác với bảng users
 */
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm kiếm người dùng theo username
    Optional<User> findByUsername(String username);
}
