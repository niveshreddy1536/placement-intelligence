import {
    useEffect,
    useState,
} from "react";

import {
    Link,
    useNavigate,
} from "react-router-dom";

import { useAuth } from "./auth/AuthContext";


function Login() {

    const navigate = useNavigate();

    const {
        login,
        isAuthenticated,
    } = useAuth();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);


    useEffect(() => {

        if (isAuthenticated) {
            navigate("/", {
                replace: true,
            });
        }

    }, [
        isAuthenticated,
        navigate,
    ]);


    const handleSubmit = async (event) => {

        event.preventDefault();

        setError("");
        setLoading(true);

        try {

            await login(
                email,
                password
            );

            navigate("/", {
                replace: true,
            });

        } catch (error) {

            setError(
                error.message ||
                "Unable to login."
            );

        } finally {

            setLoading(false);
        }
    };


    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4">

            <div className="w-full max-w-md">

                <div className="mb-8 text-center">

                    <h1 className="text-3xl font-bold text-white">
                        Placement Intelligence
                    </h1>

                    <p className="mt-2 text-slate-400">
                        Sign in to your account
                    </p>

                </div>


                <div className="rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-2xl">

                    <form
                        onSubmit={handleSubmit}
                        className="space-y-5"
                    >

                        <div>

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Email
                            </label>

                            <input
                                type="email"
                                autoComplete="email"
                                value={email}
                                onChange={(event) =>
                                    setEmail(
                                        event.target.value
                                    )
                                }
                                placeholder="you@example.com"
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-500"
                            />

                        </div>


                        <div>

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Password
                            </label>

                            <input
                                type="password"
                                autoComplete="current-password"
                                value={password}
                                onChange={(event) =>
                                    setPassword(
                                        event.target.value
                                    )
                                }
                                placeholder="Enter your password"
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-500"
                            />

                        </div>


                        {error && (

                            <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">
                                {error}
                            </div>

                        )}


                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full rounded-xl bg-blue-600 px-4 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >

                            {loading
                                ? "Signing in..."
                                : "Sign In"}

                        </button>

                    </form>


                    <div className="mt-6 text-center text-sm text-slate-400">

                        Don't have an account?{" "}

                        <Link
                            to="/register"
                            className="font-medium text-blue-400 hover:text-blue-300"
                        >
                            Create one
                        </Link>

                    </div>

                </div>

            </div>

        </div>
    );
}


export default Login;