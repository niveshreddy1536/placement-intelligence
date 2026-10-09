import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'

function Layout() {
    return (
        <div className="flex min-h-screen bg-slate-900 text-white">

            {/* Sidebar */}
            <Sidebar />

            {/* Main Content */}
            <main className="flex-1 overflow-y-auto">

                <div className="min-h-screen p-6 lg:p-8">
                    <Outlet />
                </div>

            </main>

        </div>
    )
}

export default Layout