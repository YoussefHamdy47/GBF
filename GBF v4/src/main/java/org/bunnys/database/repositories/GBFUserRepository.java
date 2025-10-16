package org.bunnys.database.repositories;

import org.bunnys.database.models.users.GBFUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GBFUserRepository extends MongoRepository<GBFUser, String> {
    Optional<GBFUser> findByUserID(String userID);

    List<GBFUser> findByFriendsContaining(String friendID);

    boolean existsByUserID(String userID);

    void deleteByUserID(String userID);
}
