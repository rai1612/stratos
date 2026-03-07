/* ─── Enums ────────────────────────────────────────────────── */

export enum WorkspaceRole {
    OWNER = 'OWNER',
    ADMIN = 'ADMIN',
    MEMBER = 'MEMBER',
    VIEWER = 'VIEWER',
}

export enum InvitationStatus {
    PENDING = 'PENDING',
    ACCEPTED = 'ACCEPTED',
    DECLINED = 'DECLINED',
    EXPIRED = 'EXPIRED',
}

export enum TaskStatus {
    TODO = 'TODO',
    IN_PROGRESS = 'IN_PROGRESS',
    DONE = 'DONE',
}

export enum TaskPriority {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH',
    URGENT = 'URGENT',
}

/* ─── Auth ─────────────────────────────────────────────────── */

export interface LoginRequest {
    username: string;
    password: string;
}

export interface SignupRequest {
    username: string;
    email: string;
    password: string;
}

export interface JwtResponse {
    token: string;
    type: string;
    id: string;
    username: string;
    email: string;
}

export interface MessageResponse {
    message: string;
}

/* ─── User ─────────────────────────────────────────────────── */

export interface UserResponse {
    id: string;
    username: string;
    email: string;
}

export interface UserRequest {
    username?: string;
    email?: string;
    password?: string;
}

/* ─── Workspace ────────────────────────────────────────────── */

export interface WorkspaceResponse {
    id: string;
    name: string;
    description: string | null;
    role: WorkspaceRole;
}

export interface WorkspaceRequest {
    name: string;
    description?: string;
}

export interface WorkspaceMemberResponse {
    id: string;
    userId: string;
    username: string;
    email: string;
    role: WorkspaceRole;
    joinedAt: string;
}

export interface WorkspaceMemberRequest {
    email: string;
    role: WorkspaceRole;
}

export interface WorkspaceMemberUpdateRoleRequest {
    role: WorkspaceRole;
}

export interface WorkspaceInvitationResponse {
    id: string;
    workspaceId: string;
    workspaceName: string;
    email: string;
    token: string;
    role: WorkspaceRole;
    status: InvitationStatus;
    invitedById: string;
    invitedAt: string;
    expiresAt: string;
}

/* ─── Project ──────────────────────────────────────────────── */

export interface ProjectResponse {
    id: string;
    name: string;
    projectKey: string;
    workspaceId: string;
}

export interface ProjectRequest {
    name: string;
    projectKey: string;
}

/* ─── Task ─────────────────────────────────────────────────── */

export interface TaskResponse {
    id: string;
    taskNumber: number;
    taskKey: string;
    title: string;
    description: string | null;
    status: TaskStatus;
    priority: TaskPriority;
    dueDate: string | null;
    assigneeId: string | null;
    assigneeUsername: string | null;
    projectId: string;
    projectName: string;
}

export interface TaskRequest {
    title?: string;
    description?: string;
    status?: TaskStatus;
    priority?: TaskPriority;
    dueDate?: string | null;
    assigneeId?: string | null;
}

export interface TaskSearchCriteria {
    status?: TaskStatus;
    priority?: TaskPriority;
    assigneeId?: string;
    dueDateStart?: string;
    dueDateEnd?: string;
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
}

/* ─── Auth Context ─────────────────────────────────────────── */

export interface AuthUser {
    id: string;
    username: string;
    email: string;
    token: string;
}
