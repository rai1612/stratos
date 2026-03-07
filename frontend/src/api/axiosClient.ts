import axios from 'axios';

const API_BASE_URL = '/api';

const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor: attach JWT token
axiosClient.interceptors.request.use(
    (config) => {
        const stored = localStorage.getItem('stratos_user');
        if (stored) {
            try {
                const user = JSON.parse(stored);
                if (user.token) {
                    config.headers.Authorization = `Bearer ${user.token}`;
                }
            } catch {
                // Invalid stored data, ignore
            }
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor: handle 401 → redirect to login
axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('stratos_user');
            // Only redirect if not already on auth pages
            if (!window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export default axiosClient;
