package com.example.bt2.repository;

import com.example.bt2.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByUserId(String userId);
    void deleteByUserId(String userId);
}
