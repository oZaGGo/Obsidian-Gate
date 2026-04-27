package com.obsidiangate.mcpanel.repository;

import com.obsidiangate.mcpanel.model.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    Optional<UserAuth> findByUsername(String username);

    Optional<UserAuth> findByToken(String token);
}
