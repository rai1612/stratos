package com.stratos.user;

import com.stratos.payload.request.UserRequest;
import com.stratos.payload.response.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse createUser(UserRequest request);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);
}
