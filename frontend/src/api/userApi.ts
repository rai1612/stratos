import axiosClient from './axiosClient';
import type { UserRequest, UserResponse, WorkspaceInvitationResponse } from '../types';

export const userApi = {
    getAll: () =>
        axiosClient.get<UserResponse[]>('/users').then((res) => res.data),

    getById: (id: string) =>
        axiosClient.get<UserResponse>(`/users/${id}`).then((res) => res.data),

    getMyInvitations: () =>
        axiosClient.get<WorkspaceInvitationResponse[]>('/users/me/invitations').then((res) => res.data),

    create: (data: UserRequest) =>
        axiosClient.post<UserResponse>('/users', data).then((res) => res.data),

    update: (id: string, data: UserRequest) =>
        axiosClient.put<UserResponse>(`/users/${id}`, data).then((res) => res.data),

    delete: (id: string) =>
        axiosClient.delete(`/users/${id}`),
};
