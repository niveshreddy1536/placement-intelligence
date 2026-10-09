import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
    BriefcaseBusiness,
    CheckCircle2,
    AlertTriangle,
    FileText,
    ArrowUpRight,
    Loader2,
    Target,
    ClipboardList,
} from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080/api'

function Dashboard() {
    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [student, setStudent] = useState(null)
    const [applications, setApplications] = useState([])
    const [recommendations, setRecommendations] = useState([])
    const [jobPool, setJobPool] = useState([])
    const [poolMatches, setPoolMatches] = useState([])

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        if (authLoading) {
            return
        }

        if (!studentId || !token) {
            setLoading(false)
            return
        }

        loadDashboard()
    }, [authLoading, studentId, token])

    async function loadDashboard() {
        if (!studentId || !token) {
            return
        }

        try {
            setLoading(true)
            setError('')

            const headers = {
                Authorization: `Bearer ${token}`,
            }

            const [
                studentResponse,
                applicationsResponse,
                recommendationsResponse,
                jobPoolResponse,
            ] = await Promise.all([
                fetch(
                    `${API_BASE_URL}/students/${studentId}`,
                    {
                        headers,
                    }
                ),

                fetch(
                    `${API_BASE_URL}/applications/student/${studentId}`,
                    {
                        headers,
                    }
                ),

                fetch(
                    `${API_BASE_URL}/job-recommendations/student/${studentId}`,
                    {
                        headers,
                    }
                ),

                fetch(
                    `${API_BASE_URL}/job-descriptions`,
                    {
                        headers,
                    }
                ),
            ])

            if (!studentResponse.ok) {
                throw new Error(
                    'Unable to load student information.'
                )
            }

            if (!applicationsResponse.ok) {
                throw new Error(
                    'Unable to load applications.'
                )
            }

            if (!recommendationsResponse.ok) {
                throw new Error(
                    'Unable to load job recommendations.'
                )
            }

            if (!jobPoolResponse.ok) {
                throw new Error(
                    'Unable to load the job pool.'
                )
            }

            const studentData =
                await studentResponse.json()

            const applicationsData =
                await applicationsResponse.json()

            const recommendationsData =
                await recommendationsResponse.json()

            const jobPoolData =
                await jobPoolResponse.json()

            setStudent(studentData)

            setApplications(
                Array.isArray(applicationsData)
                    ? applicationsData
                    : []
            )

            setRecommendations(
                Array.isArray(recommendationsData)
                    ? recommendationsData
                    : []
            )

            const savedJobs =
                Array.isArray(jobPoolData)
                    ? jobPoolData
                    : []

            setJobPool(savedJobs)

            /*
             * Calculate matches for the saved Job Pool.
             */
            const matchResults =
                await Promise.all(
                    savedJobs.map(
                        async (job) => {
                            try {
                                const response =
                                    await fetch(
                                        `${API_BASE_URL}/matching/student/${studentId}/job/${job.id}`,
                                        {
                                            headers,
                                        }
                                    )

                                if (!response.ok) {
                                    return null
                                }

                                const match =
                                    await response.json()

                                return {
                                    ...match,

                                    companyName:
                                    job.companyName,

                                    location:
                                    job.location,

                                    jobType:
                                    job.jobType,

                                    salary:
                                    job.salary,

                                    jobDescriptionId:
                                    job.id,
                                }
                            } catch {
                                return null
                            }
                        }
                    )
                )

            const validMatches =
                matchResults
                    .filter(Boolean)
                    .sort(
                        (a, b) =>
                            Number(
                                b.matchPercentage || 0
                            ) -
                            Number(
                                a.matchPercentage || 0
                            )
                    )

            setPoolMatches(validMatches)
        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Something went wrong while loading the dashboard.'
            )
        } finally {
            setLoading(false)
        }
    }

    /*
     * =========================================================
     * DASHBOARD CALCULATIONS
     * =========================================================
     */

    const fallbackRecommendation =
        recommendations.length > 0
            ? recommendations[0]
            : null

    const topPoolMatch =
        poolMatches.length > 0
            ? poolMatches[0]
            : null

    const topRecommendation =
        topPoolMatch ||
        fallbackRecommendation

    const skillCount =
        student?.skills?.length || 0

    const applicationCount =
        applications.length

    const jobPoolCount =
        jobPool.length

    const averageMatch =
        poolMatches.length > 0
            ? poolMatches.reduce(
            (total, job) =>
                total +
                Number(
                    job.matchPercentage || 0
                ),
            0
        ) / poolMatches.length
            : recommendations.length > 0
                ? recommendations.reduce(
                (total, job) =>
                    total +
                    Number(
                        job.matchPercentage || 0
                    ),
                0
            ) / recommendations.length
                : 0

    const highestMatch =
        Number(
            topRecommendation?.matchPercentage || 0
        )

    function formatMatch(value) {
        return `${Number(value).toFixed(1)}%`
    }

    function getStatusClass(status) {
        const normalizedStatus =
            String(status || '').toUpperCase()

        if (
            normalizedStatus === 'APPLIED'
        ) {
            return 'bg-blue-500/10 text-blue-400 border-blue-500/20'
        }

        if (
            normalizedStatus === 'INTERVIEW'
        ) {
            return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20'
        }

        if (
            normalizedStatus === 'SELECTED' ||
            normalizedStatus === 'OFFER'
        ) {
            return 'bg-green-500/10 text-green-400 border-green-500/20'
        }

        if (
            normalizedStatus === 'REJECTED' ||
            normalizedStatus === 'DECLINED'
        ) {
            return 'bg-red-500/10 text-red-400 border-red-500/20'
        }

        return 'bg-slate-800 text-slate-300 border-slate-700'
    }

    function getApplicationJob(application) {
        return (
            application?.jobDescription ||
            application?.job ||
            null
        )
    }

    /*
     * =========================================================
     * LOADING
     * =========================================================
     */

    if (authLoading || loading) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center">
                <div className="flex items-center gap-3 text-slate-400">
                    <Loader2
                        size={24}
                        className="animate-spin"
                    />

                    <span>
                        Loading your placement dashboard...
                    </span>
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
            <div className="rounded-2xl border border-yellow-500/20 bg-yellow-500/10 p-6">
                <h2 className="font-semibold text-yellow-300">
                    Session required
                </h2>

                <p className="mt-2 text-sm text-yellow-200/80">
                    Please log in to view your placement dashboard.
                </p>

                <Link
                    to="/login"
                    className="mt-4 inline-flex rounded-lg bg-blue-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-400"
                >
                    Go to Login
                </Link>
            </div>
        )
    }

    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    if (error) {
        return (
            <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6">
                <div className="flex items-start gap-3">
                    <AlertTriangle
                        className="mt-0.5 text-red-400"
                        size={22}
                    />

                    <div>
                        <h2 className="font-semibold text-red-300">
                            Unable to load dashboard
                        </h2>

                        <p className="mt-1 text-sm text-red-200/80">
                            {error}
                        </p>

                        <button
                            type="button"
                            onClick={loadDashboard}
                            className="mt-4 rounded-lg bg-red-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-400"
                        >
                            Try Again
                        </button>
                    </div>
                </div>
            </div>
        )
    }

    return (
        <div className="space-y-8">

            {/* =====================================================
                HEADER
            ===================================================== */}

            <section>
                <p className="text-sm font-medium text-blue-400">
                    Placement Dashboard
                </p>

                <h1 className="mt-1 text-3xl font-bold tracking-tight text-white">
                    Welcome back
                    {student?.name
                        ? `, ${student.name.split(' ')[0]}`
                        : ''}{' '}
                    👋
                </h1>

                <p className="mt-2 text-slate-400">
                    Here's an overview of your placement readiness.
                </p>
            </section>

            {/* =====================================================
                STATISTICS
            ===================================================== */}

            <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">

                {/* Skills */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-5">
                    <div className="flex items-center justify-between">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400">
                            <CheckCircle2 size={22} />
                        </div>

                        <span className="text-xs font-medium text-slate-500">
                            PROFILE
                        </span>
                    </div>

                    <p className="mt-5 text-3xl font-bold text-white">
                        {skillCount}
                    </p>

                    <p className="mt-1 text-sm text-slate-400">
                        Skills in your profile
                    </p>
                </div>

                {/* Job Pool */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-5">
                    <div className="flex items-center justify-between">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-cyan-500/10 text-cyan-400">
                            <BriefcaseBusiness size={22} />
                        </div>

                        <span className="text-xs font-medium text-slate-500">
                            JOB POOL
                        </span>
                    </div>

                    <p className="mt-5 text-3xl font-bold text-white">
                        {jobPoolCount}
                    </p>

                    <p className="mt-1 text-sm text-slate-400">
                        Saved opportunities
                    </p>
                </div>

                {/* Average Match */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-5">
                    <div className="flex items-center justify-between">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-purple-500/10 text-purple-400">
                            <Target size={22} />
                        </div>

                        <span className="text-xs font-medium text-slate-500">
                            MATCHING
                        </span>
                    </div>

                    <p className="mt-5 text-3xl font-bold text-white">
                        {formatMatch(averageMatch)}
                    </p>

                    <p className="mt-1 text-sm text-slate-400">
                        Average job match
                    </p>
                </div>

                {/* Applications */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-5">
                    <div className="flex items-center justify-between">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-green-500/10 text-green-400">
                            <ClipboardList size={22} />
                        </div>

                        <span className="text-xs font-medium text-slate-500">
                            TRACKER
                        </span>
                    </div>

                    <p className="mt-5 text-3xl font-bold text-white">
                        {applicationCount}
                    </p>

                    <p className="mt-1 text-sm text-slate-400">
                        Applications tracked
                    </p>
                </div>

                {/* Top Match */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-5">
                    <div className="flex items-center justify-between">
                        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-orange-500/10 text-orange-400">
                            <BriefcaseBusiness size={22} />
                        </div>

                        <span className="text-xs font-medium text-slate-500">
                            TOP MATCH
                        </span>
                    </div>

                    <p className="mt-5 text-3xl font-bold text-white">
                        {formatMatch(highestMatch)}
                    </p>

                    <p className="mt-1 truncate text-sm text-slate-400">
                        {topRecommendation?.jobTitle ||
                            'No recommendations yet'}
                    </p>
                </div>
            </section>

            {/* =====================================================
                JOB POOL MATCHES
            ===================================================== */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                    <div>
                        <p className="text-sm font-medium text-cyan-400">
                            Live Job Pool
                        </p>

                        <h2 className="mt-1 text-lg font-semibold text-white">
                            Your saved opportunities
                        </h2>

                        <p className="mt-1 text-sm text-slate-400">
                            Match scores below are calculated from the jobs currently saved in your Job Pool.
                        </p>
                    </div>

                    <Link
                        to="/job-descriptions"
                        className="inline-flex w-fit items-center gap-2 rounded-lg border border-slate-700 px-4 py-2 text-sm font-medium text-slate-300 transition hover:bg-slate-800 hover:text-white"
                    >
                        Manage Job Pool
                        <ArrowUpRight size={16} />
                    </Link>
                </div>

                {jobPool.length === 0 ? (
                    <div className="mt-5 rounded-xl border border-dashed border-slate-800 p-7 text-center">

                        <BriefcaseBusiness
                            className="mx-auto text-slate-600"
                            size={30}
                        />

                        <p className="mt-3 text-sm text-slate-400">
                            No saved jobs yet. Add opportunities to start comparing your profile against the Job Pool.
                        </p>

                        <Link
                            to="/job-descriptions"
                            className="mt-4 inline-flex rounded-lg bg-blue-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-400"
                        >
                            Add Your First Job
                        </Link>
                    </div>
                ) : poolMatches.length === 0 ? (
                    <div className="mt-5 rounded-xl border border-slate-800 bg-slate-900/60 p-6">
                        <p className="text-sm text-slate-400">
                            {jobPool.length}{' '}
                            job
                            {jobPool.length === 1
                                ? ''
                                : 's'}{' '}
                            saved in the pool. Match analysis is not available for the current jobs yet.
                        </p>
                    </div>
                ) : (
                    <div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">

                        {poolMatches
                            .slice(0, 3)
                            .map((match) => (
                                <Link
                                    key={
                                        match.jobDescriptionId
                                    }
                                    to={`/match?job=${match.jobDescriptionId}`}
                                    className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 transition hover:border-slate-700 hover:bg-slate-900"
                                >
                                    <div className="flex items-start justify-between gap-3">

                                        <div className="min-w-0">
                                            <h3 className="truncate font-semibold text-white">
                                                {match.jobTitle ||
                                                    'Untitled Role'}
                                            </h3>

                                            <p className="mt-1 truncate text-sm text-slate-400">
                                                {match.companyName ||
                                                    'Company not specified'}
                                            </p>
                                        </div>

                                        <span className="shrink-0 rounded-lg bg-blue-500/10 px-3 py-2 text-lg font-bold text-blue-400">
                                            {formatMatch(
                                                match.matchPercentage
                                            )}
                                        </span>
                                    </div>

                                    <div className="mt-4 flex flex-wrap gap-2 text-xs">

                                        <span className="rounded-full border border-green-500/20 bg-green-500/10 px-2.5 py-1 text-green-400">
                                            {match.matchedSkills?.length ||
                                                0}{' '}
                                            matched
                                        </span>

                                        <span className="rounded-full border border-orange-500/20 bg-orange-500/10 px-2.5 py-1 text-orange-400">
                                            {match.missingSkills?.length ||
                                                0}{' '}
                                            missing
                                        </span>

                                        <span className="rounded-full border border-slate-700 bg-slate-800/60 px-2.5 py-1 text-slate-400">
                                            {match.relevantProjects?.length ||
                                                0}{' '}
                                            projects
                                        </span>

                                    </div>
                                </Link>
                            ))}
                    </div>
                )}
            </section>

            {/* =====================================================
                MAIN CONTENT
            ===================================================== */}

            <section className="grid gap-6 xl:grid-cols-3">

                {/* Top Job Match */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 xl:col-span-2">

                    <div className="flex items-center justify-between">

                        <div>
                            <h2 className="text-lg font-semibold text-white">
                                Top Job Match
                            </h2>

                            <p className="mt-1 text-sm text-slate-400">
                                Based on your current skills and profile.
                            </p>
                        </div>

                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400">
                            <BriefcaseBusiness size={20} />
                        </div>
                    </div>

                    {topRecommendation ? (
                        <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900/70 p-5">

                            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">

                                <div>
                                    <h3 className="text-xl font-semibold text-white">
                                        {topRecommendation.jobTitle ||
                                            'Untitled Role'}
                                    </h3>

                                    <p className="mt-1 text-slate-400">
                                        {topRecommendation.companyName ||
                                            'Company not specified'}
                                    </p>

                                    {topRecommendation.location && (
                                        <p className="mt-1 text-sm text-slate-500">
                                            {
                                                topRecommendation.location
                                            }
                                        </p>
                                    )}
                                </div>

                                <div className="rounded-xl bg-blue-500/10 px-4 py-3 text-center">

                                    <p className="text-2xl font-bold text-blue-400">
                                        {formatMatch(
                                            topRecommendation.matchPercentage
                                        )}
                                    </p>

                                    <p className="text-xs text-slate-400">
                                        Match
                                    </p>
                                </div>
                            </div>

                            {/* Matched / Missing */}

                            <div className="mt-6 grid gap-4 sm:grid-cols-2">

                                <div>
                                    <p className="text-sm font-medium text-slate-300">
                                        Matched Skills
                                    </p>

                                    <div className="mt-3 flex flex-wrap gap-2">

                                        {(
                                            topRecommendation.matchedSkills ||
                                            []
                                        ).map(
                                            (skill) => (
                                                <span
                                                    key={skill}
                                                    className="rounded-full border border-green-500/20 bg-green-500/10 px-3 py-1 text-xs font-medium text-green-400"
                                                >
                                                    ✓ {skill}
                                                </span>
                                            )
                                        )}
                                    </div>
                                </div>

                                <div>
                                    <p className="text-sm font-medium text-slate-300">
                                        Missing Skills
                                    </p>

                                    <div className="mt-3 flex flex-wrap gap-2">

                                        {(
                                            topRecommendation.missingSkills ||
                                            []
                                        ).map(
                                            (skill) => (
                                                <span
                                                    key={skill}
                                                    className="rounded-full border border-orange-500/20 bg-orange-500/10 px-3 py-1 text-xs font-medium text-orange-400"
                                                >
                                                    ! {skill}
                                                </span>
                                            )
                                        )}
                                    </div>
                                </div>
                            </div>

                            {topRecommendation.reason && (
                                <div className="mt-5 border-t border-slate-800 pt-5">
                                    <p className="text-sm leading-6 text-slate-400">
                                        {topRecommendation.reason}
                                    </p>
                                </div>
                            )}

                            {topPoolMatch?.jobDescriptionId && (
                                <div className="mt-5 border-t border-slate-800 pt-5">

                                    <Link
                                        to={`/match?job=${topPoolMatch.jobDescriptionId}`}
                                        className="inline-flex items-center gap-2 text-sm font-semibold text-blue-400 transition hover:text-blue-300"
                                    >
                                        View full match analysis
                                        <ArrowUpRight size={16} />
                                    </Link>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="mt-6 rounded-xl border border-dashed border-slate-800 p-8 text-center">

                            <BriefcaseBusiness
                                className="mx-auto text-slate-600"
                                size={32}
                            />

                            <p className="mt-3 text-slate-400">
                                No job recommendations available yet.
                            </p>
                        </div>
                    )}
                </div>

                {/* Profile Summary */}

                <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                    <div className="flex items-center gap-3">

                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-500/10 text-purple-400">
                            <FileText size={20} />
                        </div>

                        <div>
                            <h2 className="font-semibold text-white">
                                Profile
                            </h2>

                            <p className="text-xs text-slate-500">
                                Student information
                            </p>
                        </div>
                    </div>

                    <div className="mt-6 space-y-4">

                        <div>
                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                Name
                            </p>

                            <p className="mt-1 text-sm font-medium text-slate-200">
                                {student?.name ||
                                    'Not available'}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                Email
                            </p>

                            <p className="mt-1 break-all text-sm font-medium text-slate-200">
                                {student?.email ||
                                    'Not available'}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                College
                            </p>

                            <p className="mt-1 text-sm font-medium text-slate-200">
                                {student?.college ||
                                    'Not available'}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                Branch
                            </p>

                            <p className="mt-1 text-sm font-medium text-slate-200">
                                {student?.branch ||
                                    'Not available'}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                Graduation Year
                            </p>

                            <p className="mt-1 text-sm font-medium text-slate-200">
                                {student?.graduationYear ||
                                    'Not available'}
                            </p>
                        </div>

                    </div>
                </div>
            </section>

            {/* =====================================================
                APPLICATIONS
            ===================================================== */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                <div className="flex items-center justify-between">

                    <div>
                        <h2 className="text-lg font-semibold text-white">
                            Recent Applications
                        </h2>

                        <p className="mt-1 text-sm text-slate-400">
                            Keep track of your placement activity.
                        </p>
                    </div>

                    <ClipboardList
                        className="text-slate-500"
                        size={21}
                    />
                </div>

                {applications.length > 0 ? (
                    <div className="mt-5 overflow-x-auto">

                        <table className="w-full min-w-[650px] text-left">

                            <thead>
                            <tr className="border-b border-slate-800 text-xs uppercase tracking-wide text-slate-500">

                                <th className="px-3 py-3 font-medium">
                                    Company
                                </th>

                                <th className="px-3 py-3 font-medium">
                                    Position
                                </th>

                                <th className="px-3 py-3 font-medium">
                                    Date
                                </th>

                                <th className="px-3 py-3 font-medium">
                                    Status
                                </th>

                            </tr>
                            </thead>

                            <tbody>

                            {applications
                                .slice(0, 5)
                                .map(
                                    (application) => {
                                        const job =
                                            getApplicationJob(
                                                application
                                            )

                                        return (
                                            <tr
                                                key={
                                                    application.id
                                                }
                                                className="border-b border-slate-800/70 last:border-0"
                                            >

                                                <td className="px-3 py-4 text-sm font-medium text-white">
                                                    {job?.companyName ||
                                                        'Unknown'}
                                                </td>

                                                <td className="px-3 py-4 text-sm text-slate-300">
                                                    {job?.jobTitle ||
                                                        job?.title ||
                                                        'Unknown'}
                                                </td>

                                                <td className="px-3 py-4 text-sm text-slate-400">
                                                    {application.appliedDate ||
                                                        '—'}
                                                </td>

                                                <td className="px-3 py-4">

                                                        <span
                                                            className={`inline-flex rounded-full border px-3 py-1 text-xs font-medium ${getStatusClass(
                                                                application.status
                                                            )}`}
                                                        >
                                                            {application.status ||
                                                                'UNKNOWN'}
                                                        </span>

                                                </td>

                                            </tr>
                                        )
                                    }
                                )}

                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="mt-5 rounded-xl border border-dashed border-slate-800 p-8 text-center">

                        <ClipboardList
                            className="mx-auto text-slate-600"
                            size={32}
                        />

                        <p className="mt-3 text-sm text-slate-400">
                            No applications have been tracked yet.
                        </p>
                    </div>
                )}
            </section>

            {/* =====================================================
                RECOMMENDED ACTIONS
            ===================================================== */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                <div>
                    <h2 className="text-lg font-semibold text-white">
                        Recommended Next Steps
                    </h2>

                    <p className="mt-1 text-sm text-slate-400">
                        Actions that can improve your placement readiness.
                    </p>
                </div>

                <div className="mt-5 grid gap-4 md:grid-cols-3">

                    {/* Skill Gaps */}

                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">

                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-500/10 text-orange-400">
                            <AlertTriangle size={20} />
                        </div>

                        <h3 className="mt-4 font-semibold text-white">
                            Close Skill Gaps
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-400">
                            Focus on the missing skills identified by the job matching engine.
                        </p>

                        <Link
                            to="/skill-gaps"
                            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-orange-400"
                        >
                            View skill gaps
                            <ArrowUpRight size={16} />
                        </Link>
                    </div>

                    {/* Recommendations */}

                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">

                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-blue-400">
                            <BriefcaseBusiness size={20} />
                        </div>

                        <h3 className="mt-4 font-semibold text-white">
                            Explore Better Matches
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-400">
                            Review recommended jobs ranked according to your current profile.
                        </p>

                        <Link
                            to="/recommendations"
                            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-blue-400"
                        >
                            Explore jobs
                            <ArrowUpRight size={16} />
                        </Link>
                    </div>

                    {/* Profile */}

                    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5">

                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-500/10 text-purple-400">
                            <Target size={20} />
                        </div>

                        <h3 className="mt-4 font-semibold text-white">
                            Improve Your Profile
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-400">
                            Strengthen your resume and add evidence for your target roles.
                        </p>

                        <Link
                            to="/profile"
                            className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-purple-400"
                        >
                            Improve profile
                            <ArrowUpRight size={16} />
                        </Link>
                    </div>
                </div>
            </section>
        </div>
    )
}

export default Dashboard