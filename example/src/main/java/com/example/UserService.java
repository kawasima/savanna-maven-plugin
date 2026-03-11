package com.example;

import java.util.HashMap;
import java.util.Map;

public class UserService {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    public User create(String name, String email) {
        User user = new User(nextId++, name, email);
        users.put(user.getId(), user);
        return user;
    }

    public User findById(long id) {
        return users.get(id);
    }

    public User update(long id, String name) {
        User user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        user.setName(name);
        return user;
    }

    public boolean delete(long id) {
        return users.remove(id) != null;
    }

    public int count() {
        return users.size();
    }
}
