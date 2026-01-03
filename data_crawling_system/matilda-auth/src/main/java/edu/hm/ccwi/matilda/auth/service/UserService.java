package edu.hm.ccwi.matilda.auth.service;

import edu.hm.ccwi.matilda.auth.model.User;

import java.util.List;

public interface UserService {

    List<User> getAllUsers();
    User getUserByUsername(String username);
}
