package com.itmo.service;

import com.itmo.model.User;

import java.util.List;

public interface UserService {
    User findUserByEmail(String email);

    User findUserById(Long id);

    void saveUser(User user);

    void modifyUser(User user);

    List<User> findUsersByActive(boolean active);

    List<User> findAllUsers();
}
