package com.civicdesk.module.iam.repository;

import com.civicdesk.module.iam.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        User user = new User();
        user.setName("Ravi Kumar");
        user.setEmail("ravi@example.com");
        user.setPasswordHash("hashed");
        user.setRole("CIT");
        user.setStatus("A");
        userRepository.save(user);
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("ravi@example.com");
        assertTrue(result.isPresent());
        assertEquals("Ravi Kumar", result.get().getName());
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        assertTrue(userRepository.existsByEmail("ravi@example.com"));
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertFalse(userRepository.existsByEmail("unknown@example.com"));
    }
}
