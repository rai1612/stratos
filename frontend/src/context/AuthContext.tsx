import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { AuthUser, JwtResponse } from '../types';

interface AuthContextType {
    user: AuthUser | null;
    isAuthenticated: boolean;
    login: (jwtResponse: JwtResponse) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STORAGE_KEY = 'stratos_user';

function loadUser(): AuthUser | null {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            return JSON.parse(stored) as AuthUser;
        }
    } catch {
        localStorage.removeItem(STORAGE_KEY);
    }
    return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<AuthUser | null>(loadUser);

    const login = useCallback((jwtResponse: JwtResponse) => {
        const authUser: AuthUser = {
            id: jwtResponse.id,
            username: jwtResponse.username,
            email: jwtResponse.email,
            token: jwtResponse.token,
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(authUser));
        setUser(authUser);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem(STORAGE_KEY);
        setUser(null);
    }, []);

    return (
        <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
