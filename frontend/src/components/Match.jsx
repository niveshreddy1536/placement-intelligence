import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080'

function ScoreCard({ label, value, description }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6">
            <p className="text-sm font-medium text-slate-400">
                {label}
            </p>

            <div className="mt-3 flex items-end gap-1">
                <span className="text-3xl font-bold text-white">
                    {Number(value || 0).toFixed(1)}
                </span>

                <span className="mb-1 text-sm text-slate-500">
                    %
                </span>
            </div>

            {description && (
                <p className="mt-2 text-sm text-slate-500">
                    {description}
                </p>
            )}
        </div>
    )
}

function SectionTitle({
                          eyebrow,
                          title,
                          description,
                      }) {
    return (
        <div className="mb-5">
            {eyebrow && (
                <p className="mb-1 text-xs font-semibold uppercase tracking-wider text-indigo-400">
                    {eyebrow}
                </p>
            )}

            <h2 className="text-xl font-semibold text-white">
                {title}
            </h2>

            {description && (
                <p className="mt-1 text-sm text-slate-500">
                    {description}
                </p>
            )}
        </div>
    )
}

function SkillBadge({
                        children,
                        type = 'default',
                    }) {
    const styles = {
        matched:
            'border-emerald-500/20 bg-emerald-500/10 text-emerald-300',

        missing:
            'border-red-500/20 bg-red-500/10 text-red-300',

        default:
            'border-slate-700 bg-slate-800/60 text-slate-300',
    }

    return (
        <span
            className={`inline-flex rounded-full border px-3 py-1.5 text-sm ${styles[type]}`}
        >
            {children}
        </span>
    )
}

function displayItem(item) {
    if (typeof item === 'string') {
        return item
    }

    if (!item) {
        return ''
    }

    return (
        item.text ||
        item.description ||
        item.reason ||
        item.title ||
        JSON.stringify(item)
    )
}

