import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';
import {
    Zap,
    LayoutDashboard,
    FolderKanban,
    CheckCircle2,
    Users,
    Shield,
    ArrowRight,
} from 'lucide-react';

const FEATURES = [
    {
        icon: LayoutDashboard,
        title: 'Workspaces',
        desc: 'Organize teams and projects under dedicated workspaces with full isolation.',
    },
    {
        icon: FolderKanban,
        title: 'Kanban Boards',
        desc: 'Visualize progress with drag-friendly task boards — To Do, In Progress, Done.',
    },
    {
        icon: CheckCircle2,
        title: 'Smart Tasks',
        desc: 'Priorities, due dates, assignees, and human-readable keys like STRAT-3.',
    },
    {
        icon: Users,
        title: 'Collaboration',
        desc: 'Assign tasks to team members and track who is working on what.',
    },
    {
        icon: Shield,
        title: 'Role-Based Access',
        desc: 'Fine-grained permissions with workspace-specific Owner, Admin, Member, and Viewer roles.',
    },
    {
        icon: Zap,
        title: 'Blazing Fast',
        desc: 'Built with React and Spring Boot for a snappy, real-time experience.',
    },
];

export default function LandingPage() {
    const { isAuthenticated } = useAuth();

    if (isAuthenticated) {
        return <Navigate to="/workspaces" replace />;
    }

    return (
        <div className="landing-page">
            {/* Nav */}
            <nav className="landing-nav">
                <div className="landing-nav-inner">
                    <div className="landing-brand">
                        <Zap size={22} style={{ color: 'var(--accent)' }} />
                        <span>Stratos</span>
                    </div>
                    <div className="landing-nav-links">
                        <Link to="/login" className="btn btn-ghost">
                            Sign in
                        </Link>
                        <Link to="/register" className="btn btn-primary">
                            Get Started <ArrowRight size={14} />
                        </Link>
                    </div>
                </div>
            </nav>

            {/* Hero */}
            <section className="landing-hero">
                <div className="landing-hero-glow" />
                <div className="landing-hero-content">
                    <div className="landing-badge">✨ Collaborative Task Management</div>
                    <h1 className="landing-heading">
                        Manage projects
                        <br />
                        <span className="landing-heading-accent">at the speed of thought</span>
                    </h1>
                    <p className="landing-subheading">
                        Stratos brings your team's work together — workspaces, projects, and tasks in one
                        beautiful, organized system. Track progress with Kanban boards, set priorities, and
                        ship faster.
                    </p>
                    <div className="landing-cta">
                        <Link to="/register" className="btn btn-primary btn-lg">
                            Start for free <ArrowRight size={16} />
                        </Link>
                        <Link to="/login" className="btn btn-secondary btn-lg">
                            Sign in
                        </Link>
                    </div>
                </div>
            </section>

            {/* Features */}
            <section className="landing-features">
                <h2 className="landing-section-title">Everything you need</h2>
                <p className="landing-section-subtitle">
                    Built for teams that move fast and need clarity.
                </p>
                <div className="landing-features-grid">
                    {FEATURES.map((feat, i) => {
                        const Icon = feat.icon;
                        return (
                            <div key={feat.title} className="landing-feature-card" style={{ animationDelay: `${i * 80}ms` }}>
                                <div className="landing-feature-icon">
                                    <Icon size={22} />
                                </div>
                                <h3>{feat.title}</h3>
                                <p>{feat.desc}</p>
                            </div>
                        );
                    })}
                </div>
            </section>

            {/* Bottom CTA */}
            <section className="landing-bottom-cta">
                <h2>Ready to get started?</h2>
                <p>Create your free account and start managing projects in minutes.</p>
                <Link to="/register" className="btn btn-primary btn-lg">
                    Create your account <ArrowRight size={16} />
                </Link>
            </section>

            {/* Footer */}
            <footer className="landing-footer">
                <p>
                    Built with <span style={{ color: 'var(--danger)' }}>♥</span> using React &amp; Spring Boot
                </p>
            </footer>
        </div>
    );
}
