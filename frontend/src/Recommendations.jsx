import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080/api'

function getMatchLabel(matchPercentage) {
    if (matchPercentage >= 80) {
        return 'Strong Match'
    }

    if (matchPercentage >= 60) {
        return 'Good Match'
    }

    if (matchPercentage >= 40) {
        return 'Moderate Match'
    }

    return 'Low Match'
}

function getMatchClass(matchPercentage) {
    if (matchPercentage >= 80) {
        return 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
    }

    if (matchPercentage >= 60) {
        return 'bg-cyan-500/10 text-cyan-300 border-cyan-500/20'
    }

    if (matchPercentage >= 40) {
        return 'bg-amber-500/10 text-amber-300 border-amber-500/20'
    }

    return 'bg-red-500/10 text-red-300 border-red-500/20'
}

function Recommendations() {
    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [recommendations, setRecommendations] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [sortBy, setSortBy] = useState('match')
    const [filter, setFilter] = useState('all')

    useEffect(() => {
        if (authLoading) {
            return
        }

        if (!studentId || !token) {
            setLoading(false)
            setError('Your session has expired. Please login again.')
            return
        }

        loadRecommendations()
    }, [authLoading, studentId, token])

    async function loadRecommendations() {
        try {
            setLoading(true)
            setError('')

            const response = await fetch(
                `${API_BASE_URL}/job-recommendations/student/${studentId}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            )

            if (!response.ok) {
                const responseText = await response.text()

                let message =
                    `Request failed with status ${response.status}`

                if (responseText) {
                    try {
                        const errorData =
                            JSON.parse(responseText)

                        if (errorData?.message) {
                            message =
                                errorData.message
                        }
                    } catch {
                        message = responseText
                    }
                }

                throw new Error(message)
            }

            const data = await response.json()

            if (!Array.isArray(data)) {
                throw new Error(
                    'Unexpected recommendation response'
                )
            }

            setRecommendations(data)
        } catch (err) {
            console.error(
                'Recommendations loading error:',
                err
            )

            setError(
                err.message ||
                'Unable to load job recommendations. Make sure the backend is running.'
            )
        } finally {
            setLoading(false)
        }
    }

    const filteredRecommendations = useMemo(() => {
        let result = [...recommendations]

        if (filter === 'strong') {
            result = result.filter(
                (job) =>
                    Number(
                        job.matchPercentage ?? 0
                    ) >= 80
            )
        }

        if (filter === 'good') {
            result = result.filter(
                (job) => {
                    const score =
                        Number(
                            job.matchPercentage ?? 0
                        )

                    return (
                        score >= 60 &&
                        score < 80
                    )
                }
            )
        }

        if (filter === 'moderate') {
            result = result.filter(
                (job) => {
                    const score =
                        Number(
                            job.matchPercentage ?? 0
                        )

                    return (
                        score >= 40 &&
                        score < 60
                    )
                }
            )
        }

        if (sortBy === 'match') {
            result.sort(
                (a, b) =>
                    Number(
                        b.matchPercentage ?? 0
                    ) -
                    Number(
                        a.matchPercentage ?? 0
                    )
            )
        }

        if (sortBy === 'salary') {
            result.sort(
                (a, b) =>
                    Number(
                        b.salary ?? 0
                    ) -
                    Number(
                        a.salary ?? 0
                    )
            )
        }

        if (sortBy === 'rank') {
            result.sort(
                (a, b) =>
                    Number(
                        a.rank ?? 999
                    ) -
                    Number(
                        b.rank ?? 999
                    )
            )
        }

        return result
    }, [
        recommendations,
        sortBy,
        filter,
    ])

    const topMatch =
        recommendations.length > 0
            ? [...recommendations].sort(
                (a, b) =>
                    Number(
                        b.matchPercentage ?? 0
                    ) -
                    Number(
                        a.matchPercentage ?? 0
                    )
            )[0]
            : null

    if (authLoading) {
        return (
            <div className="flex min-h-[60vh] items-center justify-center">
                <div className="text-center">
                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-slate-700 border-t-cyan-400" />

                    <p className="mt-4 text-sm text-slate-400">
                        Loading your session...
                    </p>
                </div>
            </div>
        )
    }

    if (!studentId || !token) {
        return (
            <div className="rounded-2xl border border-red-500/20 bg-red-500/5 p-8 text-center">
                <h2 className="text-xl font-semibold text-red-300">
                    Session required
                </h2>

                <p className="mt-2 text-sm text-slate-400">
                    Please login again to view your job recommendations.
                </p>

                <Link
                    to="/login"
                    className="mt-5 inline-flex rounded-xl bg-cyan-500 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                >
                    Go to Login
                </Link>
            </div>
        )
    }

    return (
        <div className="space-y-8">

            {/* Header */}
            <section>
                <p className="text-sm font-medium uppercase tracking-[0.2em] text-cyan-400">
                    Placement Intelligence
                </p>

                <h1 className="mt-2 text-4xl font-bold tracking-tight text-white">
                    Job Recommendations
                </h1>

                <p className="mt-3 max-w-3xl text-slate-400">
                    Discover the jobs that best match your current skills,
                    experience and profile. Recommendations are ranked using
                    your placement intelligence profile.
                </p>
            </section>

            {/* Loading */}
            {loading && (
                <div className="rounded-2xl border border-white/10 bg-slate-900/60 p-10 text-center">
                    <div className="mx-auto h-8 w-8 animate-spin rounded-full border-2 border-slate-600 border-t-cyan-400" />

                    <p className="mt-4 text-slate-300">
                        Analyzing your best job matches...
                    </p>
                </div>
            )}

            {/* Error */}
            {!loading && error && (
                <div className="rounded-2xl border border-red-500/20 bg-red-500/5 p-6">
                    <h2 className="text-lg font-semibold text-red-300">
                        Could not load recommendations
                    </h2>

                    <p className="mt-2 text-sm text-slate-400">
                        {error}
                    </p>

                    <button
                        onClick={loadRecommendations}
                        className="mt-5 rounded-xl bg-cyan-500 px-5 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                    >
                        Try Again
                    </button>
                </div>
            )}

            {/* Content */}
            {!loading && !error && (
                <>
                    {/* Summary cards */}
                    <section className="grid gap-4 md:grid-cols-3">

                        <div className="rounded-2xl border border-white/10 bg-slate-900/60 p-6">
                            <p className="text-sm text-slate-400">
                                Jobs Analyzed
                            </p>

                            <p className="mt-2 text-3xl font-bold text-white">
                                {recommendations.length}
                            </p>

                            <p className="mt-1 text-sm text-slate-500">
                                Based on your current profile
                            </p>
                        </div>

                        <div className="rounded-2xl border border-white/10 bg-slate-900/60 p-6">
                            <p className="text-sm text-slate-400">
                                Top Match
                            </p>

                            <p className="mt-2 text-3xl font-bold text-cyan-300">
                                {topMatch
                                    ? `${Number(
                                        topMatch.matchPercentage ?? 0
                                    ).toFixed(1)}%`
                                    : '—'}
                            </p>

                            <p className="mt-1 truncate text-sm text-slate-500">
                                {topMatch
                                    ? (
                                        topMatch.jobTitle ||
                                        topMatch.title ||
                                        'Job opportunity'
                                    )
                                    : 'No recommendation yet'}
                            </p>
                        </div>

                        <div className="rounded-2xl border border-white/10 bg-slate-900/60 p-6">
                            <p className="text-sm text-slate-400">
                                Best Company
                            </p>

                            <p className="mt-2 truncate text-xl font-bold text-white">
                                {topMatch
                                    ? (
                                        topMatch.companyName ||
                                        topMatch.company ||
                                        '—'
                                    )
                                    : '—'}
                            </p>

                            <p className="mt-1 text-sm text-slate-500">
                                Highest-ranked opportunity
                            </p>
                        </div>

                    </section>

                    {/* Controls */}
                    <section className="flex flex-col gap-4 rounded-2xl border border-white/10 bg-slate-900/60 p-5 md:flex-row md:items-center md:justify-between">

                        <div>
                            <h2 className="text-lg font-semibold text-white">
                                Recommended Jobs
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                Ranked from strongest to weakest match.
                            </p>
                        </div>

                        <div className="flex flex-col gap-3 sm:flex-row">

                            <select
                                value={filter}
                                onChange={(e) =>
                                    setFilter(
                                        e.target.value
                                    )
                                }
                                className="rounded-xl border border-white/10 bg-slate-950 px-4 py-2.5 text-sm text-slate-200 outline-none focus:border-cyan-400/50"
                            >
                                <option value="all">
                                    All matches
                                </option>

                                <option value="strong">
                                    Strong matches
                                </option>

                                <option value="good">
                                    Good matches
                                </option>

                                <option value="moderate">
                                    Moderate matches
                                </option>
                            </select>

                            <select
                                value={sortBy}
                                onChange={(e) =>
                                    setSortBy(
                                        e.target.value
                                    )
                                }
                                className="rounded-xl border border-white/10 bg-slate-950 px-4 py-2.5 text-sm text-slate-200 outline-none focus:border-cyan-400/50"
                            >
                                <option value="match">
                                    Sort by match
                                </option>

                                <option value="rank">
                                    Sort by rank
                                </option>

                                <option value="salary">
                                    Sort by salary
                                </option>
                            </select>

                        </div>
                    </section>

                    {/* Empty state */}
                    {filteredRecommendations.length === 0 && (
                        <div className="rounded-2xl border border-white/10 bg-slate-900/60 p-10 text-center">

                            <h2 className="text-xl font-semibold text-white">
                                No jobs match this filter
                            </h2>

                            <p className="mt-2 text-slate-400">
                                Try another filter to see more opportunities.
                            </p>

                        </div>
                    )}

                    {/* Recommendation cards */}
                    <section className="space-y-5">

                        {filteredRecommendations.map(
                            (job, index) => {

                                const matchPercentage =
                                    Number(
                                        job.matchPercentage ?? 0
                                    )

                                const matchedSkills =
                                    Array.isArray(
                                        job.matchedSkills
                                    )
                                        ? job.matchedSkills
                                        : []

                                const missingSkills =
                                    Array.isArray(
                                        job.missingSkills
                                    )
                                        ? job.missingSkills
                                        : []

                                const requiredSkills =
                                    Array.isArray(
                                        job.requiredSkills
                                    )
                                        ? job.requiredSkills
                                        : []

                                const jobId =
                                    job.jobId ??
                                    job.jobDescriptionId ??
                                    job.id

                                const jobTitle =
                                    job.jobTitle ||
                                    job.title ||
                                    'Job Opportunity'

                                const companyName =
                                    job.companyName ||
                                    job.company ||
                                    'Company not specified'

                                return (
                                    <article
                                        key={
                                            jobId ??
                                            `${jobTitle}-${index}`
                                        }
                                        className="rounded-2xl border border-white/10 bg-slate-900/60 p-6 transition hover:border-cyan-400/20"
                                    >

                                        {/* Top row */}
                                        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">

                                            <div className="flex gap-4">

                                                <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-cyan-500/10 text-lg font-bold text-cyan-300">
                                                    {job.rank ??
                                                        index + 1}
                                                </div>

                                                <div>

                                                    <div className="flex flex-wrap items-center gap-2">

                                                        <h3 className="text-xl font-bold text-white">
                                                            {jobTitle}
                                                        </h3>

                                                        <span
                                                            className={`rounded-full border px-3 py-1 text-xs font-medium ${getMatchClass(
                                                                matchPercentage
                                                            )}`}
                                                        >
                                                            {getMatchLabel(
                                                                matchPercentage
                                                            )}
                                                        </span>

                                                    </div>

                                                    <p className="mt-1 text-slate-300">
                                                        {companyName}
                                                    </p>

                                                    <div className="mt-3 flex flex-wrap gap-3 text-sm text-slate-500">

                                                        {job.location && (
                                                            <span>
                                                                📍{' '}
                                                                {job.location}
                                                            </span>
                                                        )}

                                                        {job.jobType && (
                                                            <span>
                                                                💼{' '}
                                                                {job.jobType}
                                                            </span>
                                                        )}

                                                        {job.salary != null && (
                                                            <span>
                                                                ₹
                                                                {Number(
                                                                    job.salary
                                                                ).toLocaleString(
                                                                    'en-IN'
                                                                )}
                                                            </span>
                                                        )}

                                                    </div>

                                                </div>

                                            </div>

                                            {/* Score */}
                                            <div className="lg:text-right">

                                                <p className="text-sm text-slate-500">
                                                    Match Score
                                                </p>

                                                <p className="mt-1 text-4xl font-bold text-cyan-300">
                                                    {matchPercentage.toFixed(
                                                        1
                                                    )}
                                                    %
                                                </p>

                                            </div>

                                        </div>

                                        {/* Match bar */}
                                        <div className="mt-6">

                                            <div className="h-2 overflow-hidden rounded-full bg-slate-800">

                                                <div
                                                    className="h-full rounded-full bg-cyan-400 transition-all"
                                                    style={{
                                                        width: `${Math.min(
                                                            Math.max(
                                                                matchPercentage,
                                                                0
                                                            ),
                                                            100
                                                        )}%`,
                                                    }}
                                                />

                                            </div>

                                        </div>

                                        {/* Reason */}
                                        {job.reason && (
                                            <div className="mt-6 rounded-xl border border-white/5 bg-slate-950/50 p-4">

                                                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                                                    Why this job?
                                                </p>

                                                <p className="mt-2 text-sm leading-6 text-slate-300">
                                                    {job.reason}
                                                </p>

                                            </div>
                                        )}

                                        {/* Scores */}
                                        <div className="mt-6 grid gap-3 sm:grid-cols-3">

                                            <div className="rounded-xl bg-slate-950/50 p-4">

                                                <p className="text-xs text-slate-500">
                                                    Skill Score
                                                </p>

                                                <p className="mt-1 text-lg font-semibold text-white">
                                                    {Number(
                                                        job.skillScore ?? 0
                                                    ).toFixed(1)}
                                                </p>

                                            </div>

                                            <div className="rounded-xl bg-slate-950/50 p-4">

                                                <p className="text-xs text-slate-500">
                                                    Role Score
                                                </p>

                                                <p className="mt-1 text-lg font-semibold text-white">
                                                    {Number(
                                                        job.roleScore ?? 0
                                                    ).toFixed(1)}
                                                </p>

                                            </div>

                                            <div className="rounded-xl bg-slate-950/50 p-4">

                                                <p className="text-xs text-slate-500">
                                                    Breadth Score
                                                </p>

                                                <p className="mt-1 text-lg font-semibold text-white">
                                                    {Number(
                                                        job.breadthScore ?? 0
                                                    ).toFixed(1)}
                                                </p>

                                            </div>

                                        </div>

                                        {/* Matched skills */}
                                        <div className="mt-6 grid gap-6 lg:grid-cols-2">

                                            <div>

                                                <h4 className="text-sm font-semibold text-white">
                                                    Skills you have
                                                </h4>

                                                <div className="mt-3 flex flex-wrap gap-2">

                                                    {matchedSkills.length > 0
                                                        ? matchedSkills.map(
                                                            (skill) => (
                                                                <span
                                                                    key={skill}
                                                                    className="rounded-lg border border-emerald-500/20 bg-emerald-500/10 px-3 py-1.5 text-sm text-emerald-300"
                                                                >
                                                                    ✓ {skill}
                                                                </span>
                                                            )
                                                        )
                                                        : (
                                                            <span className="text-sm text-slate-500">
                                                                No matched skills
                                                            </span>
                                                        )}

                                                </div>

                                            </div>

                                            <div>

                                                <h4 className="text-sm font-semibold text-white">
                                                    Skills to develop
                                                </h4>

                                                <div className="mt-3 flex flex-wrap gap-2">

                                                    {missingSkills.length > 0
                                                        ? missingSkills.map(
                                                            (skill) => (
                                                                <span
                                                                    key={skill}
                                                                    className="rounded-lg border border-amber-500/20 bg-amber-500/10 px-3 py-1.5 text-sm text-amber-300"
                                                                >
                                                                    ! {skill}
                                                                </span>
                                                            )
                                                        )
                                                        : (
                                                            <span className="text-sm text-emerald-300">
                                                                ✓ No major skill gaps
                                                            </span>
                                                        )}

                                                </div>

                                            </div>

                                        </div>

                                        {/* Requirements */}
                                        {requiredSkills.length > 0 && (
                                            <div className="mt-6">

                                                <h4 className="text-sm font-semibold text-white">
                                                    Job Requirements
                                                </h4>

                                                <div className="mt-3 flex flex-wrap gap-2">

                                                    {requiredSkills.map(
                                                        (skill) => {

                                                            const matched =
                                                                matchedSkills.some(
                                                                    (
                                                                        matchedSkill
                                                                    ) =>
                                                                        String(
                                                                            matchedSkill
                                                                        ).toLowerCase() ===
                                                                        String(
                                                                            skill
                                                                        ).toLowerCase()
                                                                )

                                                            return (
                                                                <span
                                                                    key={skill}
                                                                    className={`rounded-lg border px-3 py-1.5 text-sm ${
                                                                        matched
                                                                            ? 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300'
                                                                            : 'border-red-500/20 bg-red-500/10 text-red-300'
                                                                    }`}
                                                                >
                                                                    {matched
                                                                        ? '✓'
                                                                        : '!'}{' '}
                                                                    {skill}
                                                                </span>
                                                            )
                                                        }
                                                    )}

                                                </div>

                                            </div>
                                        )}

                                        {/* Bottom action */}
                                        <div className="mt-7 flex flex-col gap-3 border-t border-white/5 pt-5 sm:flex-row sm:items-center sm:justify-between">

                                            <p className="text-sm text-slate-500">
                                                Rank #{job.rank ??
                                                index + 1}
                                            </p>

                                            <div className="flex flex-wrap gap-3">

                                                {missingSkills.length > 0 && (
                                                    <Link
                                                        to={
                                                            jobId
                                                                ? `/skill-gaps?job=${jobId}`
                                                                : '/skill-gaps'
                                                        }
                                                        className="rounded-xl border border-white/10 px-4 py-2.5 text-sm font-medium text-slate-200 transition hover:bg-white/5"
                                                    >
                                                        View Skill Gaps
                                                    </Link>
                                                )}

                                                <Link
                                                    to={
                                                        jobId
                                                            ? `/match?job=${jobId}`
                                                            : '/match'
                                                    }
                                                    className="rounded-xl bg-cyan-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                                                >
                                                    Analyze Match →
                                                </Link>

                                            </div>

                                        </div>

                                    </article>
                                )
                            }
                        )}

                    </section>
                </>
            )}

        </div>
    )
}

export default Recommendations