import axiosClient from './axiosClient';
import type { ProjectRequest, ProjectResponse } from '../types';

const basePath = (workspaceId: string) => `/workspaces/${workspaceId}/projects`;

export const projectApi = {
    getAll: (workspaceId: string) =>
        axiosClient.get<ProjectResponse[]>(basePath(workspaceId)).then((res) => res.data),

    getByKey: (workspaceId: string, projectKey: string) =>
        axiosClient
            .get<ProjectResponse>(`${basePath(workspaceId)}/${projectKey}`)
            .then((res) => res.data),

    create: (workspaceId: string, data: ProjectRequest) =>
        axiosClient.post<ProjectResponse>(basePath(workspaceId), data).then((res) => res.data),

    update: (workspaceId: string, projectKey: string, data: ProjectRequest) =>
        axiosClient
            .put<ProjectResponse>(`${basePath(workspaceId)}/${projectKey}`, data)
            .then((res) => res.data),

    delete: (workspaceId: string, projectKey: string) =>
        axiosClient.delete(`${basePath(workspaceId)}/${projectKey}`),
};
