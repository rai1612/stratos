package com.stratos.user;

import com.stratos.payload.request.CreateUserRequest;
import com.stratos.payload.request.UpdateUserRequest;
import com.stratos.payload.response.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
