import axiosClient from './axiosClient';
import type { JwtResponse, LoginRequest, MessageResponse, SignupRequest } from '../types';

export const authApi = {
    login: (data: LoginRequest) =>
        axiosClient.post<JwtResponse>('/auth/login', data).then((res) => res.data),

    register: (data: SignupRequest) =>
        axiosClient.post<MessageResponse>('/auth/register', data).then((res) => res.data),
};