function Match() {
    const [searchParams] = useSearchParams()

    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const jobFromUrl = searchParams.get('job')

    const [jobs, setJobs] = useState([])

    const [selectedJobId, setSelectedJobId] =
        useState(jobFromUrl || '')

    const [matchResult, setMatchResult] =
        useState(null)

    const [loadingJobs, setLoadingJobs] =
        useState(true)

    const [loadingMatch, setLoadingMatch] =
        useState(false)

    const [externalLoading, setExternalLoading] =
        useState(false)

    const [error, setError] =
        useState('')

    const [mode, setMode] =
        useState(
            jobFromUrl
                ? 'pool'
                : 'external'
        )

    const [externalTitle, setExternalTitle] =
        useState('')

    const [externalDescription, setExternalDescription] =
        useState('')

    /*
     * =========================================================
     * LOAD SAVED JOBS
     * =========================================================
     */

    useEffect(() => {
        if (
            authLoading ||
            !token ||
            !studentId
        ) {
            return
        }

        async function loadJobsForStudent() {
            try {
                setLoadingJobs(true)
                setError('')

                const response = await fetch(
                    `${API_BASE_URL}/api/job-descriptions`,
                    {
                        headers: {
                            Authorization:
                                `Bearer ${token}`,
                        },
                    }
                )

                if (!response.ok) {
                    const message =
                        await response.text()

                    throw new Error(
                        message ||
                        'Unable to load saved job descriptions.'
                    )
                }

                const data =
                    await response.json()

                const jobList =
                    Array.isArray(data)
                        ? data
                        : []

                setJobs(jobList)

                /*
                 * If a job ID was supplied in the URL,
                 * use that job when it exists.
                 */
                if (
                    jobFromUrl &&
                    jobList.some(
                        (job) =>
                            String(job.id) ===
                            String(jobFromUrl)
                    )
                ) {
                    setSelectedJobId(
                        String(jobFromUrl)
                    )

                    setMode('pool')
                } else if (
                    jobList.length > 0
                ) {
                    /*
                     * Otherwise select the first saved job.
                     */
                    setSelectedJobId(
                        String(jobList[0].id)
                    )
                } else {
                    setSelectedJobId('')
                }
            } catch (err) {
                console.error(err)

                setError(
                    err.message ||
                    'Unable to load job descriptions.'
                )
            } finally {
                setLoadingJobs(false)
            }
        }

        loadJobsForStudent()
    }, [
        authLoading,
        token,
        studentId,
        jobFromUrl,
    ])

    /*
     * =========================================================
     * ANALYZE SAVED JOB
     * =========================================================
     */

    async function analyzeMatch() {
        if (!studentId || !token) {
            setError(
                'Please login again before analyzing a job.'
            )

            return
        }

        if (!selectedJobId) {
            setError(
                'Please select a job first.'
            )

            return
        }

        try {
            setLoadingMatch(true)
            setError('')
            setMatchResult(null)

            const response = await fetch(
                `${API_BASE_URL}/api/matching/student/${studentId}/job/${selectedJobId}`,
                {
                    headers: {
                        Authorization:
                            `Bearer ${token}`,
                    },
                }
            )

            if (!response.ok) {
                const message =
                    await response.text()

                throw new Error(
                    message ||
                    'Unable to analyze the selected job.'
                )
            }

            const data =
                await response.json()

            setMatchResult(data)
        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to analyze this job.'
            )
        } finally {
            setLoadingMatch(false)
        }
    }

    /*
     * =========================================================
     * ANALYZE EXTERNAL JOB
     * =========================================================
     */

    async function analyzeExternalJob() {
        if (!studentId || !token) {
            setError(
                'Please login again before analyzing a job.'
            )

            return
        }

        if (!externalTitle.trim()) {
            setError(
                'Please enter the job title.'
            )

            return
        }

        if (!externalDescription.trim()) {
            setError(
                'Please paste the job description.'
            )

            return
        }

        try {
            setExternalLoading(true)
            setError('')
            setMatchResult(null)

            const response = await fetch(
                `${API_BASE_URL}/api/matching/student/${studentId}/external`,
                {
                    method: 'POST',

                    headers: {
                        'Content-Type':
                            'application/json',

                        Authorization:
                            `Bearer ${token}`,
                    },

                    body: JSON.stringify({
                        title:
                        externalTitle,

                        description:
                        externalDescription,
                    }),
                }
            )

            if (!response.ok) {
                const message =
                    await response.text()

                throw new Error(
                    message ||
                    'Unable to analyze the external job.'
                )
            }

            const data =
                await response.json()

            setMatchResult(data)
        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to analyze this job.'
            )
        } finally {
            setExternalLoading(false)
        }
    }

    /*
     * =========================================================
     * SELECTED JOB
     * =========================================================
     */

    const selectedJob =
        jobs.find(
            (job) =>
                String(job.id) ===
                String(selectedJobId)
        )

    /*
     * =========================================================
     * RESULT DATA
     * =========================================================
     */

    const matchedSkills =
        Array.isArray(
            matchResult?.matchedSkills
        )
            ? matchResult.matchedSkills
            : []

    const missingSkills =
        Array.isArray(
            matchResult?.missingSkills
        )
            ? matchResult.missingSkills
            : []

    const requiredSkills =
        Array.isArray(
            matchResult?.requiredSkills
        )
            ? matchResult.requiredSkills
            : []

    const relevantProjects =
        Array.isArray(
            matchResult?.relevantProjects
        )
            ? matchResult.relevantProjects
            : []

    const strengths =
        Array.isArray(
            matchResult?.strengths
        )
            ? matchResult.strengths
            : []

    const weaknesses =
        Array.isArray(
            matchResult?.weaknesses
        )
            ? matchResult.weaknesses
            : []

    const recommendations =
        Array.isArray(
            matchResult?.recommendations
        )
            ? matchResult.recommendations
            : []

    const matchPercentage =
        Number(
            matchResult?.matchPercentage || 0
        )

    /*
     * =========================================================
     * AUTH LOADING
     * =========================================================
     */

    if (authLoading) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center bg-slate-950 px-6">
                <div className="text-center">
                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-indigo-500" />

                    <p className="mt-4 text-sm text-slate-400">
                        Loading your secure session...
                    </p>
                </div>
            </div>
        )
    }

    /*
     * =========================================================
     * NO SESSION
     * =========================================================
     */

    if (!studentId || !token) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center bg-slate-950 px-6">
                <div className="max-w-md rounded-2xl border border-red-500/20 bg-red-500/10 p-6 text-center">

                    <h2 className="text-xl font-semibold text-red-400">
                        Session required
                    </h2>

                    <p className="mt-2 text-sm leading-6 text-slate-400">
                        Please log in again to use Job Match.
                    </p>

                    <Link
                        to="/login"
                        className="mt-5 inline-flex rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-indigo-500"
                    >
                        Go to Login
                    </Link>

                </div>
            </div>
        )
    }

    /*
     * =========================================================
     * PAGE
     * =========================================================
     */

    return (
        <div className="min-h-screen bg-slate-950 px-4 py-6 text-white md:px-8 lg:px-10">

            <div className="mx-auto max-w-7xl">

                {/* HEADER */}

                <div className="mb-8">
                    <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">

                        <div>

                            <p className="mb-2 text-sm font-medium text-indigo-400">
                                Intelligent Matching
                            </p>

                            <h1 className="text-3xl font-bold tracking-tight md:text-4xl">
                                Job Match
                            </h1>

                            <p className="mt-2 max-w-2xl text-slate-400">
                                Compare your resume with a job description
                                and understand how well your profile fits
                                the opportunity.
                            </p>

                        </div>

                        <Link
                            to="/job-descriptions"
                            className="inline-flex w-fit items-center rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:border-indigo-500/50 hover:bg-slate-800 hover:text-white"
                        >
                            Manage Job Pool
                        </Link>

                    </div>
                </div>

                {/* MODE SWITCHER */}

                <div className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-2">

                    <div className="grid gap-2 md:grid-cols-2">

                        <button
                            type="button"
                            onClick={() => {
                                setMode('external')
                                setError('')
                                setMatchResult(null)
                            }}
                            className={`rounded-xl px-5 py-4 text-left transition ${
                                mode === 'external'
                                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/10'
                                    : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                            }`}
                        >

                            <div className="font-semibold">
                                ⚡ Analyze Any Job
                            </div>

                            <div
                                className={`mt-1 text-sm ${
                                    mode === 'external'
                                        ? 'text-indigo-100'
                                        : 'text-slate-500'
                                }`}
                            >
                                Paste a job description from any source
                            </div>

                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setMode('pool')
                                setError('')
                                setMatchResult(null)
                            }}
                            className={`rounded-xl px-5 py-4 text-left transition ${
                                mode === 'pool'
                                    ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/10'
                                    : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                            }`}
                        >

                            <div className="font-semibold">
                                📋 Job Pool
                            </div>

                            <div
                                className={`mt-1 text-sm ${
                                    mode === 'pool'
                                        ? 'text-indigo-100'
                                        : 'text-slate-500'
                                }`}
                            >
                                Analyze jobs already saved in your system
                            </div>

                        </button>

                    </div>
                </div>

                {/* EXTERNAL JOB */}

                {mode === 'external' && (
                    <div className="mb-8 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 md:p-8">

                        <SectionTitle
                            eyebrow="External Job"
                            title="Analyze a Job Description"
                            description="Paste a job description from LinkedIn, company websites, placement portals, or any other source."
                        />

                        <div className="space-y-5">

                            <div>

                                <label className="mb-2 block text-sm font-medium text-slate-300">
                                    Job Title
                                </label>

                                <input
                                    type="text"
                                    value={externalTitle}
                                    onChange={(event) =>
                                        setExternalTitle(
                                            event.target.value
                                        )
                                    }
                                    placeholder="e.g. Java Backend Developer"
                                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none transition placeholder:text-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                                />

                            </div>

                            <div>

                                <div className="mb-2 flex items-center justify-between gap-3">

                                    <label className="block text-sm font-medium text-slate-300">
                                        Job Description
                                    </label>

                                    <span className="text-xs text-slate-600">
                                        Paste the complete JD for better analysis
                                    </span>

                                </div>

                                <textarea
                                    value={externalDescription}
                                    onChange={(event) =>
                                        setExternalDescription(
                                            event.target.value
                                        )
                                    }
                                    placeholder="Paste the complete job description here..."
                                    rows={12}
                                    className="w-full resize-y rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20"
                                />

                            </div>

                            <div className="flex flex-col gap-3 sm:flex-row">

                                <button
                                    type="button"
                                    onClick={analyzeExternalJob}
                                    disabled={externalLoading}
                                    className="rounded-xl bg-indigo-600 px-6 py-3 font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {externalLoading
                                        ? 'Analyzing...'
                                        : 'Analyze Job Match'}
                                </button>

                                <button
                                    type="button"
                                    onClick={() => {
                                        setExternalTitle('')
                                        setExternalDescription('')
                                        setMatchResult(null)
                                        setError('')
                                    }}
                                    className="rounded-xl border border-slate-700 px-6 py-3 font-medium text-slate-300 transition hover:bg-slate-800 hover:text-white"
                                >
                                    Clear
                                </button>

                            </div>

                        </div>
                    </div>
                )}

                {/* JOB POOL */}

                {mode === 'pool' && (
                    <div className="mb-8 rounded-2xl border border-slate-800 bg-slate-900/70 p-6 md:p-8">

                        <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">

                            <SectionTitle
                                eyebrow="Saved Opportunities"
                                title="Select a Job"
                                description="Choose one of the job descriptions stored in your Job Pool."
                            />

                            <Link
                                to="/job-descriptions"
                                className="mb-5 inline-flex w-fit rounded-xl border border-slate-700 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-slate-800 hover:text-white"
                            >
                                + Add New Job
                            </Link>

                        </div>

                        {loadingJobs ? (
                            <div className="rounded-xl border border-slate-800 bg-slate-950 p-5 text-sm text-slate-500">
                                Loading saved jobs...
                            </div>
                        ) : jobs.length === 0 ? (
                            <div className="rounded-xl border border-dashed border-slate-700 bg-slate-950 p-8 text-center">

                                <div className="text-3xl">
                                    📋
                                </div>

                                <h3 className="mt-3 font-semibold text-white">
                                    No saved jobs yet
                                </h3>

                                <p className="mx-auto mt-2 max-w-md text-sm text-slate-500">
                                    Add job descriptions to your Job Pool,
                                    then compare them against your resume.
                                </p>

                                <Link
                                    to="/job-descriptions"
                                    className="mt-5 inline-flex rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-indigo-500"
                                >
                                    Add Job Description
                                </Link>

                            </div>
                        ) : (
                            <div className="space-y-4">

                                <select
                                    value={selectedJobId}
                                    onChange={(event) => {
                                        setSelectedJobId(
                                            event.target.value
                                        )

                                        setMatchResult(null)
                                        setError('')
                                    }}
                                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-white outline-none focus:border-indigo-500"
                                >

                                    {jobs.map((job) => (
                                        <option
                                            key={job.id}
                                            value={job.id}
                                        >
                                            {job.title}

                                            {job.companyName
                                                ? ` — ${job.companyName}`
                                                : ''}
                                        </option>
                                    ))}

                                </select>

                                {selectedJob && (
                                    <div className="rounded-xl border border-slate-800 bg-slate-950 p-5">

                                        <div className="flex flex-col justify-between gap-4 md:flex-row">

                                            <div>

                                                <h3 className="text-lg font-semibold text-white">
                                                    {selectedJob.title}
                                                </h3>

                                                {selectedJob.companyName && (
                                                    <p className="mt-1 text-sm text-slate-400">
                                                        {selectedJob.companyName}
                                                    </p>
                                                )}

                                            </div>

                                            <div className="flex flex-wrap gap-2">

                                                {selectedJob.location && (
                                                    <span className="rounded-full bg-slate-800 px-3 py-1 text-xs text-slate-400">
                                                        📍 {selectedJob.location}
                                                    </span>
                                                )}

                                                {selectedJob.jobType && (
                                                    <span className="rounded-full bg-slate-800 px-3 py-1 text-xs text-slate-400">
                                                        💼 {selectedJob.jobType}
                                                    </span>
                                                )}

                                                {selectedJob.workMode && (
                                                    <span className="rounded-full bg-slate-800 px-3 py-1 text-xs text-slate-400">
                                                        🏢 {selectedJob.workMode}
                                                    </span>
                                                )}

                                            </div>

                                        </div>

                                        {selectedJob.description && (
                                            <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-500">
                                                {selectedJob.description}
                                            </p>
                                        )}

                                        <button
                                            type="button"
                                            onClick={analyzeMatch}
                                            disabled={loadingMatch}
                                            className="mt-5 rounded-xl bg-indigo-600 px-6 py-3 font-semibold text-white transition hover:bg-indigo-500 disabled:cursor-not-allowed disabled:opacity-60"
                                        >
                                            {loadingMatch
                                                ? 'Analyzing...'
                                                : 'Analyze Match'}
                                        </button>

                                    </div>
                                )}

                            </div>
                        )}

                    </div>
                )}

                {/* ERROR */}

                {error && (
                    <div className="mb-8 rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300">

                        <div className="flex items-start gap-3">

                            <span>
                                ⚠️
                            </span>

                            <div>

                                <p className="font-medium">
                                    Something went wrong
                                </p>

                                <p className="mt-1 text-red-300/80">
                                    {error}
                                </p>

                            </div>

                        </div>
                    </div>
                )}

                {/* EMPTY STATE */}

                {!matchResult &&
                    !loadingMatch &&
                    !externalLoading &&
                    !error &&
                    mode === 'external' &&
                    !externalDescription && (
                        <div className="rounded-2xl border border-dashed border-slate-800 bg-slate-900/40 p-10 text-center">

                            <div className="text-4xl">
                                🎯
                            </div>

                            <h2 className="mt-4 text-xl font-semibold text-white">
                                Ready to find your match?
                            </h2>

                            <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-500">
                                Paste a job description above and our
                                matching engine will compare it with your
                                resume, skills, projects, experience and
                                education.
                            </p>

                        </div>
                    )}

                {/* LOADING */}

                {(loadingMatch ||
                    externalLoading) && (
                    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-10 text-center">

                        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-indigo-500" />

                        <h2 className="mt-5 font-semibold text-white">
                            Analyzing your match...
                        </h2>

                        <p className="mt-2 text-sm text-slate-500">
                            Comparing your profile with the job
                            requirements.
                        </p>

                    </div>
                )}

                {/* MATCH RESULT */}

                {matchResult &&
                    !loadingMatch &&
                    !externalLoading && (
                        <div className="space-y-8">

                            {/* MAIN SCORE */}

                            <div className="overflow-hidden rounded-3xl border border-slate-800 bg-gradient-to-br from-slate-900 to-slate-950">

                                <div className="p-6 md:p-8">

                                    <div className="flex flex-col justify-between gap-8 md:flex-row md:items-center">

                                        <div>

                                            <p className="text-sm font-medium text-indigo-400">
                                                Match Analysis
                                            </p>

                                            <h2 className="mt-2 text-2xl font-bold text-white md:text-3xl">
                                                {mode === 'external'
                                                    ? externalTitle
                                                    : selectedJob?.title ||
                                                    'Selected Job'}
                                            </h2>

                                            {mode === 'pool' &&
                                                selectedJob?.companyName && (
                                                    <p className="mt-2 text-slate-400">
                                                        {selectedJob.companyName}
                                                    </p>
                                                )}

                                            {mode === 'external' && (
                                                <p className="mt-2 text-slate-500">
                                                    External job analysis
                                                </p>
                                            )}

                                        </div>

                                        <div className="flex items-center gap-5">

                                            <div className="flex h-32 w-32 flex-col items-center justify-center rounded-full border-8 border-indigo-500/30 bg-indigo-500/10">

                                                <span className="text-3xl font-bold text-white">
                                                    {matchPercentage.toFixed(
                                                        1
                                                    )}
                                                </span>

                                                <span className="text-sm text-slate-500">
                                                    %
                                                </span>

                                            </div>

                                            <div>

                                                <p className="text-sm text-slate-500">
                                                    Match Level
                                                </p>

                                                <p className="mt-1 text-xl font-semibold text-white">
                                                    {matchResult.matchLevel ||
                                                        'ANALYZED'}
                                                </p>

                                                <p className="mt-1 max-w-xs text-sm text-slate-500">
                                                    Overall compatibility
                                                    between your profile and
                                                    this opportunity.
                                                </p>

                                            </div>

                                        </div>

                                    </div>

                                </div>

                            </div>

                            {/* SCORE BREAKDOWN */}

                            <div>

                                <SectionTitle
                                    eyebrow="Compatibility"
                                    title="Score Breakdown"
                                    description="See which areas are contributing to your overall match."
                                />

                                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">

                                    <ScoreCard
                                        label="Skill Match"
                                        value={
                                            matchResult.skillScore
                                        }
                                        description="Required skills you already demonstrate"
                                    />

                                    <ScoreCard
                                        label="Project Relevance"
                                        value={
                                            matchResult.projectScore
                                        }
                                        description="How strongly your projects support this role"
                                    />

                                    <ScoreCard
                                        label="Experience"
                                        value={
                                            matchResult.experienceScore
                                        }
                                        description="Relevant evidence from your experience"
                                    />

                                    <ScoreCard
                                        label="Education"
                                        value={
                                            matchResult.educationCertificationScore
                                        }
                                        description="Education and certification evidence"
                                    />

                                    <ScoreCard
                                        label="Overall Match"
                                        value={
                                            matchResult.matchPercentage
                                        }
                                        description="Combined compatibility score"
                                    />

                                </div>
                            </div>

                            {/* MATCHED + MISSING */}

                            <div className="grid gap-6 lg:grid-cols-2">

                                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">

                                    <SectionTitle
                                        eyebrow="Strengths"
                                        title="Matched Skills"
                                        description="Skills from your profile that align with the job."
                                    />

                                    {matchedSkills.length === 0 ? (
                                        <p className="text-sm text-slate-500">
                                            No directly matched skills were
                                            identified.
                                        </p>
                                    ) : (
                                        <div className="flex flex-wrap gap-2">

                                            {matchedSkills.map(
                                                (skill, index) => (
                                                    <SkillBadge
                                                        key={`${skill}-${index}`}
                                                        type="matched"
                                                    >
                                                        ✓ {skill}
                                                    </SkillBadge>
                                                )
                                            )}

                                        </div>
                                    )}

                                </div>

                                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">

                                    <SectionTitle
                                        eyebrow="Skill Gaps"
                                        title="Missing Skills"
                                        description="Skills that could improve your fit for this opportunity."
                                    />

                                    {missingSkills.length === 0 ? (
                                        <p className="text-sm text-emerald-400">
                                            Excellent — no major missing
                                            skills were identified.
                                        </p>
                                    ) : (
                                        <div className="flex flex-wrap gap-2">

                                            {missingSkills.map(
                                                (skill, index) => (
                                                    <SkillBadge
                                                        key={`${skill}-${index}`}
                                                        type="missing"
                                                    >
                                                        + {skill}
                                                    </SkillBadge>
                                                )
                                            )}

                                        </div>
                                    )}

                                </div>

                            </div>

                            {/* REQUIRED SKILLS */}

                            {requiredSkills.length > 0 && (
                                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">

                                    <SectionTitle
                                        eyebrow="Job Requirements"
                                        title="Required Skills"
                                        description="Skills identified from the job description."
                                    />

                                    <div className="flex flex-wrap gap-2">

                                        {requiredSkills.map(
                                            (skill, index) => {

                                                const isMatched =
                                                    matchedSkills.some(
                                                        (item) =>
                                                            String(
                                                                item
                                                            ).toLowerCase() ===
                                                            String(
                                                                skill
                                                            ).toLowerCase()
                                                    )

                                                return (
                                                    <SkillBadge
                                                        key={`${skill}-${index}`}
                                                        type={
                                                            isMatched
                                                                ? 'matched'
                                                                : 'default'
                                                        }
                                                    >
                                                        {isMatched
                                                            ? '✓ '
                                                            : ''}

                                                        {skill}
                                                    </SkillBadge>
                                                )
                                            }
                                        )}

                                    </div>

                                </div>
                            )}

                            {/* RELEVANT PROJECTS */}

                            {relevantProjects.length > 0 && (
                                <div>

                                    <SectionTitle
                                        eyebrow="Experience Evidence"
                                        title="Relevant Projects"
                                        description="Projects from your resume that support your fit for this role."
                                    />

                                    <div className="grid gap-4 md:grid-cols-2">

                                        {relevantProjects.map(
                                            (project, index) => {

                                                const isString =
                                                    typeof project ===
                                                    'string'

                                                const projectText =
                                                    isString
                                                        ? project
                                                        : ''

                                                const relevanceMatch =
                                                    projectText.match(
                                                        /\((\d+(?:\.\d+)?)%\s*relevance\)/i
                                                    )

                                                const projectName =
                                                    isString
                                                        ? projectText.replace(
                                                            /\s*\(\d+(?:\.\d+)?%\s*relevance\)\s*$/i,
                                                            ''
                                                        ).trim() ||
                                                        `Project ${index + 1}`
                                                        : project.project ||
                                                        project.projectName ||
                                                        project.name ||
                                                        `Project ${index + 1}`

                                                const projectScore =
                                                    isString
                                                        ? relevanceMatch
                                                            ? Number(
                                                                relevanceMatch[1]
                                                            )
                                                            : undefined
                                                        : project.score ??
                                                        project.projectScore ??
                                                        project.matchScore ??
                                                        project.relevance

                                                const projectSkills =
                                                    isString
                                                        ? []
                                                        : project.matchedSkills ||
                                                        project.skills ||
                                                        []

                                                const projectDescription =
                                                    isString
                                                        ? ''
                                                        : project.description ||
                                                        ''

                                                const projectReason =
                                                    isString
                                                        ? 'Matched using project skill and role evidence.'
                                                        : project.reason ||
                                                        project.whyRelevant ||
                                                        ''

                                                return (
                                                    <div
                                                        key={`${projectName}-${index}`}
                                                        className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"
                                                    >

                                                        <div className="flex items-start justify-between gap-4">

                                                            <div className="min-w-0">

                                                                <h3 className="font-semibold text-white">
                                                                    {projectName}
                                                                </h3>

                                                                {projectDescription && (
                                                                    <p className="mt-2 text-sm leading-6 text-slate-500">
                                                                        {projectDescription}
                                                                    </p>
                                                                )}

                                                            </div>

                                                            {projectScore !==
                                                                undefined && (
                                                                    <div className="shrink-0 rounded-lg bg-indigo-500/10 px-3 py-1.5 text-sm font-semibold text-indigo-300">
                                                                        {Number(
                                                                            projectScore
                                                                        ).toFixed(
                                                                            1
                                                                        )}
                                                                        %
                                                                    </div>
                                                                )}

                                                        </div>

                                                        {projectSkills.length >
                                                            0 && (
                                                                <div className="mt-4 flex flex-wrap gap-2">

                                                                    {projectSkills.map(
                                                                        (
                                                                            skill,
                                                                            skillIndex
                                                                        ) => (
                                                                            <span
                                                                                key={`${skill}-${skillIndex}`}
                                                                                className="rounded-full bg-slate-800 px-2.5 py-1 text-xs text-slate-400"
                                                                            >
                                                                            {
                                                                                skill
                                                                            }
                                                                        </span>
                                                                        )
                                                                    )}

                                                                </div>
                                                            )}

                                                        {projectReason && (
                                                            <p className="mt-4 border-t border-slate-800 pt-4 text-sm text-slate-500">
                                                                {
                                                                    projectReason
                                                                }
                                                            </p>
                                                        )}

                                                    </div>
                                                )
                                            }
                                        )}

                                    </div>
                                </div>
                            )}

                            {/* STRENGTHS / WEAKNESSES */}

                            <div className="grid gap-6 lg:grid-cols-2">

                                {strengths.length > 0 && (
                                    <div className="rounded-2xl border border-emerald-500/10 bg-emerald-500/5 p-6">

                                        <SectionTitle
                                            eyebrow="What You Already Have"
                                            title="Strengths"
                                        />

                                        <div className="space-y-3">

                                            {strengths.map(
                                                (item, index) => (
                                                    <div
                                                        key={index}
                                                        className="flex gap-3"
                                                    >

                                                        <span className="mt-0.5 text-emerald-400">
                                                            ✓
                                                        </span>

                                                        <p className="text-sm leading-6 text-slate-300">
                                                            {displayItem(
                                                                item
                                                            )}
                                                        </p>

                                                    </div>
                                                )
                                            )}

                                        </div>

                                    </div>
                                )}

                                {weaknesses.length > 0 && (
                                    <div className="rounded-2xl border border-amber-500/10 bg-amber-500/5 p-6">

                                        <SectionTitle
                                            eyebrow="Areas To Improve"
                                            title="Weaknesses"
                                        />

                                        <div className="space-y-3">

                                            {weaknesses.map(
                                                (item, index) => (
                                                    <div
                                                        key={index}
                                                        className="flex gap-3"
                                                    >

                                                        <span className="mt-0.5 text-amber-400">
                                                            !
                                                        </span>

                                                        <p className="text-sm leading-6 text-slate-300">
                                                            {displayItem(
                                                                item
                                                            )}
                                                        </p>

                                                    </div>
                                                )
                                            )}

                                        </div>

                                    </div>
                                )}

                            </div>

                            {/* RECOMMENDATIONS */}

                            {recommendations.length > 0 && (
                                <div className="rounded-2xl border border-indigo-500/20 bg-indigo-500/5 p-6 md:p-8">

                                    <SectionTitle
                                        eyebrow="Next Steps"
                                        title="Recommendations"
                                        description="Actions that can improve your readiness for this opportunity."
                                    />

                                    <div className="space-y-4">

                                        {recommendations.map(
                                            (item, index) => (
                                                <div
                                                    key={index}
                                                    className="flex gap-4 rounded-xl border border-slate-800 bg-slate-950/60 p-4"
                                                >

                                                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-500/10 text-sm font-semibold text-indigo-300">
                                                        {index + 1}
                                                    </div>

                                                    <p className="text-sm leading-6 text-slate-300">
                                                        {displayItem(
                                                            item
                                                        )}
                                                    </p>

                                                </div>
                                            )
                                        )}

                                    </div>

                                </div>
                            )}

                            {/* BOTTOM ACTIONS */}

                            <div className="flex flex-col gap-3 border-t border-slate-800 pt-6 sm:flex-row sm:items-center sm:justify-between">

                                <div>

                                    <p className="font-medium text-white">
                                        Want to improve this match?
                                    </p>

                                    <p className="mt-1 text-sm text-slate-500">
                                        Work on your missing skills and
                                        strengthen your project evidence.
                                    </p>

                                </div>

                                <div className="flex flex-col gap-3 sm:flex-row">

                                    <Link
                                        to="/skill-gaps"
                                        className="rounded-xl border border-slate-700 px-5 py-3 text-center text-sm font-semibold text-slate-300 transition hover:bg-slate-800 hover:text-white"
                                    >
                                        View Skill Gaps
                                    </Link>

                                    <Link
                                        to="/roadmap"
                                        className="rounded-xl bg-indigo-600 px-5 py-3 text-center text-sm font-semibold text-white transition hover:bg-indigo-500"
                                    >
                                        Open Learning Roadmap
                                    </Link>

                                </div>

                            </div>

                        </div>
                    )}

            </div>
        </div>
    )
}

export default Match