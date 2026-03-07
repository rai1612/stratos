import { X, AlertCircle, CheckCircle2, Info, AlertTriangle } from 'lucide-react';
import { useToast, type Toast, type ToastType } from '../context/ToastContext';

const ICONS: Record<ToastType, React.ReactNode> = {
    error: <AlertCircle size={18} />,
    success: <CheckCircle2 size={18} />,
    info: <Info size={18} />,
    warning: <AlertTriangle size={18} />,
};

function ToastItem({ toast }: { toast: Toast }) {
    const { removeToast } = useToast();

    return (
        <div className={`toast toast-${toast.type}`} role="alert" aria-live="assertive">
            <span className="toast-icon">{ICONS[toast.type]}</span>
            <span className="toast-message">{toast.message}</span>
            <button
                className="toast-close"
                onClick={() => removeToast(toast.id)}
                aria-label="Dismiss notification"
            >
                <X size={15} />
            </button>
        </div>
    );
}

export default function ToastContainer() {
    const { toasts } = useToast();

    if (toasts.length === 0) return null;

    return (
        <div className="toast-container" aria-label="Notifications">
            {toasts.map((toast) => (
                <ToastItem key={toast.id} toast={toast} />
            ))}
        </div>
    );
}
