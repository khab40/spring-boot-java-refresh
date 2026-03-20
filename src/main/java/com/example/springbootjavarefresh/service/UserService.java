package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.CreateUserRequest;
import com.example.springbootjavarefresh.dto.UpdateUserProfileRequest;
import com.example.springbootjavarefresh.entity.AuthProvider;
import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByProviderSubject(AuthProvider provider, String providerSubject) {
        return userRepository.findByAuthProviderAndProviderSubject(provider, providerSubject);
    }

    public User createUser(CreateUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User already exists for email: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompany(request.getCompany());
        user.setCountry(request.getCountry());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setProviderSubject(null);
        user.setEmailVerified(Boolean.FALSE);
        user.setEmailVerifiedAt(null);
        return userRepository.save(user);
    }

    public User upsertGoogleUser(String email, String firstName, String lastName, String providerSubject) {
        Optional<User> byProvider = userRepository.findByAuthProviderAndProviderSubject(AuthProvider.GOOGLE, providerSubject);
        if (byProvider.isPresent()) {
            User existing = byProvider.get();
            existing.setEmail(email);
            existing.setFirstName(firstName);
            existing.setLastName(lastName);
            existing.setEmailVerified(Boolean.TRUE);
            if (existing.getEmailVerifiedAt() == null) {
                existing.setEmailVerifiedAt(java.time.LocalDateTime.now());
            }
            return userRepository.save(existing);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setFirstName(firstName);
            existing.setLastName(lastName);
            existing.setAuthProvider(AuthProvider.GOOGLE);
            existing.setProviderSubject(providerSubject);
            existing.setEmailVerified(Boolean.TRUE);
            if (existing.getEmailVerifiedAt() == null) {
                existing.setEmailVerifiedAt(java.time.LocalDateTime.now());
            }
            return userRepository.save(existing);
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(null);
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderSubject(providerSubject);
        user.setEmailVerified(Boolean.TRUE);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    public User updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id: " + userId));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCompany(request.getCompany());
        user.setCountry(request.getCountry());
        user.setPhoneNumber(request.getPhoneNumber());
        return userRepository.save(user);
    }
}
