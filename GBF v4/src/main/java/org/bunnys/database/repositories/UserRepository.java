package org.bunnys.database.repositories;

import org.bunnys.database.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserID(String userID);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContaining(String username);

    List<User> findByGuildID(String guildID);

    boolean existsByUserID(String userID);

    void deleteByUserID(String userID);

    @Query("{'userID': ?0}")
    Optional<User> findUserByDiscordId(String userID);
}