import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
    Search,
    MapPin,
    BriefcaseBusiness,
    Building2,
    IndianRupee,
    ArrowUpRight,
    Loader2,
    AlertTriangle,
    Clock3,
    GraduationCap,
    CalendarDays,
    ExternalLink,
    CheckCircle2,
} from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080/api'

function Jobs() {

    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [jobs, setJobs] = useState([])

    const [searchTerm, setSearchTerm] =
        useState('')

    const [locationFilter, setLocationFilter] =
        useState('ALL')

    const [loading, setLoading] =
        useState(true)

    const [error, setError] =
        useState('')

    /*
     * Stores JobDescription IDs that the
     * authenticated student has already applied to.
     */
    const [appliedJobIds, setAppliedJobIds] =
        useState(new Set())

    /*
     * Stores the JobDescription ID currently
     * being applied to.
     */
    const [applyingJobId, setApplyingJobId] =
        useState(null)


    /*
     * =============================================================
     * INITIAL LOAD
     * =============================================================
     */

    useEffect(() => {

        if (
            authLoading ||
            !token ||
            !studentId
        ) {
            return
        }

        loadJobs()
        loadApplications()

    }, [
        authLoading,
        token,
        studentId,
    ])


    /*
     * =============================================================
     * LOAD JOB POOL
     * =============================================================
     */

    async function loadJobs() {

        try {

            setLoading(true)
            setError('')

            const response =
                await fetch(
                    `${API_BASE_URL}/job-descriptions`
                )

            if (!response.ok) {

                throw new Error(
                    'Unable to load jobs.'
                )
            }

            const data =
                await response.json()

            setJobs(
                Array.isArray(data)
                    ? data
                    : []
            )

        } catch (err) {

            console.error(
                'Load jobs error:',
                err
            )

            setError(
                err.message ||
                'Something went wrong while loading jobs.'
            )

        } finally {

            setLoading(false)
        }
    }


    /*
     * =============================================================
     * LOAD EXISTING APPLICATIONS
     * =============================================================
     *
     * This tells the Jobs page which Job Pool
     * opportunities this logged-in student
     * has already applied to.
     */

    async function loadApplications() {

        if (
            !token ||
            !studentId
        ) {
            return
        }

        try {

            const response =
                await fetch(
                    `${API_BASE_URL}/applications/student/${studentId}`,
                    {
                        headers: {
                            Authorization:
                                `Bearer ${token}`,
                        },
                    }
                )

            if (!response.ok) {

                console.error(
                    'Unable to load applications:',
                    response.status
                )

                return
            }

            const data =
                await response.json()

            if (!Array.isArray(data)) {
                return
            }

            const ids =
                new Set()

            data.forEach(
                (application) => {

                    /*
                     * New Job Pool application
                     */
                    if (
                        application
                            ?.jobDescription
                            ?.id
                    ) {

                        ids.add(
                            application
                                .jobDescription
                                .id
                        )
                    }

                    /*
                     * Legacy application support
                     */
                    if (
                        application?.job?.id &&
                        !application?.jobDescription
                    ) {

                        ids.add(
                            application.job.id
                        )
                    }
                }
            )

            setAppliedJobIds(ids)

        } catch (err) {

            console.error(
                'Load applications error:',
                err
            )
        }
    }


    /*
     * =============================================================
     * APPLY TO JOB POOL JOB
     * =============================================================
     *
     * The backend now verifies the authenticated
     * student's identity.
     *
     * We still send studentId because the existing
     * API request object expects it, but the backend
     * no longer trusts it blindly.
     */

    async function handleApply(job) {

        if (
            !job?.id ||
            !token ||
            !studentId
        ) {
            return
        }

        /*
         * Prevent duplicate clicks.
         */
        if (
            appliedJobIds.has(job.id) ||
            applyingJobId === job.id
        ) {
            return
        }

        try {

            setApplyingJobId(job.id)
            setError('')

            const response =
                await fetch(
                    `${API_BASE_URL}/applications/from-job-description`,
                    {
                        method: 'POST',

                        headers: {

                            'Content-Type':
                                'application/json',

                            Authorization:
                                `Bearer ${token}`,
                        },

                        body:
                            JSON.stringify({
                                studentId:
                                studentId,

                                jobDescriptionId:
                                job.id,

                                notes:
                                    'Applied through Placement Intelligence Job Pool',
                            }),
                    }
                )

            const responseText =
                await response.text()

            if (!response.ok) {

                let message =
                    'Unable to apply to this job.'

                try {

                    const errorData =
                        JSON.parse(
                            responseText
                        )

                    if (
                        errorData?.message
                    ) {

                        message =
                            errorData.message
                    }

                } catch {

                    if (
                        responseText
                    ) {

                        message =
                            responseText
                    }
                }

                throw new Error(
                    message
                )
            }

            /*
             * Add the job to the local applied set
             * immediately.
             */
            setAppliedJobIds(
                (previous) => {

                    const updated =
                        new Set(previous)

                    updated.add(
                        job.id
                    )

                    return updated
                }
            )

        } catch (err) {

            console.error(
                'Apply job error:',
                err
            )

            setError(
                err.message ||
                'Unable to apply to this job.'
            )

        } finally {

            setApplyingJobId(null)
        }
    }


    /*
     * =============================================================
     * LOCATIONS
     * =============================================================
     */

    const locations = [
        ...new Set(
            jobs
                .map(
                    (job) =>
                        job.location
                )
                .filter(Boolean)
        ),
    ]


    /*
     * =============================================================
     * FILTER JOBS
     * =============================================================
     */

    const filteredJobs =
        jobs.filter(
            (job) => {

                const search =
                    searchTerm
                        .toLowerCase()
                        .trim()

                const skillsText =
                    Array.isArray(
                        job.extractedSkills
                    )
                        ? job.extractedSkills.join(
                            ' '
                        )
                        : typeof job.extractedSkills ===
                        'string'
                            ? job.extractedSkills
                            : ''

                const matchesSearch =
                    !search ||
                    String(
                        job.title || ''
                    )
                        .toLowerCase()
                        .includes(search) ||

                    String(
                        job.companyName || ''
                    )
                        .toLowerCase()
                        .includes(search) ||

                    String(
                        job.description || ''
                    )
                        .toLowerCase()
                        .includes(search) ||

                    skillsText
                        .toLowerCase()
                        .includes(search)

                const matchesLocation =
                    locationFilter ===
                    'ALL' ||
                    job.location ===
                    locationFilter

                return (
                    matchesSearch &&
                    matchesLocation
                )
            }
        )


    /*
     * =============================================================
     * SALARY
     * =============================================================
     */

    function formatSalary(job) {

        if (job.salaryText) {

            return job.salaryText
        }

        if (
            job.salary !== null &&
            job.salary !== undefined &&
            job.salary !== ''
        ) {

            return `₹${Number(
                job.salary
            ).toLocaleString('en-IN')}`
        }

        return 'Salary not specified'
    }


    /*
     * =============================================================
     * DATE
     * =============================================================
     */

    function formatDate(dateValue) {

        if (!dateValue) {
            return null
        }

        try {

            const date =
                new Date(dateValue)

            if (
                Number.isNaN(
                    date.getTime()
                )
            ) {

                return dateValue
            }

            return date.toLocaleDateString(
                'en-IN',
                {
                    day: 'numeric',
                    month: 'short',
                    year: 'numeric',
                }
            )

        } catch {

            return dateValue
        }
    }


    /*
     * =============================================================
     * SKILLS
     * =============================================================
     */

    function getSkills(job) {

        if (!job.extractedSkills) {
            return []
        }

        if (
            Array.isArray(
                job.extractedSkills
            )
        ) {

            return job.extractedSkills
        }

        return String(
            job.extractedSkills
        )
            .split(',')
            .map(
                (skill) =>
                    skill.trim()
            )
            .filter(Boolean)
    }


    /*
     * =============================================================
     * AUTH LOADING
     * =============================================================
     */

    if (authLoading) {

        return (
            <div className="flex min-h-[70vh] items-center justify-center">

                <div className="flex items-center gap-3 text-slate-400">

                    <Loader2
                        size={24}
                        className="animate-spin"
                    />

                    <span>
                        Loading your session...
                    </span>

                </div>

            </div>
        )
    }


    /*
     * =============================================================
     * NO SESSION
     * =============================================================
     */

    if (!token || !studentId) {

        return (
            <div className="rounded-2xl border border-yellow-500/20 bg-yellow-500/10 p-6">

                <div className="flex items-start gap-3">

                    <AlertTriangle
                        className="mt-0.5 text-yellow-400"
                        size={22}
                    />

                    <div>

                        <h2 className="font-semibold text-yellow-300">
                            Session required
                        </h2>

                        <p className="mt-1 text-sm text-yellow-200/80">
                            Please log in again to view your personalized jobs.
                        </p>

                        <Link
                            to="/login"
                            className="mt-4 inline-flex rounded-lg bg-yellow-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-yellow-400"
                        >
                            Go to Login
                        </Link>

                    </div>

                </div>

            </div>
        )
    }


    /*
     * =============================================================
     * LOADING
     * =============================================================
     */

    if (loading) {

        return (
            <div className="flex min-h-[70vh] items-center justify-center">

                <div className="flex items-center gap-3 text-slate-400">

                    <Loader2
                        size={24}
                        className="animate-spin"
                    />

                    <span>
                        Loading available jobs...
                    </span>

                </div>

            </div>
        )
    }


    /*
     * =============================================================
     * COMPLETE LOAD FAILURE
     * =============================================================
     */

    if (
        error &&
        jobs.length === 0
    ) {

        return (
            <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6">

                <div className="flex items-start gap-3">

                    <AlertTriangle
                        className="mt-0.5 text-red-400"
                        size={22}
                    />

                    <div>

                        <h2 className="font-semibold text-red-300">
                            Unable to load jobs
                        </h2>

                        <p className="mt-1 text-sm text-red-200/80">
                            {error}
                        </p>

                        <button
                            onClick={() => {
                                loadJobs()
                                loadApplications()
                            }}
                            className="mt-4 rounded-lg bg-red-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-400"
                        >
                            Try Again
                        </button>

                    </div>

                </div>

            </div>
        )
    }


    /*
     * =============================================================
     * PAGE
     * =============================================================
     */

    return (

        <div className="space-y-7">

            {/* =================================================
                HEADER
            ================================================== */}

            <section>

                <p className="text-sm font-medium text-blue-400">
                    Opportunities
                </p>

                <div className="mt-1 flex flex-col justify-between gap-4 lg:flex-row lg:items-end">

                    <div>

                        <h1 className="text-3xl font-bold tracking-tight text-white">
                            Jobs
                        </h1>

                        <p className="mt-2 text-slate-400">
                            Explore opportunities from your Job Pool and find roles that fit your profile.
                        </p>

                    </div>

                    <div className="flex flex-wrap items-center gap-3">

                        <div className="text-sm text-slate-500">

                            {filteredJobs.length}
                            {' '}
                            job
                            {filteredJobs.length !== 1
                                ? 's'
                                : ''}
                            {' '}
                            available

                        </div>

                        <Link
                            to="/job-descriptions"
                            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 transition hover:border-blue-500 hover:text-white"
                        >
                            Manage Job Pool
                        </Link>

                    </div>

                </div>

            </section>


            {/* =================================================
                ERROR
            ================================================== */}

            {error && (

                <div className="flex items-start justify-between gap-4 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">

                    <div className="flex items-start gap-3">

                        <AlertTriangle
                            size={19}
                            className="mt-0.5 shrink-0"
                        />

                        <span>
                            {error}
                        </span>

                    </div>

                    <button
                        type="button"
                        onClick={() =>
                            setError('')
                        }
                        className="text-red-400 transition hover:text-red-200"
                    >
                        ×
                    </button>

                </div>
            )}


            {/* =================================================
                SEARCH / FILTER
            ================================================== */}

            <section className="rounded-2xl border border-slate-800 bg-slate-950 p-4">

                <div className="grid gap-3 lg:grid-cols-[1fr_220px]">

                    <div className="relative">

                        <Search
                            size={20}
                            className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
                        />

                        <input
                            type="text"
                            value={searchTerm}
                            onChange={(event) =>
                                setSearchTerm(
                                    event.target.value
                                )
                            }
                            placeholder="Search by job title, company, skill, or keyword..."
                            className="w-full rounded-xl border border-slate-800 bg-slate-900 py-3 pl-11 pr-4 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500"
                        />

                    </div>


                    <select
                        value={
                            locationFilter
                        }
                        onChange={(event) =>
                            setLocationFilter(
                                event.target.value
                            )
                        }
                        className="rounded-xl border border-slate-800 bg-slate-900 px-4 py-3 text-sm text-slate-300 outline-none focus:border-blue-500"
                    >

                        <option value="ALL">
                            All Locations
                        </option>

                        {locations.map(
                            (location) => (

                                <option
                                    key={location}
                                    value={location}
                                >
                                    {location}
                                </option>

                            )
                        )}

                    </select>

                </div>

            </section>


            {/* =================================================
                EMPTY JOB POOL
            ================================================== */}

            {jobs.length === 0 ? (

                <section className="rounded-2xl border border-dashed border-slate-800 bg-slate-950 p-12 text-center">

                    <BriefcaseBusiness
                        size={42}
                        className="mx-auto text-slate-600"
                    />

                    <h2 className="mt-4 text-lg font-semibold text-white">
                        No jobs in your Job Pool
                    </h2>

                    <p className="mx-auto mt-2 max-w-lg text-sm text-slate-500">
                        Add job descriptions from any source to start building your personalized opportunity pool.
                    </p>

                    <Link
                        to="/job-descriptions"
                        className="mt-6 inline-flex items-center gap-2 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-500"
                    >
                        Add Job Description

                        <ArrowUpRight
                            size={16}
                        />

                    </Link>

                </section>

            ) : filteredJobs.length > 0 ? (

                /* =================================================
                   JOBS
                ================================================== */

                <section className="grid gap-5">

                    {filteredJobs.map(
                        (job) => {

                            const skills =
                                getSkills(
                                    job
                                )

                            const isApplied =
                                appliedJobIds.has(
                                    job.id
                                )

                            const isApplying =
                                applyingJobId ===
                                job.id

                            return (

                                <article
                                    key={job.id}
                                    className="rounded-2xl border border-slate-800 bg-slate-950 p-6 transition hover:border-slate-700 hover:bg-slate-950/80"
                                >

                                    {/* TOP */}

                                    <div className="flex flex-col justify-between gap-5 lg:flex-row">

                                        <div className="flex gap-4">

                                            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-500/10 text-blue-400">

                                                <Building2
                                                    size={23}
                                                />

                                            </div>

                                            <div>

                                                <h2 className="text-xl font-semibold text-white">
                                                    {job.title ||
                                                        'Untitled Job'}
                                                </h2>

                                                <p className="mt-1 font-medium text-slate-300">
                                                    {job.companyName ||
                                                        'Company not specified'}
                                                </p>

                                                <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-slate-500">

                                                    {job.location && (

                                                        <span className="flex items-center gap-1.5">

                                                            <MapPin
                                                                size={15}
                                                            />

                                                            {
                                                                job.location
                                                            }

                                                        </span>

                                                    )}

                                                    {job.jobType && (

                                                        <span className="flex items-center gap-1.5">

                                                            <BriefcaseBusiness
                                                                size={15}
                                                            />

                                                            {
                                                                job.jobType
                                                            }

                                                        </span>

                                                    )}

                                                    {job.workMode && (

                                                        <span className="flex items-center gap-1.5">

                                                            <Clock3
                                                                size={15}
                                                            />

                                                            {
                                                                job.workMode
                                                            }

                                                        </span>

                                                    )}

                                                </div>

                                            </div>

                                        </div>


                                        {/* SALARY */}

                                        <div className="lg:min-w-[180px] lg:text-right">

                                            <div className="flex items-center gap-1.5 text-lg font-semibold text-green-400 lg:justify-end">

                                                {job.salary !==
                                                    null &&
                                                    job.salary !==
                                                    undefined &&
                                                    !job.salaryText && (

                                                        <IndianRupee
                                                            size={17}
                                                        />

                                                    )}

                                                {formatSalary(
                                                    job
                                                )}

                                            </div>

                                            <p className="mt-1 text-xs text-slate-500">
                                                Compensation
                                            </p>

                                        </div>

                                    </div>


                                    {/* METADATA */}

                                    <div className="mt-5 flex flex-wrap gap-3">

                                        {job.experience && (

                                            <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-300">

                                                <Clock3
                                                    size={14}
                                                    className="text-slate-500"
                                                />

                                                {
                                                    job.experience
                                                }

                                            </span>

                                        )}

                                        {job.education && (

                                            <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-300">

                                                <GraduationCap
                                                    size={14}
                                                    className="text-slate-500"
                                                />

                                                {
                                                    job.education
                                                }

                                            </span>

                                        )}

                                        {job.postedDate && (

                                            <span className="inline-flex items-center gap-1.5 rounded-lg border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-300">

                                                <CalendarDays
                                                    size={14}
                                                    className="text-slate-500"
                                                />

                                                Posted{' '}

                                                {formatDate(
                                                    job.postedDate
                                                )}

                                            </span>

                                        )}

                                        {job.applicationDeadline && (

                                            <span className="inline-flex items-center gap-1.5 rounded-lg border border-orange-500/20 bg-orange-500/5 px-3 py-2 text-xs text-orange-300">

                                                <CalendarDays
                                                    size={14}
                                                />

                                                Deadline{' '}

                                                {formatDate(
                                                    job.applicationDeadline
                                                )}

                                            </span>

                                        )}

                                    </div>


                                    {/* DESCRIPTION */}

                                    {job.description && (

                                        <div className="mt-5">

                                            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">
                                                Job Description
                                            </p>

                                            <p className="max-w-5xl text-sm leading-6 text-slate-400">
                                                {
                                                    job.description
                                                }
                                            </p>

                                        </div>

                                    )}


                                    {/* SKILLS */}

                                    {skills.length > 0 && (

                                        <div className="mt-5">

                                            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-slate-500">
                                                Required Skills
                                            </p>

                                            <div className="flex flex-wrap gap-2">

                                                {skills.map(
                                                    (
                                                        skill,
                                                        index
                                                    ) => (

                                                        <span
                                                            key={`${skill}-${index}`}
                                                            className="rounded-full border border-slate-700 bg-slate-900 px-3 py-1 text-xs font-medium text-slate-300"
                                                        >
                                                            {skill}
                                                        </span>

                                                    )
                                                )}

                                            </div>

                                        </div>

                                    )}


                                    {/* FOOTER */}

                                    <div className="mt-6 flex flex-col justify-between gap-3 border-t border-slate-800 pt-5 sm:flex-row sm:items-center">

                                        <span className="text-xs text-slate-500">

                                            Job ID: #
                                            {job.id}

                                        </span>


                                        <div className="flex flex-wrap gap-2">

                                            {/* ANALYZE MATCH */}

                                            <Link
                                                to={`/match?job=${job.id}`}
                                                className="flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-blue-500"
                                            >

                                                Analyze Match

                                                <ArrowUpRight
                                                    size={16}
                                                />

                                            </Link>


                                            {/* INTERNAL APPLICATION */}

                                            <button
                                                type="button"
                                                onClick={() =>
                                                    handleApply(
                                                        job
                                                    )
                                                }
                                                disabled={
                                                    isApplied ||
                                                    isApplying
                                                }
                                                className={`flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition ${
                                                    isApplied
                                                        ? 'cursor-default border border-emerald-500/20 bg-emerald-500/10 text-emerald-300'
                                                        : 'bg-cyan-500 text-slate-950 hover:bg-cyan-400'
                                                } ${
                                                    isApplying
                                                        ? 'cursor-wait opacity-70'
                                                        : ''
                                                }`}
                                            >

                                                {isApplying ? (

                                                    <>
                                                        <Loader2
                                                            size={15}
                                                            className="animate-spin"
                                                        />

                                                        Applying...
                                                    </>

                                                ) : isApplied ? (

                                                    <>
                                                        <CheckCircle2
                                                            size={15}
                                                        />

                                                        Applied
                                                    </>

                                                ) : (

                                                    <>
                                                        Apply
                                                    </>

                                                )}

                                            </button>


                                            {/* COMPANY SITE */}

                                            {job.applicationUrl && (

                                                <a
                                                    href={
                                                        job.applicationUrl
                                                    }
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="flex items-center justify-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-semibold text-slate-200 transition hover:border-slate-500 hover:text-white"
                                                >

                                                    Company Site

                                                    <ExternalLink
                                                        size={15}
                                                    />

                                                </a>

                                            )}

                                        </div>

                                    </div>

                                </article>
                            )
                        }
                    )}

                </section>

            ) : (

                /* =================================================
                   SEARCH EMPTY STATE
                ================================================== */

                <section className="rounded-2xl border border-dashed border-slate-800 bg-slate-950 p-12 text-center">

                    <Search
                        size={38}
                        className="mx-auto text-slate-600"
                    />

                    <h2 className="mt-4 text-lg font-semibold text-white">
                        No jobs found
                    </h2>

                    <p className="mt-2 text-sm text-slate-500">
                        Try changing your search or location filter.
                    </p>

                </section>
            )}

        </div>
    )
}

export default Jobs