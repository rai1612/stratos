import axiosClient from './axiosClient';
import type { Page, TaskRequest, TaskResponse, TaskSearchCriteria, TaskStatus } from '../types';

const basePath = (workspaceId: string, projectKey: string) =>
    `/workspaces/${workspaceId}/projects/${projectKey}/tasks`;

export const taskApi = {
    getAll: (workspaceId: string, projectKey: string) =>
        axiosClient.get<TaskResponse[]>(basePath(workspaceId, projectKey)).then((res) => res.data),

    getByNumber: (workspaceId: string, projectKey: string, taskNumber: number) =>
        axiosClient
            .get<TaskResponse>(`${basePath(workspaceId, projectKey)}/${taskNumber}`)
            .then((res) => res.data),

    create: (workspaceId: string, projectKey: string, data: TaskRequest) =>
        axiosClient
            .post<TaskResponse>(basePath(workspaceId, projectKey), data)
            .then((res) => res.data),

    update: (workspaceId: string, projectKey: string, taskNumber: number, data: TaskRequest) =>
        axiosClient
            .put<TaskResponse>(`${basePath(workspaceId, projectKey)}/${taskNumber}`, data)
            .then((res) => res.data),

    updateStatus: (
        workspaceId: string,
        projectKey: string,
        taskNumber: number,
        status: TaskStatus
    ) =>
        axiosClient
            .patch<TaskResponse>(
                `${basePath(workspaceId, projectKey)}/${taskNumber}/status?status=${status}`
            )
            .then((res) => res.data),

    search: (
        workspaceId: string,
        projectKey: string,
        criteria: TaskSearchCriteria,
        page: number = 0,
        size: number = 20
    ) => {
        const params = new URLSearchParams();
        if (criteria.status) params.append('status', criteria.status);
        if (criteria.priority) params.append('priority', criteria.priority);
        if (criteria.assigneeId) params.append('assigneeId', criteria.assigneeId);
        if (criteria.dueDateStart) params.append('dueDateStart', criteria.dueDateStart);
        if (criteria.dueDateEnd) params.append('dueDateEnd', criteria.dueDateEnd);
        params.append('page', String(page));
        params.append('size', String(size));

        return axiosClient
            .get<Page<TaskResponse>>(`${basePath(workspaceId, projectKey)}/search?${params}`)
            .then((res) => res.data);
    },

    delete: (workspaceId: string, projectKey: string, taskNumber: number) =>
        axiosClient.delete(`${basePath(workspaceId, projectKey)}/${taskNumber}`),
};
