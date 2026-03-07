import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { projectApi } from '../api/projectApi';
import { workspaceApi } from '../api/workspaceApi';
import type { ProjectRequest, ProjectResponse, WorkspaceResponse } from '../types';
import { WorkspaceRole } from '../types';
import { Plus, FolderKanban, Trash2, Pencil, X, LogOut } from 'lucide-react';
import { WorkspaceMembers } from './WorkspaceMembers';
import { useToast } from '../context/ToastContext';

export default function WorkspaceDetailPage() {
    const { workspaceId } = useParams<{ workspaceId: string }>();
    const navigate = useNavigate();
    const wsId = workspaceId!;
    const { addToast } = useToast();

    const [workspace, setWorkspace] = useState<WorkspaceResponse | null>(null);
    const [projects, setProjects] = useState<ProjectResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingProject, setEditingProject] = useState<ProjectResponse | null>(null);
    const [formData, setFormData] = useState<ProjectRequest>({ name: '', projectKey: '' });
    const [submitting, setSubmitting] = useState(false);
    const [activeTab, setActiveTab] = useState<'projects' | 'members'>('projects');
    const [modalError, setModalError] = useState('');

    const fetchData = async () => {
        try {
            const [ws, prjs] = await Promise.all([
                workspaceApi.getById(wsId),
                projectApi.getAll(wsId),
            ]);
            setWorkspace(ws);
            setProjects(prjs);
        } catch (err: any) {
            if (err.response?.status === 403 || err.response?.status === 404) {
                addToast(err.response?.data?.message || 'Access denied', 'error');
                navigate('/workspaces', { replace: true });
                return;
            }
            addToast(err.response?.data?.message || 'Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        let ignore = false;

        const load = async () => {
            try {
                const [ws, prjs] = await Promise.all([
                    workspaceApi.getById(wsId),
                    projectApi.getAll(wsId),
                ]);
                if (ignore) return;
                setWorkspace(ws);
                setProjects(prjs);
            } catch (err: any) {
                if (ignore) return;
                if (err.response?.status === 403 || err.response?.status === 404) {
                    addToast(err.response?.data?.message || 'Access denied', 'error');
                    navigate('/workspaces', { replace: true });
                    return;
                }
                addToast(err.response?.data?.message || 'Failed to load data');
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        load();
        return () => { ignore = true; };
    }, [wsId]);

    const openCreate = () => {
        setEditingProject(null);
        setFormData({ name: '', projectKey: '' });
        setModalError('');
        setShowModal(true);
    };

    const openEdit = (proj: ProjectResponse, e: React.MouseEvent) => {
        e.stopPropagation();
        setEditingProject(proj);
        setFormData({ name: proj.name, projectKey: proj.projectKey });
        setModalError('');
        setShowModal(true);
    };

    const handleSubmit = async () => {
        if (!formData.name.trim() || !formData.projectKey.trim()) return;
        setSubmitting(true);
        try {
            if (editingProject) {
                await projectApi.update(wsId, editingProject.projectKey, formData);
            } else {
                await projectApi.create(wsId, formData);
            }
            setShowModal(false);
            fetchData();
        } catch (err: any) {
            setModalError(err.response?.data?.message || 'Failed to save project');
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (projectKey: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!confirm('Delete this project and all its tasks?')) return;
        try {
            await projectApi.delete(wsId, projectKey);
            fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to delete project');
        }
    };

    const handleLeaveWorkspace = async () => {
        if (!confirm('Are you sure you want to leave this workspace?')) return;
        try {
            await workspaceApi.leaveWorkspace(wsId);
            navigate('/workspaces');
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to leave workspace.');
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
            <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <h1 className="page-title">{workspace?.name || 'Workspace'}</h1>
                    <p className="page-subtitle">
                        {workspace?.description || 'Workspace overview'}
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                    <button
                        className="btn btn-outline"
                        style={{ color: '#ff4d4f', borderColor: '#ff4d4f', display: 'flex', gap: '8px', alignItems: 'center' }}
                        onClick={handleLeaveWorkspace}
                        title="Leave Workspace"
                    >
                        <LogOut size={16} /> Leave
                    </button>
                    {workspace?.role !== WorkspaceRole.VIEWER && activeTab === 'projects' && (
                        <button className="btn btn-primary" onClick={openCreate} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                            <Plus size={16} /> New Project
                        </button>
                    )}
                </div>
            </div>

            <div className="tabs" style={{ display: 'flex', gap: 16, marginBottom: 24, borderBottom: '1px solid var(--border-color)' }}>
                <button
                    className={`btn btn-ghost`}
                    style={{ borderBottom: activeTab === 'projects' ? '2px solid var(--primary-color)' : '2px solid transparent', borderRadius: 0, paddingBottom: 12 }}
                    onClick={() => setActiveTab('projects')}
                >
                    Projects
                </button>
                <button
                    className={`btn btn-ghost`}
                    style={{ borderBottom: activeTab === 'members' ? '2px solid var(--primary-color)' : '2px solid transparent', borderRadius: 0, paddingBottom: 12 }}
                    onClick={() => setActiveTab('members')}
                >
                    Members
                </button>
            </div>

            {activeTab === 'projects' ? (
                projects.length === 0 ? (
                    <div className="empty-state">
                        <FolderKanban />
                        <h3>No projects yet</h3>
                        <p>Create a project to start tracking tasks.</p>
                        {workspace?.role !== WorkspaceRole.VIEWER && (
                            <button className="btn btn-primary" onClick={openCreate}>
                                <Plus size={16} /> Create Project
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="grid grid-cols-3">
                        {projects.map((proj, i) => (
                            <div
                                key={proj.id}
                                className="card card-clickable"
                                onClick={() => navigate(`/workspaces/${wsId}/projects/${proj.projectKey}`)}
                                style={{ animationDelay: `${i * 50}ms` }}
                            >
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                    <div>
                                        <span className="project-card-key">{proj.projectKey}</span>
                                        <div className="project-card-name">{proj.name}</div>
                                    </div>
                                    {workspace?.role !== WorkspaceRole.VIEWER && (
                                        <div style={{ display: 'flex', gap: 4 }}>
                                            <button className="btn btn-ghost btn-icon btn-sm" onClick={(e) => openEdit(proj, e)} title="Edit">
                                                <Pencil size={14} />
                                            </button>
                                            <button className="btn btn-ghost btn-icon btn-sm" onClick={(e) => handleDelete(proj.projectKey, e)} title="Delete">
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                                <div className="workspace-card-meta" style={{ marginTop: 12 }}>
                                    <FolderKanban size={12} /> {proj.projectKey}
                                </div>
                            </div>
                        ))}
                    </div>
                )
            ) : (
                <WorkspaceMembers workspaceId={wsId} currentUserRole={workspace?.role || WorkspaceRole.VIEWER} />
            )}

            {/* Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{editingProject ? 'Edit Project' : 'New Project'}</h2>
                            <button className="btn btn-ghost btn-icon" onClick={() => setShowModal(false)}>
                                <X size={18} />
                            </button>
                        </div>
                        <div className="modal-body">
                            {modalError && <div className="auth-error" style={{ marginBottom: 16 }}>{modalError}</div>}
                            <div className="form-group">
                                <label htmlFor="proj-name">Name</label>
                                <input
                                    id="proj-name"
                                    className="form-input"
                                    placeholder="e.g. Stratos Backend"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="proj-key">Project Key</label>
                                <input
                                    id="proj-key"
                                    className="form-input"
                                    placeholder="e.g. STRAT (max 10 chars)"
                                    maxLength={10}
                                    value={formData.projectKey}
                                    onChange={(e) =>
                                        setFormData({ ...formData, projectKey: e.target.value.toUpperCase() })
                                    }
                                />
                                <small style={{ color: 'var(--text-tertiary)', fontSize: 'var(--font-size-xs)' }}>
                                    Used as a short identifier for the project
                                </small>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                Cancel
                            </button>
                            <button
                                className="btn btn-primary"
                                onClick={handleSubmit}
                                disabled={submitting || !formData.name.trim() || !formData.projectKey.trim()}
                            >
                                {submitting ? 'Saving…' : editingProject ? 'Update' : 'Create'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
