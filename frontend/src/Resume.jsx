import { useEffect, useState } from "react";
import { useAuth } from "./auth/AuthContext";

const API_BASE_URL = "http://localhost:8080";

function Resume() {
    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth();

    const [file, setFile] = useState(null);
    const [analysis, setAnalysis] = useState(null);
    const [student, setStudent] = useState(null);

    const [loading, setLoading] = useState(false);
    const [loadingAnalysis, setLoadingAnalysis] = useState(true);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    /*
     * =========================================================
     * LOAD USER DATA AFTER AUTHENTICATION
     * =========================================================
     */

    useEffect(() => {
        if (authLoading) {
            return;
        }

        if (!studentId || !token) {
            setLoadingAnalysis(false);
            setError("Please log in to access your resume.");
            return;
        }

        loadResumeAnalysis();
        loadStudent();
    }, [studentId, token, authLoading]);

    /*
     * =========================================================
     * AUTHORIZATION HEADERS
     * =========================================================
     */

    const getAuthHeaders = () => {
        return {
            Authorization: `Bearer ${token}`,
        };
    };

    /*
     * =========================================================
     * LOAD RESUME ANALYSIS
     * =========================================================
     */

    const loadResumeAnalysis = async () => {
        if (!studentId || !token) {
            return;
        }

        try {
            setLoadingAnalysis(true);
            setError("");

            const response = await fetch(
                `${API_BASE_URL}/api/resumes/analysis/${studentId}`,
                {
                    headers: getAuthHeaders(),
                }
            );

            if (!response.ok) {
                if (response.status === 404) {
                    setAnalysis(null);
                    return;
                }

                if (response.status === 401 || response.status === 403) {
                    throw new Error(
                        "Your session has expired. Please log in again."
                    );
                }

                throw new Error("Could not load resume analysis");
            }

            const data = await response.json();
            setAnalysis(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoadingAnalysis(false);
        }
    };

    /*
     * =========================================================
     * LOAD STUDENT PROFILE
     * =========================================================
     */

    const loadStudent = async () => {
        if (!studentId || !token) {
            return;
        }

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/students/${studentId}`,
                {
                    headers: getAuthHeaders(),
                }
            );

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    throw new Error(
                        "Your session has expired. Please log in again."
                    );
                }

                return;
            }

            const data = await response.json();
            setStudent(data);
        } catch (err) {
            if (
                err.message ===
                "Your session has expired. Please log in again."
            ) {
                setError(err.message);
            }

            // Student information is optional for this page.
        }
    };

    /*
     * =========================================================
     * FILE SELECTION
     * =========================================================
     */

    const handleFileChange = (event) => {
        const selectedFile = event.target.files?.[0];

        setMessage("");
        setError("");

        if (!selectedFile) {
            setFile(null);
            return;
        }

        if (selectedFile.type !== "application/pdf") {
            setFile(null);
            setError("Please select a PDF resume.");
            return;
        }

        setFile(selectedFile);
    };

    /*
     * =========================================================
     * UPLOAD RESUME
     * =========================================================
     */

    const handleUpload = async () => {
        if (!studentId || !token) {
            setError("Please log in before uploading your resume.");
            return;
        }

        if (!file) {
            setError("Please select a PDF resume first.");
            return;
        }

        try {
            setLoading(true);
            setMessage("");
            setError("");

            const formData = new FormData();
            formData.append("file", file);

            const response = await fetch(
                `${API_BASE_URL}/api/resumes/upload/${studentId}`,
                {
                    method: "POST",
                    headers: getAuthHeaders(),
                    body: formData,
                }
            );

            if (!response.ok) {
                let errorMessage = "Resume upload failed.";

                try {
                    const errorData = await response.json();

                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch {
                    // Keep default error message.
                }

                if (response.status === 401 || response.status === 403) {
                    errorMessage =
                        "Your session has expired. Please log in again.";
                }

                throw new Error(errorMessage);
            }

            await response.json();

            setMessage(
                "Resume uploaded successfully. Your resume intelligence has been updated."
            );

            setFile(null);

            const fileInput = document.getElementById("resume-file");

            if (fileInput) {
                fileInput.value = "";
            }

            /*
             * Refresh the profile and analysis after upload.
             */
            await loadStudent();
            await loadResumeAnalysis();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    /*
     * =========================================================
     * RE-ANALYZE RESUME
     * =========================================================
     */

    const handleReanalyze = async () => {
        if (!studentId || !token) {
            setError("Please log in before re-analyzing your resume.");
            return;
        }

        try {
            setLoading(true);
            setMessage("");
            setError("");

            const response = await fetch(
                `${API_BASE_URL}/api/resumes/reanalyze/${studentId}`,
                {
                    method: "POST",
                    headers: getAuthHeaders(),
                }
            );

            if (!response.ok) {
                let errorMessage = "Resume re-analysis failed.";

                try {
                    const errorData = await response.json();

                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch {
                    // Keep default error message.
                }

                if (response.status === 401 || response.status === 403) {
                    errorMessage =
                        "Your session has expired. Please log in again.";
                }

                throw new Error(errorMessage);
            }

            const data = await response.json();

            setAnalysis(data);

            setMessage("Resume analysis refreshed successfully.");

            await loadStudent();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    /*
     * =========================================================
     * SKILLS
     * =========================================================
     */

    const skills = student?.skills || [];

    /*
     * =========================================================
     * AUTH LOADING SCREEN
     * =========================================================
     */

    if (authLoading) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center">
                <div className="rounded-2xl border border-slate-800 bg-slate-950 px-8 py-6 text-center shadow-xl">
                    <div className="text-3xl">🔐</div>

                    <h2 className="mt-4 text-lg font-semibold text-white">
                        Checking your session...
                    </h2>

                    <p className="mt-2 text-sm text-slate-500">
                        Loading your resume intelligence.
                    </p>
                </div>
            </div>
        );
    }

    /*
     * =========================================================
     * PAGE
     * =========================================================
     */

    return (
        <div className="space-y-8">

            {/* Page Header */}

            <div>
                <p className="mb-2 text-sm font-medium text-blue-400">
                    Resume Intelligence
                </p>

                <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">

                    <div>
                        <h1 className="text-4xl font-bold tracking-tight text-white">
                            Resume
                        </h1>

                        <p className="mt-3 max-w-2xl text-base leading-7 text-slate-400">
                            Upload your resume and let the placement intelligence
                            engine extract your skills, experience, projects and
                            other important information.
                        </p>
                    </div>

                    {analysis && (
                        <button
                            onClick={handleReanalyze}
                            disabled={loading}
                            className="rounded-xl border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:border-blue-500 hover:text-blue-400 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {loading
                                ? "Analyzing..."
                                : "Re-analyze Resume"}
                        </button>
                    )}
                </div>
            </div>

            {/* Messages */}

            {message && (
                <div className="rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-5 py-4 text-sm text-emerald-400">
                    {message}
                </div>
            )}

            {error && (
                <div className="rounded-xl border border-red-500/20 bg-red-500/10 px-5 py-4 text-sm text-red-400">
                    {error}
                </div>
            )}

            {/* Upload Section */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-xl">

                <div className="mb-6">

                    <h2 className="text-xl font-semibold text-white">
                        Upload Resume
                    </h2>

                    <p className="mt-2 text-sm text-slate-400">
                        Upload a PDF resume to extract structured career
                        information.
                    </p>

                </div>

                <div className="rounded-2xl border border-dashed border-slate-700 bg-slate-900/50 p-8 text-center transition hover:border-blue-500/50">

                    <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 text-3xl">
                        📄
                    </div>

                    <h3 className="text-lg font-semibold text-white">
                        Choose your resume
                    </h3>

                    <p className="mt-2 text-sm text-slate-500">
                        PDF files only
                    </p>

                    <div className="mt-6 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">

                        <label
                            htmlFor="resume-file"
                            className="cursor-pointer rounded-xl border border-slate-700 bg-slate-800 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:border-blue-500 hover:bg-slate-800/80"
                        >
                            Choose PDF
                        </label>

                        <input
                            id="resume-file"
                            type="file"
                            accept=".pdf,application/pdf"
                            onChange={handleFileChange}
                            className="hidden"
                        />

                        <button
                            onClick={handleUpload}
                            disabled={!file || loading}
                            className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-40"
                        >
                            {loading
                                ? "Processing..."
                                : "Upload & Analyze"}
                        </button>

                    </div>

                    {file && (
                        <div className="mx-auto mt-6 max-w-md rounded-xl border border-slate-800 bg-slate-950 px-4 py-3 text-left">

                            <p className="text-xs uppercase tracking-wider text-slate-500">
                                Selected file
                            </p>

                            <p className="mt-1 truncate text-sm font-medium text-slate-200">
                                {file.name}
                            </p>

                            <p className="mt-1 text-xs text-slate-500">
                                {(file.size / 1024 / 1024).toFixed(2)} MB
                            </p>

                        </div>
                    )}

                </div>
            </section>

            {/* Skills Section */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-xl">

                <div className="mb-6">

                    <p className="text-sm font-medium text-blue-400">
                        Extracted Profile
                    </p>

                    <h2 className="mt-1 text-xl font-semibold text-white">
                        Detected Skills
                    </h2>

                    <p className="mt-2 text-sm text-slate-400">
                        Skills currently associated with your student profile.
                    </p>

                </div>

                {skills.length > 0 ? (
                    <div className="flex flex-wrap gap-3">

                        {skills.map((skill) => (
                            <span
                                key={skill.id}
                                className="rounded-full border border-blue-500/20 bg-blue-500/10 px-4 py-2 text-sm font-medium text-blue-300"
                            >
                                {skill.name}
                            </span>
                        ))}

                    </div>
                ) : (
                    <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-5 text-sm text-slate-500">
                        No extracted skills are available yet. Upload a resume
                        to build your profile.
                    </div>
                )}

            </section>

            {/* Analysis */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-xl">

                <div className="mb-7">

                    <p className="text-sm font-medium text-purple-400">
                        Intelligent Analysis
                    </p>

                    <h2 className="mt-1 text-xl font-semibold text-white">
                        Resume Analysis
                    </h2>

                    <p className="mt-2 text-sm text-slate-400">
                        Structured information extracted from your resume.
                    </p>

                </div>

                {loadingAnalysis ? (
                    <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-8 text-center text-sm text-slate-500">
                        Loading resume analysis...
                    </div>
                ) : analysis ? (
                    <div className="grid gap-5 md:grid-cols-2">

                        <AnalysisCard
                            title="Professional Summary"
                            value={analysis.summary}
                            icon="👤"
                        />

                        <AnalysisCard
                            title="Education"
                            value={analysis.education}
                            icon="🎓"
                        />

                        <AnalysisCard
                            title="Experience"
                            value={analysis.experience}
                            icon="💼"
                        />

                        <AnalysisCard
                            title="Projects"
                            value={analysis.projects}
                            icon="🚀"
                        />

                        <AnalysisCard
                            title="Certifications"
                            value={analysis.certifications}
                            icon="🏆"
                        />

                        <AnalysisCard
                            title="Achievements"
                            value={analysis.achievements}
                            icon="⭐"
                        />

                    </div>
                ) : (
                    <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-8 text-center">

                        <div className="text-4xl">
                            📄
                        </div>

                        <h3 className="mt-4 text-lg font-semibold text-white">
                            No resume analysis yet
                        </h3>

                        <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">
                            Upload your PDF resume above and the system will
                            extract your professional summary, education,
                            experience, projects, certifications and
                            achievements.
                        </p>

                    </div>
                )}

            </section>

            {/* Intelligence Flow */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-xl">

                <div className="mb-6">

                    <p className="text-sm font-medium text-emerald-400">
                        How it works
                    </p>

                    <h2 className="mt-1 text-xl font-semibold text-white">
                        Resume Intelligence Pipeline
                    </h2>

                </div>

                <div className="grid gap-4 md:grid-cols-4">

                    <PipelineStep
                        number="01"
                        title="Upload"
                        description="Your PDF resume is uploaded to the backend."
                    />

                    <PipelineStep
                        number="02"
                        title="Extract"
                        description="PDFBox extracts the text from your resume."
                    />

                    <PipelineStep
                        number="03"
                        title="Analyze"
                        description="Skills and resume sections are identified."
                    />

                    <PipelineStep
                        number="04"
                        title="Match"
                        description="The extracted profile powers job intelligence."
                    />

                </div>

            </section>

        </div>
    );
}

/*
 * =========================================================
 * ANALYSIS CARD
 * =========================================================
 */

function AnalysisCard({ title, value, icon }) {
    const displayValue =
        value && value.trim()
            ? value
            : "No information detected in this section.";

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">

            <div className="flex items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-800 text-lg">
                    {icon}
                </div>

                <h3 className="font-semibold text-white">
                    {title}
                </h3>

            </div>

            <div className="mt-5 whitespace-pre-line text-sm leading-7 text-slate-400">
                {displayValue}
            </div>

        </div>
    );
}

/*
 * =========================================================
 * PIPELINE STEP
 * =========================================================
 */

function PipelineStep({ number, title, description }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">

            <span className="text-xs font-bold tracking-widest text-blue-400">
                {number}
            </span>

            <h3 className="mt-3 font-semibold text-white">
                {title}
            </h3>

            <p className="mt-2 text-sm leading-6 text-slate-500">
                {description}
            </p>

        </div>
    );
}

export default Resume;