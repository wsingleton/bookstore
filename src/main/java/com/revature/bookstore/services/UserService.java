package com.revature.bookstore.services;

import com.revature.bookstore.datasource.documents.AppUser;
import com.revature.bookstore.datasource.repos.UserRepository;
import com.revature.bookstore.util.PasswordUtils;
import com.revature.bookstore.util.exceptions.AuthenticationException;
import com.revature.bookstore.util.exceptions.InvalidRequestException;
import com.revature.bookstore.util.exceptions.ResourceNotFoundException;
import com.revature.bookstore.util.exceptions.ResourcePersistenceException;
import com.revature.bookstore.web.dtos.AppUserDTO;
import com.revature.bookstore.web.dtos.Principal;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserService {

    private final UserRepository userRepo;
    private final PasswordUtils passwordUtils;

    public UserService(UserRepository userRepo, PasswordUtils passwordUtils) {
        this.userRepo = userRepo;
        this.passwordUtils = passwordUtils;
    }

    public List<AppUserDTO> findAll() {
        return userRepo.findAll()
                       .stream()
                       .map(AppUserDTO::new)
                       .collect(Collectors.toList());
    }

    public AppUserDTO findUserById(String id) {

        if (id == null || id.trim().isEmpty()) {
            throw new InvalidRequestException("Invalid id provided");
        }

        AppUser user = userRepo.findById(id);

        if (user == null) {
            throw new ResourceNotFoundException();
        }

        return new AppUserDTO(user);

    }

    public AppUser register(AppUser newUser) {

        if (!isUserValid(newUser)) {
            throw new InvalidRequestException("Invalid user data provided!");
        }

        if (userRepo.findUserByUsername(newUser.getUsername()) != null) {
            throw new ResourcePersistenceException("Provided username is already taken!");
        }

        if (userRepo.findUserByEmail(newUser.getEmail()) != null) {
            throw new ResourcePersistenceException("Provided username is already taken!");
        }

        newUser.setRegistrationDateTime(LocalDateTime.now());
        String encryptedPassword = passwordUtils.generateSecurePassword(newUser.getPassword());
        newUser.setPassword(encryptedPassword);

        return userRepo.save(newUser);

    }

    public Principal login(String username, String password) {

        if (username == null || username.trim().equals("") || password == null || password.trim().equals("")) {
            throw new InvalidRequestException("Invalid user credentials provided!");
        }

        String encryptedPassword = passwordUtils.generateSecurePassword(password);
        AppUser authUser = userRepo.findUserByCredentials(username, encryptedPassword);

        if (authUser == null) {
            throw new AuthenticationException("Invalid credentials provided!");
        }

        return new Principal(authUser);

    }

    public boolean isUserValid(AppUser user) {
        if (user == null) return false;
        if (user.getFirstName() == null || user.getFirstName().trim().equals("")) return false;
        if (user.getLastName() == null || user.getLastName().trim().equals("")) return false;
        if (user.getEmail() == null || user.getEmail().trim().equals("")) return false;
        if (user.getUsername() == null || user.getUsername().trim().equals("")) return false;
        return user.getPassword() != null && !user.getPassword().trim().equals("");
    }

}
