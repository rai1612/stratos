import { useState } from 'react';
import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
    LayoutDashboard,
    FolderKanban,
    LogOut,
    ChevronRight,
    PanelLeftClose,
    PanelLeftOpen,
    Inbox
} from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

export default function AppLayout() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const { workspaceId, projectKey } = useParams();
    const [sidebarOpen, setSidebarOpen] = useState(true);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    // Build breadcrumbs from URL
    const breadcrumbs = buildBreadcrumbs(location.pathname, workspaceId, projectKey);

    return (
        <div className="app-layout">
            {/* Sidebar */}
            <aside className={`sidebar ${sidebarOpen ? '' : 'collapsed'}`}>
                <div className="sidebar-brand">
                    <h2>⚡ Stratos</h2>
                </div>

                <nav className="sidebar-nav">
                    <div className="sidebar-section-label">Navigation</div>
                    <NavLink
                        to="/workspaces"
                        className={({ isActive }) =>
                            `sidebar-nav-item ${isActive && location.pathname === '/workspaces' ? 'active' : ''}`
                        }
                    >
                        <LayoutDashboard />
                        Workspaces
                    </NavLink>

                    <NavLink
                        to="/inbox"
                        className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
                    >
                        <Inbox />
                        Inbox
                    </NavLink>

                    {workspaceId && (
                        <NavLink
                            to={`/workspaces/${workspaceId}`}
                            end
                            className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}
                        >
                            <FolderKanban />
                            Projects
                        </NavLink>
                    )}
                </nav>

                <div className="sidebar-footer">
                    <div className="sidebar-user">
                        <div className="sidebar-user-avatar">
                            {user?.username.charAt(0).toUpperCase()}
                        </div>
                        <div className="sidebar-user-info">
                            <div className="sidebar-user-name">{user?.username}</div>
                            <div className="sidebar-user-email">{user?.email}</div>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="btn btn-ghost btn-icon"
                            title="Sign out"
                        >
                            <LogOut size={16} />
                        </button>
                    </div>
                </div>
            </aside>

            {/* Main Content */}
            <main className={`main-content ${sidebarOpen ? '' : 'sidebar-collapsed'}`}>
                <header className="header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <button
                            className="btn btn-ghost btn-icon btn-sm"
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            title={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
                        >
                            {sidebarOpen ? <PanelLeftClose size={18} /> : <PanelLeftOpen size={18} />}
                        </button>
                        <div className="header-breadcrumbs">
                            {breadcrumbs.map((crumb, i) => (
                                <span key={crumb.path} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    {i > 0 && <ChevronRight size={14} />}
                                    {i < breadcrumbs.length - 1 ? (
                                        <Link to={crumb.path}>{crumb.label}</Link>
                                    ) : (
                                        <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>
                                            {crumb.label}
                                        </span>
                                    )}
                                </span>
                            ))}
                        </div>
                    </div>
                </header>

                <div className="page-content">
                    <Outlet />
                </div>
            </main>
        </div>
    );
}

interface Crumb {
    label: string;
    path: string;
}

function buildBreadcrumbs(
    pathname: string,
    workspaceId?: string,
    projectKey?: string
): Crumb[] {
    const crumbs: Crumb[] = [{ label: 'Workspaces', path: '/workspaces' }];

    if (workspaceId) {
        crumbs.push({
            label: 'Projects',
            path: `/workspaces/${workspaceId}`,
        });
    }

    if (projectKey && workspaceId) {
        crumbs.push({
            label: projectKey,
            path: `/workspaces/${workspaceId}/projects/${projectKey}`,
        });
    }

    return crumbs;
}
