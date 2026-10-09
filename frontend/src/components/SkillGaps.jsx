import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080'

function SkillGaps() {

    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [searchParams, setSearchParams] =
        useSearchParams()

    const jobFromUrl =
        searchParams.get('job')

    const [jobs, setJobs] =
        useState([])

    const [selectedJobId, setSelectedJobId] =
        useState(jobFromUrl || '')

    const [data, setData] =
        useState(null)

    const [loadingJobs, setLoadingJobs] =
        useState(true)

    const [loading, setLoading] =
        useState(false)

    const [error, setError] =
        useState('')


    /*
     * =========================================================
     * LOAD JOBS AFTER AUTHENTICATION
     * =========================================================
     */

    useEffect(() => {

        if (
            authLoading ||
            !studentId ||
            !token
        ) {
            return
        }

        loadJobs()

    }, [
        authLoading,
        studentId,
        token
    ])


    /*
     * =========================================================
     * SYNC JOB FROM URL
     * =========================================================
     */

    useEffect(() => {

        if (jobFromUrl) {

            setSelectedJobId(
                jobFromUrl
            )
        }

    }, [jobFromUrl])


    /*
     * =========================================================
     * LOAD SKILL GAPS
     * =========================================================
     */

    useEffect(() => {

        if (
            authLoading ||
            !studentId ||
            !token
        ) {
            return
        }

        if (selectedJobId) {

            loadSkillGaps(
                selectedJobId
            )
        }

    }, [
        authLoading,
        studentId,
        token,
        selectedJobId
    ])


    /*
     * =========================================================
     * LOAD SAVED JOB DESCRIPTIONS
     * =========================================================
     */

    async function loadJobs() {

        try {

            setLoadingJobs(true)
            setError('')

            const response =
                await fetch(
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
                    'Failed to load saved job descriptions'
                )
            }

            const result =
                await response.json()

            if (!Array.isArray(result)) {

                throw new Error(
                    'Unexpected job description response'
                )
            }

            setJobs(result)

            if (result.length > 0) {

                const requestedJobExists =
                    jobFromUrl &&
                    result.some(
                        (job) =>
                            String(job.id) ===
                            String(jobFromUrl)
                    )

                if (requestedJobExists) {

                    setSelectedJobId(
                        String(jobFromUrl)
                    )

                } else {

                    setSelectedJobId(
                        String(result[0].id)
                    )
                }

            } else {

                setSelectedJobId('')
                setData(null)
            }

        } catch (err) {

            console.error(err)

            setError(
                err.message ||
                'Unable to load saved job descriptions.'
            )

        } finally {

            setLoadingJobs(false)
        }
    }


    /*
     * =========================================================
     * LOAD SKILL GAP ANALYSIS
     * =========================================================
     */

    async function loadSkillGaps(
        jobId
    ) {

        if (!jobId) {
            return
        }

        try {

            setLoading(true)
            setError('')
            setData(null)

            const response =
                await fetch(
                    `${API_BASE_URL}/api/skill-gaps/student/${studentId}/job/${jobId}`,
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
                    'Failed to load skill gap analysis'
                )
            }

            const result =
                await response.json()

            setData(result)

        } catch (err) {

            console.error(err)

            setError(
                err.message ||
                'Failed to load skill gap analysis'
            )

        } finally {

            setLoading(false)
        }
    }


    /*
     * =========================================================
     * JOB CHANGE
     * =========================================================
     */

    function handleJobChange(
        event
    ) {

        const jobId =
            event.target.value

        setSelectedJobId(
            jobId
        )

        setData(null)
        setError('')

        if (jobId) {

            setSearchParams({
                job: jobId
            })

        } else {

            setSearchParams({})
        }
    }


    /*
     * =========================================================
     * SESSION LOADING
     * =========================================================
     */

    if (
        authLoading ||
        !studentId ||
        !token
    ) {

        return (
            <div className="flex min-h-[70vh] items-center justify-center">

                <div className="text-center">

                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-blue-500" />

                    <p className="mt-4 text-slate-400">
                        Loading your session...
                    </p>

                </div>

            </div>
        )
    }


    /*
     * =========================================================
     * JOB LOADING
     * =========================================================
     */

    if (
        loadingJobs &&
        jobs.length === 0
    ) {

        return (
            <div className="flex min-h-[70vh] items-center justify-center">

                <div className="text-center">

                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-blue-500" />

                    <p className="mt-4 text-slate-400">
                        Loading available jobs...
                    </p>

                </div>

            </div>
        )
    }


    /*
     * =========================================================
     * ERROR
     * =========================================================
     */

    if (
        error &&
        !data
    ) {

        return (
            <div className="p-8">

                <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6">

                    <h2 className="text-xl font-semibold text-red-400">
                        Unable to load skill gaps
                    </h2>

                    <p className="mt-2 text-slate-400">
                        {error}
                    </p>

                    <button
                        type="button"
                        onClick={() => {

                            if (selectedJobId) {

                                loadSkillGaps(
                                    selectedJobId
                                )

                            } else {

                                loadJobs()
                            }

                        }}
                        className="mt-5 rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white transition hover:bg-blue-500"
                    >
                        Try Again
                    </button>

                </div>

            </div>
        )
    }


    const coverage =
        Number(
            data?.skillCoverage || 0
        )


    const matchedSkills =
        Array.isArray(
            data?.matchedSkills
        )
            ? data.matchedSkills
            : []


    const missingSkills =
        Array.isArray(
            data?.missingSkills
        )
            ? data.missingSkills
            : []


    const requiredSkills =
        Array.isArray(
            data?.requiredSkills
        )
            ? data.requiredSkills
            : []


    const skillGaps =
        Array.isArray(
            data?.skillGaps
        )
            ? data.skillGaps
            : []


    return (
        <div className="mx-auto max-w-7xl px-8 py-8">


            {/* =================================================
                HEADER
            ================================================= */}

            <div className="mb-8">

                <p className="text-sm font-semibold uppercase tracking-[0.25em] text-blue-400">
                    SKILL ANALYSIS
                </p>

                <h1 className="mt-2 text-4xl font-bold text-white">
                    Skill Gaps
                </h1>

                <p className="mt-3 text-lg text-slate-400">

                    See exactly which skills you need to improve for{' '}

                    <span className="font-medium text-white">
                        {data?.jobTitle ||
                            'the selected job'}
                    </span>

                </p>

            </div>


            {/* =================================================
                JOB SELECTOR
            ================================================= */}

            <div className="mb-8 rounded-2xl border border-slate-800 bg-slate-950 p-6">

                <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">

                    <div className="flex-1">

                        <label
                            htmlFor="skill-gap-job"
                            className="mb-2 block text-sm font-semibold text-slate-300"
                        >
                            Analyze Skill Gaps For
                        </label>

                        <select
                            id="skill-gap-job"
                            value={selectedJobId}
                            onChange={
                                handleJobChange
                            }
                            disabled={
                                loadingJobs ||
                                jobs.length === 0 ||
                                loading
                            }
                            className="w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20"
                        >

                            {jobs.length === 0 ? (

                                <option value="">
                                    No saved jobs available
                                </option>

                            ) : (

                                jobs.map(
                                    (job) => (

                                        <option
                                            key={job.id}
                                            value={String(
                                                job.id
                                            )}
                                        >

                                            {job.title ||
                                                job.jobTitle ||
                                                'Untitled Job'}

                                            {job.companyName
                                                ? ` — ${job.companyName}`
                                                : ''}

                                        </option>
                                    )
                                )

                            )}

                        </select>

                    </div>


                    <div className="text-sm text-slate-500">

                        {loading
                            ? 'Analyzing selected job...'
                            : `${jobs.length} saved job${jobs.length === 1 ? '' : 's'} available`}

                    </div>

                </div>

            </div>


            {/* =================================================
                ANALYSIS LOADING
            ================================================= */}

            {loading && (

                <div className="flex min-h-[35vh] items-center justify-center">

                    <div className="text-center">

                        <div className="mx-auto h-10 w-10 animate-spin rounded-full border-4 border-slate-700 border-t-blue-500" />

                        <p className="mt-4 text-slate-400">
                            Analyzing your skill gaps...
                        </p>

                    </div>

                </div>
            )}


            {/* =================================================
                ANALYSIS RESULT
            ================================================= */}

            {!loading &&
                data && (

                    <>

                        {/* SUMMARY */}

                        <div className="grid gap-6 lg:grid-cols-3">


                            {/* COVERAGE */}

                            <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-medium uppercase tracking-wider text-slate-500">
                                    Skill Coverage
                                </p>

                                <div className="mt-4 flex items-end gap-2">

                                    <span className="text-5xl font-bold text-white">
                                        {coverage.toFixed(
                                            0
                                        )}
                                    </span>

                                    <span className="mb-1 text-slate-500">
                                        %
                                    </span>

                                </div>

                                <div className="mt-5 h-3 overflow-hidden rounded-full bg-slate-800">

                                    <div
                                        className="h-full rounded-full bg-blue-500 transition-all"
                                        style={{
                                            width: `${Math.min(
                                                Math.max(
                                                    coverage,
                                                    0
                                                ),
                                                100
                                            )}%`,
                                        }}
                                    />

                                </div>

                            </div>


                            {/* MATCHED */}

                            <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-medium uppercase tracking-wider text-slate-500">
                                    Skills You Have
                                </p>

                                <div className="mt-4 text-4xl font-bold text-emerald-400">
                                    {
                                        matchedSkills.length
                                    }
                                </div>

                                <p className="mt-2 text-sm text-slate-500">
                                    Required skills already matched
                                </p>

                            </div>


                            {/* MISSING */}

                            <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-medium uppercase tracking-wider text-slate-500">
                                    Skills To Develop
                                </p>

                                <div className="mt-4 text-4xl font-bold text-orange-400">
                                    {
                                        missingSkills.length
                                    }
                                </div>

                                <p className="mt-2 text-sm text-slate-500">
                                    Skills requiring preparation
                                </p>

                            </div>

                        </div>


                        {/* =================================================
                            MATCHED SKILLS
                        ================================================= */}

                        <div className="mt-8 grid gap-6 lg:grid-cols-2">


                            <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-semibold uppercase tracking-wider text-emerald-400">
                                    YOUR SKILLS
                                </p>

                                <h2 className="mt-2 text-2xl font-bold text-white">
                                    Matched Skills
                                </h2>

                                <div className="mt-5 flex flex-wrap gap-2">

                                    {matchedSkills.length > 0
                                        ? matchedSkills.map(
                                            (skill) => (

                                                <span
                                                    key={skill}
                                                    className="rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-300"
                                                >
                                                    ✓ {skill}
                                                </span>

                                            )
                                        )
                                        : (
                                            <p className="text-sm text-slate-500">
                                                No directly matched skills.
                                            </p>
                                        )}

                                </div>

                            </div>


                            {/* MISSING SKILLS */}

                            <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-semibold uppercase tracking-wider text-orange-400">
                                    DEVELOPMENT
                                </p>

                                <h2 className="mt-2 text-2xl font-bold text-white">
                                    Skills To Develop
                                </h2>

                                <div className="mt-5 flex flex-wrap gap-2">

                                    {missingSkills.length > 0
                                        ? missingSkills.map(
                                            (skill) => (

                                                <span
                                                    key={skill}
                                                    className="rounded-full border border-orange-500/20 bg-orange-500/10 px-3 py-2 text-sm text-orange-300"
                                                >
                                                    + {skill}
                                                </span>

                                            )
                                        )
                                        : (
                                            <p className="text-sm text-emerald-400">
                                                No major skill gaps identified.
                                            </p>
                                        )}

                                </div>

                            </div>

                        </div>


                        {/* =================================================
                            REQUIRED SKILLS
                        ================================================= */}

                        {requiredSkills.length > 0 && (

                            <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-950 p-6">

                                <p className="text-sm font-semibold uppercase tracking-wider text-blue-400">
                                    JOB REQUIREMENTS
                                </p>

                                <h2 className="mt-2 text-2xl font-bold text-white">
                                    Required Skills
                                </h2>

                                <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">

                                    {requiredSkills.map(
                                        (skill) => {

                                            const matched =
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

                                                <div
                                                    key={skill}
                                                    className={`rounded-xl border p-4 ${
                                                        matched
                                                            ? 'border-emerald-500/20 bg-emerald-500/5'
                                                            : 'border-orange-500/20 bg-orange-500/5'
                                                    }`}
                                                >

                                                    <div className="flex items-center justify-between gap-3">

                                                        <span className="font-medium text-white">
                                                            {skill}
                                                        </span>

                                                        <span
                                                            className={
                                                                matched
                                                                    ? 'text-emerald-400'
                                                                    : 'text-orange-400'
                                                            }
                                                        >
                                                            {matched
                                                                ? '✓'
                                                                : '!'}
                                                        </span>

                                                    </div>

                                                    <p className="mt-1 text-xs text-slate-500">

                                                        {matched
                                                            ? 'Already demonstrated'
                                                            : 'Needs improvement'}

                                                    </p>

                                                </div>
                                            )
                                        }
                                    )}

                                </div>

                            </div>
                        )}


                        {/* =================================================
                            DEVELOPMENT PLAN
                        ================================================= */}

                        <div className="mt-10">

                            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-purple-400">
                                DEVELOPMENT PLAN
                            </p>

                            <h2 className="mt-2 text-2xl font-bold text-white">
                                What You Need to Learn
                            </h2>

                            <p className="mt-2 text-slate-400">
                                Prioritized gaps based on the selected job.
                            </p>

                        </div>


                        <div className="mt-6 space-y-5">

                            {skillGaps.length > 0
                                ? skillGaps.map(
                                    (
                                        gap,
                                        index
                                    ) => (

                                        <div
                                            key={
                                                gap.skill ||
                                                index
                                            }
                                            className="rounded-2xl border border-slate-800 bg-slate-950 p-6"
                                        >

                                            <div className="flex flex-col gap-5">

                                                <div>

                                                    <div className="flex flex-wrap items-center gap-3">

                                                        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-orange-500/10 text-orange-400">
                                                            {index +
                                                                1}
                                                        </span>

                                                        <h3 className="text-xl font-bold text-white">
                                                            {
                                                                gap.skill
                                                            }
                                                        </h3>

                                                        {gap.priority && (

                                                            <span className="rounded-full border border-red-500/20 bg-red-500/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-red-400">
                                                                {
                                                                    gap.priority
                                                                }
                                                            </span>
                                                        )}

                                                        {gap.currentLevel && (

                                                            <span className="rounded-full border border-slate-700 bg-slate-900 px-3 py-1 text-xs text-slate-400">
                                                                Current:{' '}
                                                                {
                                                                    gap.currentLevel
                                                                }
                                                            </span>
                                                        )}

                                                    </div>


                                                    {gap.learningRecommendation && (

                                                        <div className="mt-5 rounded-xl border border-blue-500/10 bg-blue-500/5 p-5">

                                                            <p className="text-xs font-semibold uppercase tracking-wider text-blue-400">
                                                                LEARNING RECOMMENDATION
                                                            </p>

                                                            <p className="mt-2 leading-7 text-slate-300">
                                                                {
                                                                    gap.learningRecommendation
                                                                }
                                                            </p>

                                                        </div>
                                                    )}


                                                    {gap.suggestedProject && (

                                                        <div className="mt-4 rounded-xl border border-purple-500/10 bg-purple-500/5 p-5">

                                                            <p className="text-xs font-semibold uppercase tracking-wider text-purple-400">
                                                                SUGGESTED PROJECT
                                                            </p>

                                                            <p className="mt-2 leading-7 text-slate-300">
                                                                {
                                                                    gap.suggestedProject
                                                                }
                                                            </p>

                                                        </div>
                                                    )}

                                                </div>

                                            </div>

                                        </div>

                                    )
                                )
                                : (

                                    <div className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-7">

                                        <p className="text-lg font-semibold text-emerald-300">
                                            No skill gaps identified
                                        </p>

                                        <p className="mt-2 text-slate-400">
                                            Your current profile covers the required skills identified for this job.
                                        </p>

                                    </div>
                                )}

                        </div>


                        {/* =================================================
                            OVERALL ADVICE
                        ================================================= */}

                        {data.overallAdvice && (

                            <div className="mt-8 rounded-2xl border border-blue-500/20 bg-blue-500/5 p-7">

                                <p className="text-sm font-semibold uppercase tracking-[0.2em] text-blue-400">
                                    INTELLIGENCE INSIGHT
                                </p>

                                <h2 className="mt-2 text-2xl font-bold text-white">
                                    Recommended Focus
                                </h2>

                                <p className="mt-4 max-w-4xl leading-7 text-slate-300">
                                    {
                                        data.overallAdvice
                                    }
                                </p>

                            </div>
                        )}


                        {/* =================================================
                            ROADMAP BUTTON
                        ================================================= */}

                        <div className="mt-8 flex flex-col gap-4 rounded-2xl border border-slate-800 bg-slate-950 p-7 sm:flex-row sm:items-center sm:justify-between">

                            <div>

                                <p className="text-lg font-semibold text-white">
                                    Ready to close these gaps?
                                </p>

                                <p className="mt-1 text-slate-400">
                                    Turn your missing skills into a structured learning plan.
                                </p>

                            </div>

                            <Link
                                to={
                                    selectedJobId
                                        ? `/roadmap?job=${selectedJobId}`
                                        : '/roadmap'
                                }
                                className="inline-flex items-center justify-center rounded-xl bg-blue-600 px-5 py-3 font-semibold text-white transition hover:bg-blue-500"
                            >
                                View Learning Roadmap →
                            </Link>

                        </div>

                    </>
                )}

        </div>
    )
}

export default SkillGaps