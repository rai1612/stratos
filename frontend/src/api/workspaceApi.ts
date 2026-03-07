import axiosClient from './axiosClient';
import type {
    WorkspaceRequest,
    WorkspaceResponse,
    WorkspaceMemberResponse,
    WorkspaceMemberRequest,
    WorkspaceMemberUpdateRoleRequest,
    WorkspaceInvitationResponse
} from '../types';

export const workspaceApi = {
    getAll: () =>
        axiosClient.get<WorkspaceResponse[]>('/workspaces').then((res) => res.data),

    getById: (workspaceId: string) =>
        axiosClient.get<WorkspaceResponse>(`/workspaces/${workspaceId}`).then((res) => res.data),

    create: (data: WorkspaceRequest) =>
        axiosClient.post<WorkspaceResponse>('/workspaces', data).then((res) => res.data),

    update: (workspaceId: string, data: WorkspaceRequest) =>
        axiosClient.put<WorkspaceResponse>(`/workspaces/${workspaceId}`, data).then((res) => res.data),

    delete: (workspaceId: string) =>
        axiosClient.delete(`/workspaces/${workspaceId}`),

    getMembers: (workspaceId: string) =>
        axiosClient.get<WorkspaceMemberResponse[]>(`/workspaces/${workspaceId}/members`).then(res => res.data),

    getInvitations: (workspaceId: string) =>
        axiosClient.get<WorkspaceInvitationResponse[]>(`/workspaces/${workspaceId}/invitations`).then(res => res.data),

    inviteMember: (workspaceId: string, data: WorkspaceMemberRequest) =>
        axiosClient.post<WorkspaceInvitationResponse>(`/workspaces/${workspaceId}/invitations`, data).then(res => res.data),

    updateMemberRole: (workspaceId: string, userId: string, data: WorkspaceMemberUpdateRoleRequest) =>
        axiosClient.put<WorkspaceMemberResponse>(`/workspaces/${workspaceId}/members/${userId}/role`, data).then(res => res.data),

    removeMember: (workspaceId: string, userId: string) =>
        axiosClient.delete(`/workspaces/${workspaceId}/members/${userId}`),

    leaveWorkspace: (workspaceId: string) =>
        axiosClient.delete(`/workspaces/${workspaceId}/leave`),

    transferOwnership: (workspaceId: string, newOwnerId: string) =>
        axiosClient.put(`/workspaces/${workspaceId}/transfer-ownership/${newOwnerId}`),

    acceptInvitation: (token: string) =>
        axiosClient.post<WorkspaceMemberResponse>(`/workspaces/invitations/${token}/accept`).then(res => res.data),
};
