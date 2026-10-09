import {
    createContext,
    useContext,
    useEffect,
    useState,
} from "react";

const API_BASE_URL = "http://localhost:8080";
const STORAGE_KEY = "placement_auth";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);
    const [studentId, setStudentId] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        initializeAuth();
    }, []);

    async function initializeAuth() {
        try {
            const savedAuth =
                localStorage.getItem(STORAGE_KEY);

            if (!savedAuth) {
                setLoading(false);
                return;
            }

            let authData;

            try {
                authData = JSON.parse(savedAuth);
            } catch {
                localStorage.removeItem(STORAGE_KEY);
                setLoading(false);
                return;
            }

            if (!authData?.token) {
                localStorage.removeItem(STORAGE_KEY);
                setLoading(false);
                return;
            }

            /*
             * Verify the stored token with the backend.
             */
            const response = await fetch(
                `${API_BASE_URL}/api/auth/me`,
                {
                    method: "GET",
                    headers: {
                        Authorization:
                            `Bearer ${authData.token}`,
                    },
                }
            );

            if (!response.ok) {
                localStorage.removeItem(STORAGE_KEY);

                setUser(null);
                setToken(null);
                setStudentId(null);

                setLoading(false);
                return;
            }

            const currentUser =
                await response.json();

            const normalizedUser = {
                studentId:
                currentUser.studentId,
                name:
                currentUser.name,
                email:
                currentUser.email,
                token:
                    currentUser.token ||
                    authData.token,
            };

            setUser(normalizedUser);
            setToken(normalizedUser.token);
            setStudentId(normalizedUser.studentId);

            /*
             * Keep the verified authentication
             * information in localStorage.
             */
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify(normalizedUser)
            );
        } catch (error) {
            console.error(
                "Authentication initialization failed:",
                error
            );

            localStorage.removeItem(STORAGE_KEY);

            setUser(null);
            setToken(null);
            setStudentId(null);
        } finally {
            setLoading(false);
        }
    }

    async function login(email, password) {
        const response = await fetch(
            `${API_BASE_URL}/api/auth/login`,
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json",
                },
                body: JSON.stringify({
                    email,
                    password,
                }),
            }
        );

        let data = null;

        try {
            data = await response.json();
        } catch {
            data = null;
        }

        if (!response.ok) {
            throw new Error(
                data?.message ||
                "Login failed."
            );
        }

        const authData = {
            studentId: data.studentId,
            name: data.name,
            email: data.email,
            token: data.token,
        };

        /*
         * Save authentication immediately.
         */
        localStorage.setItem(
            STORAGE_KEY,
            JSON.stringify(authData)
        );

        setUser(authData);
        setToken(authData.token);
        setStudentId(authData.studentId);

        return authData;
    }

    async function register(
        name,
        email,
        password
    ) {
        const response = await fetch(
            `${API_BASE_URL}/api/auth/register`,
            {
                method: "POST",
                headers: {
                    "Content-Type":
                        "application/json",
                },
                body: JSON.stringify({
                    name,
                    email,
                    password,
                }),
            }
        );

        let data = null;

        try {
            data = await response.json();
        } catch {
            data = null;
        }

        if (!response.ok) {
            throw new Error(
                data?.message ||
                "Registration failed."
            );
        }

        const authData = {
            studentId: data.studentId,
            name: data.name,
            email: data.email,
            token: data.token,
        };

        localStorage.setItem(
            STORAGE_KEY,
            JSON.stringify(authData)
        );

        setUser(authData);
        setToken(authData.token);
        setStudentId(authData.studentId);

        return authData;
    }

    async function logout() {
        try {
            if (token) {
                await fetch(
                    `${API_BASE_URL}/api/auth/logout`,
                    {
                        method: "POST",
                        headers: {
                            Authorization:
                                `Bearer ${token}`,
                        },
                    }
                );
            }
        } catch (error) {
            console.error(
                "Logout request failed:",
                error
            );
        } finally {
            localStorage.removeItem(
                STORAGE_KEY
            );

            setUser(null);
            setToken(null);
            setStudentId(null);
        }
    }

    const value = {
        user,
        token,
        studentId,
        loading,
        isAuthenticated:
            Boolean(user && token && studentId),
        login,
        register,
        logout,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context =
        useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth must be used inside AuthProvider"
        );
    }

    return context;
}

export default AuthContext;