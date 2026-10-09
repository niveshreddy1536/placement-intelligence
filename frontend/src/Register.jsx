import {
    useState,
} from 'react'

import {
    Link,
    useNavigate,
} from 'react-router-dom'

import { useAuth } from './auth/AuthContext'

function Register() {

    const navigate = useNavigate()

    const {
        login,
    } = useAuth()

    const [name, setName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')

    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSubmit = async event => {

        event.preventDefault()

        setError('')

        if (password !== confirmPassword) {

            setError(
                'Passwords do not match.'
            )

            return
        }

        if (password.length < 6) {

            setError(
                'Password must contain at least 6 characters.'
            )

            return
        }

        setLoading(true)

        try {

            const response =
                await fetch(
                    'http://localhost:8080/api/auth/register',
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type':
                                'application/json',
                        },
                        body: JSON.stringify({
                            name,
                            email,
                            password,
                        }),
                    }
                )

            const data =
                await response.json()

            if (!response.ok) {

                throw new Error(
                    data.message ||
                    'Registration failed.'
                )
            }

            /*
             * Registration automatically logs
             * the student into the application.
             */
            login(data)

            navigate('/', {
                replace: true,
            })

        } catch (error) {

            setError(
                error.message ||
                'Unable to create account.'
            )

        } finally {

            setLoading(false)
        }
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-slate-950 px-4">

            <div className="w-full max-w-md">

                <div className="mb-8 text-center">

                    <h1 className="text-3xl font-bold text-white">
                        Create Account
                    </h1>

                    <p className="mt-2 text-slate-400">
                        Start your Placement Intelligence journey
                    </p>

                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-900 p-8 shadow-2xl">

                    <form
                        onSubmit={handleSubmit}
                        className="space-y-5"
                    >

                        <div>

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Full Name
                            </label>

                            <input
                                type="text"
                                value={name}
                                onChange={event =>
                                    setName(
                                        event.target.value
                                    )
                                }
                                placeholder="Your full name"
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-500"
                            />

                        </div>

                        <div>

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Email
                            </label>

                            <input
                                type="email"
                                value={email}
                                onChange={event =>
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
                                value={password}
                                onChange={event =>
                                    setPassword(
                                        event.target.value
                                    )
                                }
                                placeholder="Minimum 6 characters"
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition focus:border-blue-500"
                            />

                        </div>

                        <div>

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Confirm Password
                            </label>

                            <input
                                type="password"
                                value={confirmPassword}
                                onChange={event =>
                                    setConfirmPassword(
                                        event.target.value
                                    )
                                }
                                placeholder="Enter password again"
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
                                ? 'Creating account...'
                                : 'Create Account'}
                        </button>

                    </form>

                    <div className="mt-6 text-center text-sm text-slate-400">

                        Already have an account?{' '}

                        <Link
                            to="/login"
                            className="font-medium text-blue-400 hover:text-blue-300"
                        >
                            Sign in
                        </Link>

                    </div>

                </div>

            </div>

        </div>
    )
}

export default Register