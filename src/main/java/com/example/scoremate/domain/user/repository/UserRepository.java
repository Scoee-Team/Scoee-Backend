package com.example.scoremate.domain.user.repository;

import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
