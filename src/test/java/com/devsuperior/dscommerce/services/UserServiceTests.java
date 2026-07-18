package com.devsuperior.dscommerce.services;

import com.devsuperior.dscommerce.entities.User;
import com.devsuperior.dscommerce.projections.UserDetailsProjection;
import com.devsuperior.dscommerce.repositories.UserRepository;
import com.devsuperior.dscommerce.services.exceptions.DatabaseException;
import com.devsuperior.dscommerce.tests.UserDetailsFactory;
import com.devsuperior.dscommerce.tests.UserFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(SpringExtension.class)
public class UserServiceTests {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository repository;

    private String existingUsername, nonExistingUsername;
    private User user;
    private UserDetailsProjection userProjection;
    private List<UserDetailsProjection> list, emptyList;

    @BeforeEach
    void setUp() throws Exception {
        existingUsername = "maria@gmail.com";
        nonExistingUsername = "user@gmail.com";

        user = UserFactory.createCustomClientUser(1L, existingUsername);
        list = UserDetailsFactory.createCustomClientUser(existingUsername);
        emptyList = new ArrayList<>();

        Mockito.when(repository.searchUserAndRolesByEmail(existingUsername)).thenReturn(list);
        Mockito.when(repository.searchUserAndRolesByEmail(nonExistingUsername)).thenReturn(emptyList);
    }

    @Test
    public void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
        UserDetails result = service.loadUserByUsername(existingUsername);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getUsername(), existingUsername);
    }

    @Test
    public void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenUsernameDoesNotExist() {
        Assertions.assertThrows(UsernameNotFoundException.class, () -> {
            service.loadUserByUsername(nonExistingUsername);
        });
    }
}
