package org.bunnys.database.services;

import org.bunnys.database.models.User;
import org.bunnys.database.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User createOrUpdateUser(String userID, String username) {
        Optional<User> existingUser = userRepository.findByUserID(userID);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setUsername(username);
        } else {
            user = new User(userID, username);
        }

        return userRepository.save(user);
    }

    public Optional<User> findByUserID(String userID) {
        return userRepository.findByUserID(userID);
    }

    public boolean userExists(String userID) {
        return userRepository.existsByUserID(userID);
    }

    public List<User> getUsersFromGuild(String guildID) {
        return userRepository.findByGuildID(guildID);
    }
}