import { useEffect, useState } from 'react';
import { workspaceApi } from '../api/workspaceApi';
import type { WorkspaceMemberResponse, WorkspaceInvitationResponse } from '../types';
import { WorkspaceRole, InvitationStatus } from '../types';
import { Trash2, UserPlus, Shield, X, Crown } from 'lucide-react';
import { useToast } from '../context/ToastContext';

interface WorkspaceMembersProps {
    workspaceId: string;
    currentUserRole: WorkspaceRole;
}

export function WorkspaceMembers({ workspaceId, currentUserRole }: WorkspaceMembersProps) {
    const [members, setMembers] = useState<WorkspaceMemberResponse[]>([]);
    const [invitations, setInvitations] = useState<WorkspaceInvitationResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const { addToast } = useToast();

    const [showInviteModal, setShowInviteModal] = useState(false);
    const [inviteEmail, setInviteEmail] = useState('');
    const [inviteRole, setInviteRole] = useState<WorkspaceRole>(WorkspaceRole.MEMBER);
    const [inviting, setInviting] = useState(false);
    const [inviteError, setInviteError] = useState('');

    const canManage = currentUserRole === WorkspaceRole.OWNER || currentUserRole === WorkspaceRole.ADMIN;

    const fetchData = async () => {
        try {
            setLoading(true);
            const m = await workspaceApi.getMembers(workspaceId);
            setMembers(m);
            if (canManage) {
                const i = await workspaceApi.getInvitations(workspaceId);
                setInvitations(i);
            }
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to load members');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workspaceId, canManage]);

    const handleInvite = async () => {
        if (!inviteEmail.trim()) return;
        try {
            setInviting(true);
            await workspaceApi.inviteMember(workspaceId, { email: inviteEmail, role: inviteRole });
            setShowInviteModal(false);
            setInviteEmail('');
            setInviteRole(WorkspaceRole.MEMBER);
            setInviteError('');
            fetchData();
        } catch (err: any) {
            setInviteError(err.response?.data?.message || 'Failed to invite user');
        } finally {
            setInviting(false);
        }
    };

    const handleRemoveMember = async (userId: string) => {
        if (!confirm('Are you sure you want to remove this member?')) return;
        try {
            await workspaceApi.removeMember(workspaceId, userId);
            fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to remove member');
        }
    };

    const handleRoleChange = async (userId: string, newRole: WorkspaceRole) => {
        try {
            await workspaceApi.updateMemberRole(workspaceId, userId, { role: newRole });
            fetchData();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to update role');
        }
    };

    const handleTransferOwnership = async (newOwnerId: string) => {
        if (!confirm('Are you sure you want to transfer ownership? You will be demoted to Admin.')) return;
        try {
            await workspaceApi.transferOwnership(workspaceId, newOwnerId);
            fetchData();
            window.location.reload();
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to transfer ownership');
        }
    };

    if (loading) return <div className="spinner" />;

    return (
        <div>
            <div className="page-header" style={{ marginBottom: 24 }}>
                <h2 className="section-title">Members</h2>
                {canManage && (
                    <button className="btn btn-primary" onClick={() => { setInviteError(''); setShowInviteModal(true); }}>
                        <UserPlus size={16} /> Invite Member
                    </button>
                )}
            </div>

            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                        <tr style={{ borderBottom: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)' }}>
                            <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>User</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Email</th>
                            <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Role</th>
                            {canManage && <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)', width: 80 }}>Actions</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {members.map(member => {
                            // Enforce strict hierarchy
                            const outranksMe = currentUserRole === WorkspaceRole.ADMIN && (member.role === WorkspaceRole.OWNER || member.role === WorkspaceRole.ADMIN);
                            const canEditRole = canManage && member.role !== WorkspaceRole.OWNER && !outranksMe;
                            const canRemove = canManage && member.role !== WorkspaceRole.OWNER && !outranksMe;
                            const canTransferOwnership = currentUserRole === WorkspaceRole.OWNER && member.role !== WorkspaceRole.OWNER;

                            return (
                                <tr key={member.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                                    <td style={{ padding: '12px 16px' }}>
                                        {member.username}
                                        {member.role === WorkspaceRole.OWNER && (
                                            <span style={{ marginLeft: 8, color: '#f59e0b', verticalAlign: 'middle' }} title="Workspace Owner">
                                                <Crown size={14} />
                                            </span>
                                        )}
                                    </td>
                                    <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{member.email}</td>
                                    <td style={{ padding: '12px 16px' }}>
                                        {canEditRole ? (
                                            <select
                                                className="form-input"
                                                style={{ padding: '4px 8px', width: 'auto' }}
                                                value={member.role}
                                                onChange={(e) => handleRoleChange(member.userId, e.target.value as WorkspaceRole)}
                                            >
                                                {currentUserRole === WorkspaceRole.OWNER && <option value={WorkspaceRole.ADMIN}>Admin</option>}
                                                <option value={WorkspaceRole.MEMBER}>Member</option>
                                                <option value={WorkspaceRole.VIEWER}>Viewer</option>
                                            </select>
                                        ) : (
                                            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 'var(--font-size-sm)', backgroundColor: 'var(--bg-tertiary)', padding: '4px 8px', borderRadius: 'var(--radius-sm)' }}>
                                                <Shield size={12} /> {member.role}
                                            </span>
                                        )}
                                    </td>
                                    {canManage && (
                                        <td style={{ padding: '12px 16px' }}>
                                            <div style={{ display: 'flex', gap: '8px' }}>
                                                {canTransferOwnership && (
                                                    <button
                                                        className="btn btn-ghost btn-icon btn-sm text-primary"
                                                        onClick={() => handleTransferOwnership(member.userId)}
                                                        title="Transfer Ownership"
                                                    >
                                                        <Crown size={16} />
                                                    </button>
                                                )}
                                                {canRemove && (
                                                    <button className="btn btn-ghost btn-icon btn-sm text-danger" onClick={() => handleRemoveMember(member.userId)} title="Remove Member">
                                                        <Trash2 size={16} />
                                                    </button>
                                                )}
                                            </div>
                                        </td>
                                    )}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>

            {canManage && invitations.length > 0 && (
                <div style={{ marginTop: 32 }}>
                    <h3 className="section-title">Pending Invitations</h3>
                    <div className="card" style={{ padding: 0, overflow: 'hidden', marginTop: 12 }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                            <thead>
                                <tr style={{ borderBottom: '1px solid var(--border-color)', backgroundColor: 'var(--bg-secondary)' }}>
                                    <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Email</th>
                                    <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Role</th>
                                    <th style={{ padding: '12px 16px', fontWeight: 600, color: 'var(--text-secondary)' }}>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {invitations.map(inv => (
                                    <tr key={inv.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                                        <td style={{ padding: '12px 16px' }}>{inv.email}</td>
                                        <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{inv.role}</td>
                                        <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{inv.status}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* Invite Modal */}
            {showInviteModal && (
                <div className="modal-overlay" onClick={() => { setShowInviteModal(false); setInviteError(''); }}>
                    <div className="modal" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Invite Member</h2>
                            <button className="btn btn-ghost btn-icon" onClick={() => { setShowInviteModal(false); setInviteError(''); }}>
                                <X size={18} />
                            </button>
                        </div>
                        <div className="modal-body">
                            {inviteError && <div className="auth-error" style={{ marginBottom: 16 }}>{inviteError}</div>}
                            <div className="form-group">
                                <label>Email Address</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    placeholder="colleague@example.com"
                                    value={inviteEmail}
                                    onChange={(e) => setInviteEmail(e.target.value)}
                                    autoFocus
                                />
                            </div>
                            <div className="form-group">
                                <label>Role</label>
                                <select
                                    className="form-input"
                                    value={inviteRole}
                                    onChange={(e) => setInviteRole(e.target.value as WorkspaceRole)}
                                >
                                    <option value={WorkspaceRole.ADMIN}>Admin - Can manage members and projects</option>
                                    <option value={WorkspaceRole.MEMBER}>Member - Can manage tasks</option>
                                    <option value={WorkspaceRole.VIEWER}>Viewer - Read-only access</option>
                                </select>
                            </div>
                        </div>
                        <div className="modal-footer">
                            <button className="btn btn-secondary" onClick={() => { setShowInviteModal(false); setInviteError(''); }}>Cancel</button>
                            <button
                                className="btn btn-primary"
                                onClick={handleInvite}
                                disabled={inviting || !inviteEmail.trim()}
                            >
                                {inviting ? 'Inviting...' : 'Send Invitation'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
