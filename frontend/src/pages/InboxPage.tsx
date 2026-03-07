import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api/userApi';
import { workspaceApi } from '../api/workspaceApi';
import { WorkspaceInvitationResponse } from '../types';
import { Inbox, CheckCircle2, XCircle } from 'lucide-react';
import { useToast } from '../context/ToastContext';

export default function InboxPage() {
    const [invitations, setInvitations] = useState<WorkspaceInvitationResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const { addToast } = useToast();

    useEffect(() => {
        fetchInvitations();
    }, []);

    const fetchInvitations = async () => {
        try {
            setLoading(true);
            const data = await userApi.getMyInvitations();
            setInvitations(data);
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to fetch invitations');
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = async (token: string, workspaceId: string) => {
        try {
            await workspaceApi.acceptInvitation(token);
            navigate(`/workspaces/${workspaceId}`);
        } catch (err: any) {
            addToast(err.response?.data?.message || 'Failed to accept invitation');
        }
    };

    if (loading) return <div className="loading">Loading invitations...</div>;

    return (
        <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
                <Inbox size={28} />
                <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 600 }}>Inbox</h1>
            </div>

            {invitations.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '48px', backgroundColor: 'var(--bg-secondary)', borderRadius: '12px', color: 'var(--text-secondary)' }}>
                    <Inbox style={{ margin: '0 auto 16px', display: 'block', opacity: 0.5 }} size={48} />
                    <p style={{ margin: 0, fontSize: '1.1rem' }}>You're all caught up!</p>
                    <p style={{ margin: '8px 0 0', fontSize: '0.9rem' }}>No pending workspace invitations.</p>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    {invitations.map(invitation => (
                        <div key={invitation.id} style={{
                            backgroundColor: 'var(--bg-secondary)',
                            border: '1px solid var(--border-color)',
                            borderRadius: '12px',
                            padding: '20px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between'
                        }}>
                            <div>
                                <h3 style={{ margin: '0 0 8px', fontSize: '1.2rem' }}>
                                    Join <strong style={{ color: 'var(--primary-color)' }}>{invitation.workspaceName}</strong>
                                </h3>
                                <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                                    You've been invited to join this workspace as a <strong>{invitation.role}</strong>.
                                </p>
                            </div>
                            <div style={{ display: 'flex', gap: '12px' }}>
                                <button
                                    className="btn btn-primary"
                                    onClick={() => handleAccept(invitation.token, invitation.workspaceId)}
                                    style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
                                >
                                    <CheckCircle2 size={18} />
                                    Accept
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
