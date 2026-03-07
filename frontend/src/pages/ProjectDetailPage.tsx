import { useEffect, useState, useCallback, useRef, type DragEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { taskApi } from '../api/taskApi';
import { projectApi } from '../api/projectApi';
import { workspaceApi } from '../api/workspaceApi';
import type { ProjectResponse, TaskRequest, TaskResponse, TaskSearchCriteria, WorkspaceMemberResponse, WorkspaceResponse } from '../types';
import { TaskStatus, TaskPriority, WorkspaceRole } from '../types';
import {
    Plus,
    X,
    ListTodo,
    Clock,
    CheckCircle2,
    Calendar,
    Trash2,
    Pencil,
    Search,
} from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { useToast } from '../context/ToastContext';

const STATUS_CONFIG = {
    [TaskStatus.TODO]: { label: 'To Do', icon: ListTodo, colorVar: '--status-todo' },
    [TaskStatus.IN_PROGRESS]: { label: 'In Progress', icon: Clock, colorVar: '--status-in-progress' },
    [TaskStatus.DONE]: { label: 'Done', icon: CheckCircle2, colorVar: '--status-done' },
};

const PRIORITY_LABELS: Record<TaskPriority, string> = {
    [TaskPriority.LOW]: 'Low',
    [TaskPriority.MEDIUM]: 'Medium',
    [TaskPriority.HIGH]: 'High',
    [TaskPriority.URGENT]: 'Urgent',
};

export default function ProjectDetailPage() {
    const { workspaceId, projectKey } = useParams<{
        workspaceId: string;
        projectKey: string;
    }>();
    const wsId = workspaceId!;
    const pKey = projectKey!;
    const navigate = useNavigate();

    const [workspace, setWorkspace] = useState<WorkspaceResponse | null>(null);
    const [project, setProject] = useState<ProjectResponse | null>(null);
    const [tasks, setTasks] = useState<TaskResponse[]>([]);
    const [members, setMembers] = useState<WorkspaceMemberResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const { addToast } = useToast();

    // Task form modal
    const [showModal, setShowModal] = useState(false);
    const [editingTask, setEditingTask] = useState<TaskResponse | null>(null);
    const [formData, setFormData] = useState<TaskRequest>({
        title: '',
        description: '',
        status: TaskStatus.TODO,
        priority: TaskPriority.MEDIUM,
        dueDate: null,
        assigneeId: null,
    });
    const [submitting, setSubmitting] = useState(false);

    // Filter state
    const [filterPriority, setFilterPriority] = useState<TaskPriority | ''>('');
    const [filterStatus, setFilterStatus] = useState<TaskStatus | ''>('');

    // Search state
    const [useSearch, setUseSearch] = useState(false);
    const [searchCriteria, setSearchCriteria] = useState<TaskSearchCriteria>({});
    const [searchPage, setSearchPage] = useState(0);
    const [searchTotalPages, setSearchTotalPages] = useState(0);
    const [searchLoading, setSearchLoading] = useState(false);

    // Drag-and-drop state
    const [draggingTaskNumber, setDraggingTaskNumber] = useState<number | null>(null);
    const [dragOverStatus, setDragOverStatus] = useState<TaskStatus | null>(null);
    const dragCounterRef = useRef<Record<string, number>>({});

    // Task detail
    const [detailTask, setDetailTask] = useState<TaskResponse | null>(null);
    const [showDetail, setShowDetail] = useState(false);

    const fetchData = useCallback(async () => {
        try {
            const [ws, proj, taskList, memberList] = await Promise.all([
                workspaceApi.getById(wsId),
                projectApi.getByKey(wsId, pKey),
                taskApi.getAll(wsId, pKey),
                workspaceApi.getMembers(wsId),
            ]);
            setWorkspace(ws);
            setProject(proj);
            setTasks(taskList);
            setMembers(memberList);
        } catch (err: any) {
            if (err.response?.status === 403 || err.response?.status === 404) {
                addToast(err.response?.data?.message || 'Access denied', 'error');
                navigate('/workspaces', { replace: true });
                return;
            }
            addToast(err.response?.data?.message || 'Failed to load project data');
        } finally {
            setLoading(false);
        }
    }, [wsId, pKey]);

    useEffect(() => {
        let ignore = false;

        const load = async () => {
            try {
                const [ws, proj, taskList, memberList] = await Promise.all([
                    workspaceApi.getById(wsId),
                    projectApi.getByKey(wsId, pKey),
                    taskApi.getAll(wsId, pKey),
                    workspaceApi.getMembers(wsId),
                ]);
                if (ignore) return;
                setWorkspace(ws);
                setProject(proj);
                setTasks(taskList);
                setMembers(memberList);
            } catch (err: any) {
                if (ignore) return;
                if (err.response?.status === 403 || err.response?.status === 404) {
                    addToast(err.response?.data?.message || 'Access denied', 'error');
                    navigate('/workspaces', { replace: true });
                    return;
                }
                addToast(err.response?.data?.message || 'Failed to load project data');
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        load();
        return () => { ignore = true; };
    }, [wsId, pKey]);

    // Search tasks using the search endpoint
    const handleSearch = async (page = 0) => {
        setSearchLoading(true);
        try {
            const result = await taskApi.search(wsId, pKey, searchCriteria, page, 20);
            setTasks(result.content);
            setSearchPage(result.number);
            setSearchTotalPages(result.totalPages);
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Search failed');
        } finally {
            setSearchLoading(false);
        }
    };

    const toggleSearch = () => {
        if (useSearch) {
            setUseSearch(false);
            setSearchCriteria({});
            fetchData(); // reload all tasks
        } else {
            setUseSearch(true);
        }
    };

    // View task detail
    const handleViewDetail = async (task: TaskResponse) => {
        try {
            const detail = await taskApi.getByNumber(wsId, pKey, task.taskNumber);
            setDetailTask(detail);
            setShowDetail(true);
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to load task details');
        }
    };

    const openCreate = (initialStatus: TaskStatus = TaskStatus.TODO) => {
        setEditingTask(null);
        setFormData({
            title: '',
            description: '',
            status: initialStatus,
            priority: TaskPriority.MEDIUM,
            dueDate: null,
            assigneeId: null,
        });
        setShowModal(true);
    };

    const openEdit = (task: TaskResponse) => {
        setEditingTask(task);
        setFormData({
            title: task.title,
            description: task.description || '',
            status: task.status,
            priority: task.priority,
            dueDate: task.dueDate,
            assigneeId: task.assigneeId,
        });
        setShowModal(true);
    };

    const handleSubmit = async () => {
        if (!formData.title?.trim()) return;
        setSubmitting(true);
        try {
            if (editingTask) {
                await taskApi.update(wsId, pKey, editingTask.taskNumber, formData);
            } else {
                await taskApi.create(wsId, pKey, formData);
            }
            setShowModal(false);
            if (useSearch) handleSearch(searchPage);
            else fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to save task');
        } finally {
            setSubmitting(false);
        }
    };

    const handleStatusChange = async (task: TaskResponse, newStatus: TaskStatus) => {
        try {
            await taskApi.updateStatus(wsId, pKey, task.taskNumber, newStatus);
            if (useSearch) handleSearch(searchPage);
            else fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to update status');
        }
    };

    const handleDelete = async (task: TaskResponse) => {
        if (!confirm(`Delete task ${task.taskKey}?`)) return;
        try {
            await taskApi.delete(wsId, pKey, task.taskNumber);
            if (useSearch) handleSearch(searchPage);
            else fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to delete task');
        }
    };

    // Filter tasks (client-side, for the Kanban view)
    const filteredTasks = tasks.filter((t) => {
        if (filterPriority && t.priority !== filterPriority) return false;
        if (filterStatus && t.status !== filterStatus) return false;
        return true;
    });

    // Group tasks by status
    const columns = Object.values(TaskStatus).map((status) => ({
        status,
        tasks: filteredTasks.filter((t) => t.status === status),
        config: STATUS_CONFIG[status],
    }));

    // Drag-and-drop handlers
    const handleDragStart = (e: DragEvent, taskNumber: number) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        setDraggingTaskNumber(taskNumber);
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', String(taskNumber));
        requestAnimationFrame(() => {
            (e.target as HTMLElement).style.opacity = '0.4';
        });
    };

    const handleDragEnd = (e: DragEvent) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        (e.target as HTMLElement).style.opacity = '1';
        setDraggingTaskNumber(null);
        setDragOverStatus(null);
        dragCounterRef.current = {};
    };

    const handleDragOver = (e: DragEvent) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
    };

    const handleDragEnter = (e: DragEvent, status: TaskStatus) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        e.preventDefault();
        dragCounterRef.current[status] = (dragCounterRef.current[status] || 0) + 1;
        setDragOverStatus(status);
    };

    const handleDragLeave = (_e: DragEvent, status: TaskStatus) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        dragCounterRef.current[status] = (dragCounterRef.current[status] || 0) - 1;
        if (dragCounterRef.current[status] <= 0) {
            dragCounterRef.current[status] = 0;
            if (dragOverStatus === status) {
                setDragOverStatus(null);
            }
        }
    };

    const handleDrop = async (e: DragEvent, targetStatus: TaskStatus) => {
        if (workspace?.role === WorkspaceRole.VIEWER) return;
        e.preventDefault();
        setDragOverStatus(null);
        dragCounterRef.current = {};

        const taskNumber = Number(e.dataTransfer.getData('text/plain'));
        const task = tasks.find((t) => t.taskNumber === taskNumber);
        if (!task || task.status === targetStatus) return;

        try {
            await taskApi.updateStatus(wsId, pKey, taskNumber, targetStatus);
            fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to move task');
        }
    };

    if (loading) {
        return (
            <div className="loading-screen">
                <div className="spinner" />
            </div>
        );
    }

    return (
        <div>
            <div className="page-header">
                <div>
                    <h1 className="page-title">
                        <span className="project-card-key" style={{ marginRight: 12, fontSize: 'var(--font-size-sm)' }}>
                            {project?.projectKey}
                        </span>
                        {project?.name}
                    </h1>
                    <p className="page-subtitle">
                        {tasks.length} task{tasks.length !== 1 ? 's' : ''} total
                    </p>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                    <button
                        className={`btn ${useSearch ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={toggleSearch}
                        title="Toggle search mode"
                    >
                        <Search size={16} /> {useSearch ? 'Board View' : 'Search'}
                    </button>
                    {workspace?.role !== WorkspaceRole.VIEWER && (
                        <button className="btn btn-primary" onClick={() => openCreate()}>
                            <Plus size={16} /> New Task
                        </button>
                    )}
                </div>
            </div>

            {/* Search Panel */}
            {useSearch && (
                <div className="card" style={{ marginBottom: 'var(--space-lg)' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 'var(--space-md)' }}>
                        <div className="form-group" style={{ margin: 0 }}>
                            <label style={{ fontSize: 'var(--font-size-xs)' }}>Status</label>
                            <select
                                className="form-select"
                                value={searchCriteria.status || ''}
                                onChange={(e) =>
                                    setSearchCriteria({ ...searchCriteria, status: (e.target.value || undefined) as TaskStatus | undefined })
                                }
                            >
                                <option value="">Any</option>
                                {Object.values(TaskStatus).map((s) => (
                                    <option key={s} value={s}>{STATUS_CONFIG[s].label}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group" style={{ margin: 0 }}>
                            <label style={{ fontSize: 'var(--font-size-xs)' }}>Priority</label>
                            <select
                                className="form-select"
                                value={searchCriteria.priority || ''}
                                onChange={(e) =>
                                    setSearchCriteria({ ...searchCriteria, priority: (e.target.value || undefined) as TaskPriority | undefined })
                                }
                            >
                                <option value="">Any</option>
                                {Object.values(TaskPriority).map((p) => (
                                    <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group" style={{ margin: 0 }}>
                            <label style={{ fontSize: 'var(--font-size-xs)' }}>Assignee</label>
                            <select
                                className="form-select"
                                value={searchCriteria.assigneeId || ''}
                                onChange={(e) =>
                                    setSearchCriteria({ ...searchCriteria, assigneeId: e.target.value || undefined })
                                }
                            >
                                <option value="">Any</option>
                                {members.map(member => (
                                    <option key={member.userId} value={member.userId}>
                                        {member.username}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="form-group" style={{ margin: 0 }}>
                            <label style={{ fontSize: 'var(--font-size-xs)' }}>Due From</label>
                            <input
                                className="form-input"
                                type="date"
                                value={searchCriteria.dueDateStart || ''}
                                onChange={(e) =>
                                    setSearchCriteria({ ...searchCriteria, dueDateStart: e.target.value || undefined })
                                }
                            />
                        </div>
                        <div className="form-group" style={{ margin: 0 }}>
                            <label style={{ fontSize: 'var(--font-size-xs)' }}>Due Until</label>
                            <input
                                className="form-input"
                                type="date"
                                value={searchCriteria.dueDateEnd || ''}
                                onChange={(e) =>
                                    setSearchCriteria({ ...searchCriteria, dueDateEnd: e.target.value || undefined })
                                }
                            />
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, marginTop: 'var(--space-md)' }}>
                        <button className="btn btn-primary" onClick={() => handleSearch(0)} disabled={searchLoading}>
                            {searchLoading ? 'Searching…' : 'Search'}
                        </button>
                        <button className="btn btn-secondary" onClick={() => { setSearchCriteria({}); }}>
                            Clear
                        </button>
                    </div>
                    {/* Search pagination */}
                    {searchTotalPages > 1 && (
                        <div style={{ display: 'flex', gap: 8, marginTop: 'var(--space-md)', alignItems: 'center' }}>
                            <button
                                className="btn btn-ghost btn-sm"
                                disabled={searchPage === 0}
                                onClick={() => handleSearch(searchPage - 1)}
                            >
                                ← Prev
                            </button>
                            <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--text-secondary)' }}>
                                Page {searchPage + 1} of {searchTotalPages}
                            </span>
                            <button
                                className="btn btn-ghost btn-sm"
                                disabled={searchPage >= searchTotalPages - 1}
                                onClick={() => handleSearch(searchPage + 1)}
                            >
                                Next →
                            </button>
                        </div>
                    )}
                </div>
            )}

            {/* When in search mode, show results as a list */}
            {useSearch ? (
                <div>
                    {tasks.length === 0 ? (
                        <div className="empty-state">
                            <Search />
                            <h3>No results</h3>
                            <p>Try adjusting your search criteria.</p>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                            {tasks.map((task) => (
                                <TaskCard
                                    key={task.id}
                                    task={task}
                                    isDragging={false}
                                    onDragStart={() => { }}
                                    onDragEnd={() => { }}
                                    onEdit={() => openEdit(task)}
                                    onDelete={() => handleDelete(task)}
                                    onStatusChange={(newStatus) => handleStatusChange(task, newStatus)}
                                    onViewDetail={() => handleViewDetail(task)}
                                    canEdit={workspace?.role !== WorkspaceRole.VIEWER}
                                />
                            ))}
                        </div>
                    )}
                </div>
            ) : (
                <>
                    {/* Filters */}
                    <div className="task-filters">
                        <select
                            className="form-select"
                            value={filterPriority}
                            onChange={(e) => setFilterPriority(e.target.value as TaskPriority | '')}
                        >
                            <option value="">All Priorities</option>
                            {Object.values(TaskPriority).map((p) => (
                                <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>
                            ))}
                        </select>
                        <select
                            className="form-select"
                            value={filterStatus}
                            onChange={(e) => setFilterStatus(e.target.value as TaskStatus | '')}
                        >
                            <option value="">All Statuses</option>
                            {Object.values(TaskStatus).map((s) => (
                                <option key={s} value={s}>{STATUS_CONFIG[s].label}</option>
                            ))}
                        </select>
                    </div>

                    {/* Kanban Board */}
                    <div className="task-board">
                        {columns.map(({ status, tasks: colTasks, config }) => {
                            const Icon = config.icon;
                            return (
                                <div
                                    key={status}
                                    className={`task-column ${dragOverStatus === status ? 'drag-over' : ''}`}
                                    onDragOver={workspace?.role !== WorkspaceRole.VIEWER ? handleDragOver : undefined}
                                    onDragEnter={workspace?.role !== WorkspaceRole.VIEWER ? (e) => handleDragEnter(e, status) : undefined}
                                    onDragLeave={workspace?.role !== WorkspaceRole.VIEWER ? (e) => handleDragLeave(e, status) : undefined}
                                    onDrop={workspace?.role !== WorkspaceRole.VIEWER ? (e) => handleDrop(e, status) : undefined}
                                >
                                    <div className="task-column-header">
                                        <div className="task-column-title">
                                            <Icon
                                                size={16}
                                                style={{ color: `var(${config.colorVar})` }}
                                            />
                                            {config.label}
                                            <span className="task-column-count">{colTasks.length}</span>
                                        </div>
                                        {workspace?.role !== WorkspaceRole.VIEWER && (
                                            <button
                                                className="btn btn-ghost btn-icon btn-sm"
                                                onClick={() => openCreate(status)}
                                                title={`Add ${config.label} task`}
                                            >
                                                <Plus size={14} />
                                            </button>
                                        )}
                                    </div>
                                    <div className="task-column-body">
                                        {colTasks.length === 0 && (
                                            <div style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                flex: 1,
                                                color: 'var(--text-tertiary)',
                                                fontSize: 'var(--font-size-xs)',
                                            }}>
                                                {draggingTaskNumber ? 'Drop here' : 'No tasks'}
                                            </div>
                                        )}
                                        {colTasks.map((task) => (
                                            <TaskCard
                                                key={task.id}
                                                task={task}
                                                isDragging={draggingTaskNumber === task.taskNumber}
                                                onDragStart={workspace?.role !== WorkspaceRole.VIEWER ? (e) => handleDragStart(e, task.taskNumber) : () => { }}
                                                onDragEnd={workspace?.role !== WorkspaceRole.VIEWER ? handleDragEnd : () => { }}
                                                onEdit={() => openEdit(task)}
                                                onDelete={() => handleDelete(task)}
                                                onStatusChange={(newStatus) => handleStatusChange(task, newStatus)}
                                                onViewDetail={() => handleViewDetail(task)}
                                                canEdit={workspace?.role !== WorkspaceRole.VIEWER}
                                            />
                                        ))}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </>
            )}

            {/* Task Form Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{editingTask ? `Edit ${editingTask.taskKey}` : 'New Task'}</h2>
                            <button className="btn btn-ghost btn-icon" onClick={() => setShowModal(false)}>
                                <X size={18} />
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label htmlFor="task-title">Title</label>
                                <input
                                    id="task-title"
                                    className="form-input"
                                    placeholder="What needs to be done?"
                                    value={formData.title || ''}
                                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="task-desc">Description</label>
                                <textarea
                                    id="task-desc"
                                    className="form-textarea"
                                    placeholder="Add more details…"
                                    value={formData.description || ''}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                />
                            </div>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
                                <div className="form-group">
                                    <label htmlFor="task-status">Status</label>
                                    <select
                                        id="task-status"
                                        className="form-select"
                                        value={formData.status || TaskStatus.TODO}
                                        onChange={(e) =>
                                            setFormData({ ...formData, status: e.target.value as TaskStatus })
                                        }
                                    >
                                        {Object.values(TaskStatus).map((s) => (
                                            <option key={s} value={s}>{STATUS_CONFIG[s].label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label htmlFor="task-priority">Priority</label>
                                    <select
                                        id="task-priority"
                                        className="form-select"
                                        value={formData.priority || TaskPriority.MEDIUM}
                                        onChange={(e) =>
                                            setFormData({ ...formData, priority: e.target.value as TaskPriority })
                                        }
                                    >
                                        {Object.values(TaskPriority).map((p) => (
                                            <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                            <div className="form-group">
                                <label>Due Date</label>
                                <DatePicker
                                    selected={formData.dueDate ? new Date(formData.dueDate) : null}
                                    onChange={(date: Date | null) =>
                                        setFormData({
                                            ...formData,
                                            dueDate: date ? date.toISOString().split('T')[0] : null,
                                        })
                                    }
                                    dateFormat="yyyy-MM-dd"
                                    placeholderText="Select due date"
                                    minDate={new Date()}
                                    isClearable
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="task-assignee">Assignee</label>
                                <select
                                    id="task-assignee"
                                    className="form-select"
                                    value={formData.assigneeId || ''}
                                    onChange={(e) =>
                                        setFormData({
                                            ...formData,
                                            assigneeId: e.target.value || null,
                                        })
                                    }
                                >
                                    <option value="">Unassigned</option>
                                    {members.map(member => (
                                        <option key={member.userId} value={member.userId}>
                                            {member.username} ({member.email})
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                Cancel
                            </button>
                            <button
                                className="btn btn-primary"
                                onClick={handleSubmit}
                                disabled={submitting || !formData.title?.trim()}
                            >
                                {submitting ? 'Saving…' : editingTask ? 'Update' : 'Create'}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Task Detail Modal */}
            {showDetail && detailTask && (
                <div className="modal-overlay" onClick={() => setShowDetail(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{detailTask.taskKey}</h2>
                            <button className="btn btn-ghost btn-icon" onClick={() => setShowDetail(false)}>
                                <X size={18} />
                            </button>
                        </div>
                        <div className="modal-body">
                            <h3 style={{ margin: '0 0 var(--space-sm)' }}>{detailTask.title}</h3>
                            {detailTask.description && (
                                <p style={{ color: 'var(--text-secondary)', marginBottom: 'var(--space-md)' }}>
                                    {detailTask.description}
                                </p>
                            )}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-sm)', fontSize: 'var(--font-size-sm)' }}>
                                <div>
                                    <strong>Status:</strong>{' '}
                                    <span className={`badge badge-priority-${detailTask.status}`}>
                                        {STATUS_CONFIG[detailTask.status].label}
                                    </span>
                                </div>
                                <div>
                                    <strong>Priority:</strong>{' '}
                                    <span className={`badge badge-priority-${detailTask.priority}`}>
                                        {PRIORITY_LABELS[detailTask.priority]}
                                    </span>
                                </div>
                                {detailTask.dueDate && (
                                    <div><strong>Due Date:</strong> {detailTask.dueDate}</div>
                                )}
                                {detailTask.assigneeUsername && (
                                    <div><strong>Assignee:</strong> {detailTask.assigneeUsername}</div>
                                )}
                                <div><strong>Project:</strong> {detailTask.projectName}</div>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setShowDetail(false)}>
                                Close
                            </button>
                            {workspace?.role !== WorkspaceRole.VIEWER && (
                                <button
                                    className="btn btn-primary"
                                    onClick={() => { setShowDetail(false); openEdit(detailTask); }}
                                >
                                    Edit
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

/* ─── Task Card Sub-component ─────────────────────────────── */

interface TaskCardProps {
    task: TaskResponse;
    isDragging: boolean;
    onDragStart: (e: DragEvent<HTMLDivElement>) => void;
    onDragEnd: (e: DragEvent<HTMLDivElement>) => void;
    onEdit: () => void;
    onDelete: () => void;
    onStatusChange: (status: TaskStatus) => void;
    onViewDetail: () => void;
    canEdit: boolean;
}

function TaskCard({ task, isDragging, onDragStart, onDragEnd, onEdit, onDelete, onStatusChange, onViewDetail, canEdit }: TaskCardProps) {
    const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== TaskStatus.DONE;

    return (
        <div
            className={`task-card ${isDragging ? 'dragging' : ''}`}
            draggable={canEdit}
            onDragStart={onDragStart}
            onDragEnd={onDragEnd}
            onClick={onViewDetail}
        >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <span className="task-card-key">{task.taskKey}</span>
                {canEdit && (
                    <div style={{ display: 'flex', gap: 2 }}>
                        <button
                            className="btn btn-ghost btn-icon btn-sm"
                            onClick={(e) => {
                                e.stopPropagation();
                                onEdit();
                            }}
                            title="Edit"
                        >
                            <Pencil size={12} />
                        </button>
                        <button
                            className="btn btn-ghost btn-icon btn-sm"
                            onClick={(e) => {
                                e.stopPropagation();
                                onDelete();
                            }}
                            title="Delete"
                        >
                            <Trash2 size={12} />
                        </button>
                    </div>
                )}
            </div>
            <div className="task-card-title">{task.title}</div>
            <div className="task-card-footer">
                <span className={`badge badge-priority-${task.priority}`}>
                    {PRIORITY_LABELS[task.priority]}
                </span>
                <div className="task-card-meta">
                    {task.dueDate && (
                        <span className={`task-card-due ${isOverdue ? 'overdue' : ''}`}>
                            <Calendar size={11} />
                            {task.dueDate}
                        </span>
                    )}
                    {task.assigneeUsername && (
                        <span className="task-card-assignee" title={task.assigneeUsername}>
                            {task.assigneeUsername.charAt(0).toUpperCase()}
                        </span>
                    )}
                </div>
            </div>

            {/* Quick status buttons */}
            {canEdit && (
                <div
                    style={{
                        display: 'flex',
                        gap: 4,
                        marginTop: 8,
                        paddingTop: 8,
                        borderTop: '1px solid var(--border-subtle)',
                    }}
                >
                    {Object.values(TaskStatus)
                        .filter((s) => s !== task.status)
                        .map((s) => {
                            const cfg = STATUS_CONFIG[s];
                            return (
                                <button
                                    key={s}
                                    className="btn btn-ghost btn-sm"
                                    style={{ fontSize: 'var(--font-size-xs)', flex: 1 }}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onStatusChange(s);
                                    }}
                                    title={`Move to ${cfg.label}`}
                                >
                                    {cfg.label}
                                </button>
                            );
                        })}
                </div>
            )}
        </div>
    );
}
