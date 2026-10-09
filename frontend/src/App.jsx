import {
    BrowserRouter,
    Navigate,
    Route,
    Routes,
} from "react-router-dom";

import { AuthProvider } from "./auth/AuthContext";
import ProtectedRoute from "./auth/ProtectedRoute";

import Login from "./Login";
import Register from "./Register";

import Layout from "./components/Layout";

import Dashboard from "./components/Dashboard";
import Jobs from "./components/Jobs";
import Match from "./components/Match";
import SkillGaps from "./components/SkillGaps";
import Roadmap from "./components/Roadmap";
import Applications from "./components/Applications";

import Resume from "./Resume";
import Projects from "./Projects";
import Recommendations from "./Recommendations";
import JobDescriptions from "./JobDescriptions";

import Profile from "./Profile";


function App() {

    return (
        <AuthProvider>

            <BrowserRouter>

                <Routes>

                    {/* ================================
                        PUBLIC ROUTES
                    ================================= */}

                    <Route
                        path="/login"
                        element={<Login />}
                    />

                    <Route
                        path="/register"
                        element={<Register />}
                    />


                    {/* ================================
                        PROTECTED ROUTES
                    ================================= */}

                    <Route
                        element={<ProtectedRoute />}
                    >

                        <Route
                            element={<Layout />}
                        >

                            <Route
                                path="/"
                                element={<Dashboard />}
                            />

                            <Route
                                path="/jobs"
                                element={<Jobs />}
                            />

                            <Route
                                path="/resume"
                                element={<Resume />}
                            />

                            <Route
                                path="/match"
                                element={<Match />}
                            />

                            <Route
                                path="/skill-gaps"
                                element={<SkillGaps />}
                            />

                            <Route
                                path="/roadmap"
                                element={<Roadmap />}
                            />

                            <Route
                                path="/projects"
                                element={<Projects />}
                            />

                            <Route
                                path="/recommendations"
                                element={<Recommendations />}
                            />

                            <Route
                                path="/job-descriptions"
                                element={<JobDescriptions />}
                            />

                            <Route
                                path="/applications"
                                element={<Applications />}
                            />

                            <Route
                                path="/profile"
                                element={<Profile />}
                            />

                        </Route>

                    </Route>


                    {/* ================================
                        UNKNOWN ROUTE
                    ================================= */}

                    <Route
                        path="*"
                        element={
                            <Navigate
                                to="/"
                                replace
                            />
                        }
                    />

                </Routes>

            </BrowserRouter>

        </AuthProvider>
    );
}

export default App;