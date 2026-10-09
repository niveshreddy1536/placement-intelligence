import {
    LayoutDashboard,
    BriefcaseBusiness,
    FileText,
    Target,
    AlertTriangle,
    BookOpen,
    Lightbulb,
    Star,
    ClipboardList,
    UserCircle,
    LogOut,
} from 'lucide-react'

import {
    NavLink,
    useNavigate,
} from 'react-router-dom'

import { useAuth } from '../auth/AuthContext'

const menuItems = [
    {
        label: 'Dashboard',
        path: '/',
        icon: LayoutDashboard,
    },
    {
        label: 'Jobs',
        path: '/jobs',
        icon: BriefcaseBusiness,
    },
    {
        label: 'Resume',
        path: '/resume',
        icon: FileText,
    },
    {
        label: 'Job Match',
        path: '/match',
        icon: Target,
    },
    {
        label: 'Skill Gaps',
        path: '/skill-gaps',
        icon: AlertTriangle,
    },
    {
        label: 'Learning Roadmap',
        path: '/roadmap',
        icon: BookOpen,
    },
    {
        label: 'Projects',
        path: '/projects',
        icon: Lightbulb,
    },
    {
        label: 'Recommendations',
        path: '/recommendations',
        icon: Star,
    },
    {
        label: 'Applications',
        path: '/applications',
        icon: ClipboardList,
    },
]

function Sidebar() {
    const navigate = useNavigate()

    const {
        user,
        logout,
    } = useAuth()

    function handleLogout() {
        logout()
        navigate('/login', {
            replace: true,
        })
    }

    return (
        <aside className="flex h-screen w-64 flex-col border-r border-slate-800 bg-slate-950 text-white">

            {/* Logo */}
            <div className="border-b border-slate-800 px-6 py-6">
                <div className="flex items-center gap-3">

                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600">
                        <Target size={22} />
                    </div>

                    <div>
                        <h1 className="text-lg font-bold">
                            Placement
                        </h1>

                        <p className="text-sm text-slate-400">
                            Intelligence
                        </p>
                    </div>

                </div>
            </div>

            {/* Navigation */}
            <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-5">

                {menuItems.map((item) => {
                    const Icon = item.icon

                    return (
                        <NavLink
                            key={item.label}
                            to={item.path}
                            className={({ isActive }) =>
                                `flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm font-medium transition ${
                                    isActive
                                        ? 'bg-blue-600/15 text-blue-400'
                                        : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                                }`
                            }
                        >
                            <Icon size={19} />

                            <span>
                                {item.label}
                            </span>
                        </NavLink>
                    )
                })}

            </nav>

            {/* Bottom Section */}
            <div className="border-t border-slate-800 p-3">

                {/* Profile */}
                <NavLink
                    to="/profile"
                    className={({ isActive }) =>
                        `flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-medium transition ${
                            isActive
                                ? 'bg-purple-600/15 text-purple-400'
                                : 'text-slate-300 hover:bg-slate-800 hover:text-white'
                        }`
                    }
                >
                    <UserCircle size={20} />

                    <div className="min-w-0">
                        <span className="block">
                            Profile
                        </span>

                        {user?.name && (
                            <span className="mt-0.5 block truncate text-xs text-slate-500">
                                {user.name}
                            </span>
                        )}
                    </div>
                </NavLink>

                {/* Logout */}
                <button
                    type="button"
                    onClick={handleLogout}
                    className="mt-1 flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-medium text-slate-300 transition hover:bg-red-500/10 hover:text-red-400"
                >
                    <LogOut size={20} />

                    <span>
                        Logout
                    </span>
                </button>

            </div>

        </aside>
    )
}

export default Sidebar