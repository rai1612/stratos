import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { workspaceApi } from '../api/workspaceApi';
import type { WorkspaceRequest, WorkspaceResponse } from '../types';
import { Plus, FolderOpen, Trash2, Pencil, X } from 'lucide-react';
import { useToast } from '../context/ToastContext';

export default function WorkspacesPage() {
    const navigate = useNavigate();
    const { addToast } = useToast();
    const [workspaces, setWorkspaces] = useState<WorkspaceResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [showModal, setShowModal] = useState(false);
    const [editingWorkspace, setEditingWorkspace] = useState<WorkspaceResponse | null>(null);
    const [formData, setFormData] = useState<WorkspaceRequest>({ name: '', description: '' });
    const [submitting, setSubmitting] = useState(false);

    const fetchWorkspaces = async () => {
        try {
            const data = await workspaceApi.getAll();
            setWorkspaces(data);
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to load workspaces');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchWorkspaces();
    }, []);

    const openCreate = () => {
        setEditingWorkspace(null);
        setFormData({ name: '', description: '' });
        setShowModal(true);
    };

    const openEdit = (ws: WorkspaceResponse, e: React.MouseEvent) => {
        e.stopPropagation();
        setEditingWorkspace(ws);
        setFormData({ name: ws.name, description: ws.description || '' });
        setShowModal(true);
    };

    const handleSubmit = async () => {
        if (!formData.name.trim()) return;
        setSubmitting(true);
        try {
            if (editingWorkspace) {
                await workspaceApi.update(editingWorkspace.id, formData);
            } else {
                await workspaceApi.create(formData);
            }
            setShowModal(false);
            fetchWorkspaces();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to save workspace');
        } finally {
            setSubmitting(false);
        }
    };

    const handleDelete = async (workspaceId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!confirm('Delete this workspace? This will also delete all its projects and tasks.')) return;
        try {
            await workspaceApi.delete(workspaceId);
            fetchWorkspaces();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to delete workspace');
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
                    <h1 className="page-title">Workspaces</h1>
                    <p className="page-subtitle">Manage your collaborative workspaces</p>
                </div>
                <button className="btn btn-primary" onClick={openCreate}>
                    <Plus size={16} /> New Workspace
                </button>
            </div>

            {workspaces.length === 0 ? (
                <div className="empty-state">
                    <FolderOpen />
                    <h3>No workspaces yet</h3>
                    <p>Create your first workspace to start organizing projects and tasks.</p>
                    <button className="btn btn-primary" onClick={openCreate}>
                        <Plus size={16} /> Create Workspace
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-3">
                    {workspaces.map((ws, i) => (
                        <div
                            key={ws.id}
                            className="card card-clickable workspace-card"
                            onClick={() => navigate(`/workspaces/${ws.id}`)}
                            style={{ animationDelay: `${i * 50}ms` }}
                        >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                                <div className="workspace-card-name">{ws.name}</div>
                                <div style={{ display: 'flex', gap: 4 }}>
                                    <button className="btn btn-ghost btn-icon btn-sm" onClick={(e) => openEdit(ws, e)} title="Edit">
                                        <Pencil size={14} />
                                    </button>
                                    <button className="btn btn-ghost btn-icon btn-sm" onClick={(e) => handleDelete(ws.id, e)} title="Delete">
                                        <Trash2 size={14} />
                                    </button>
                                </div>
                            </div>
                            {ws.description && <p className="workspace-card-desc">{ws.description}</p>}
                            <div className="workspace-card-meta">
                                <FolderOpen size={12} /> {ws.name}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Modal */}
            {showModal && (
                <div className="modal-overlay" onClick={() => setShowModal(false)}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>{editingWorkspace ? 'Edit Workspace' : 'New Workspace'}</h2>
                            <button className="btn btn-ghost btn-icon" onClick={() => setShowModal(false)}>
                                <X size={18} />
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label htmlFor="ws-name">Name</label>
                                <input
                                    id="ws-name"
                                    className="form-input"
                                    placeholder="e.g. Engineering Team"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="ws-desc">Description</label>
                                <textarea
                                    id="ws-desc"
                                    className="form-textarea"
                                    placeholder="Optional description"
                                    value={formData.description || ''}
                                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                                />
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
                                Cancel
                            </button>
                            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting || !formData.name.trim()}>
                                {submitting ? 'Saving…' : editingWorkspace ? 'Update' : 'Create'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
