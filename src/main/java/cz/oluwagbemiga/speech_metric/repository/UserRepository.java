package cz.oluwagbemiga.speech_metric.repository;


import cz.oluwagbemiga.speech_metric.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Modifying
    @Query("delete from User u where u.username = :username")
    int deleteByUsername(@Param("username") String username);

    Optional<User> findByUsername(String username);
}
