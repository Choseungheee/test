package com.posco.web.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    boolean existsByNickName(String nickName);

    boolean existsByEmail(String email);

    void deleteById(String id);

    UserEntity findByEmail(String email);
}
