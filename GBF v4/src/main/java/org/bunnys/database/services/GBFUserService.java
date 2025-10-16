package org.bunnys.database.services;

import org.bunnys.database.models.users.GBFUser;
import org.bunnys.database.repositories.GBFUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("unused")
public class GBFUserService {

    private static final Logger logger = LoggerFactory.getLogger(GBFUserService.class);
    private final GBFUserRepository gbfUserRepository;

    @Autowired
    public GBFUserService(GBFUserRepository gbfUserRepository) {
        this.gbfUserRepository = gbfUserRepository;
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userID")
    public GBFUser createOrUpdateUser(String userID) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            Optional<GBFUser> existingUser = gbfUserRepository.findByUserID(userID);
            GBFUser user = existingUser.orElse(new GBFUser(userID));
            GBFUser savedUser = gbfUserRepository.save(user);

            logger.debug("Successfully created/updated user with ID: {}", userID);
            return savedUser;
        } catch (Exception e) {
            logger.error("Error creating/updating user with ID: {}", userID, e);
            throw new RuntimeException("Failed to create or update user", e);
        }
    }

    @Cacheable(value = "users", key = "#userID")
    @Transactional(readOnly = true)
    public Optional<GBFUser> findByUserID(String userID) {
        if (userID == null || userID.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return gbfUserRepository.findByUserID(userID);
        } catch (Exception e) {
            logger.error("Error finding user with ID: {}", userID, e);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public boolean userExists(String userID) {
        if (userID == null || userID.trim().isEmpty()) {
            return false;
        }

        try {
            return gbfUserRepository.existsByUserID(userID);
        } catch (Exception e) {
            logger.error("Error checking if user exists with ID: {}", userID, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<GBFUser> findUsersWithFriend(String friendID) {
        if (friendID == null || friendID.trim().isEmpty()) {
            return List.of();
        }

        try {
            return gbfUserRepository.findByFriendsContaining(friendID);
        } catch (Exception e) {
            logger.error("Error finding users with friend ID: {}", friendID, e);
            return List.of();
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userID")
    public void deleteUser(String userID) {
        if (userID == null || userID.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            if (gbfUserRepository.existsByUserID(userID)) {
                gbfUserRepository.deleteByUserID(userID);
                logger.info("Successfully deleted user with ID: {}", userID);
            } else {
                logger.warn("Attempted to delete non-existent user with ID: {}", userID);
            }
        } catch (Exception e) {
            logger.error("Error deleting user with ID: {}", userID, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    @Transactional
    @CacheEvict(value = "users", key = "#user.userID")
    public GBFUser saveUser(GBFUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUserID() == null || user.getUserID().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            return gbfUserRepository.save(user);
        } catch (Exception e) {
            logger.error("Error saving user: {}", user.getUserID(), e);
            throw new RuntimeException("Failed to save user", e);
        }
    }
}