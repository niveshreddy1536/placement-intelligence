import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
    BriefcaseBusiness,
    CalendarDays,
    CheckCircle2,
    ClipboardList,
    ExternalLink,
    Search,
    Sparkles,
    XCircle,
    Loader2,
} from 'lucide-react'
import { useAuth } from '../auth/AuthContext'

const API_BASE_URL = 'http://localhost:8080'

const STATUS_OPTIONS = [
    'APPLIED',
    'SHORTLISTED',
    'INTERVIEW',
    'SELECTED',
    'REJECTED',
    'WITHDRAWN',
]

const STATUS_LABELS = {
    APPLIED: 'Applied',
    SHORTLISTED: 'Shortlisted',
    INTERVIEW: 'Interview',
    SELECTED: 'Selected',
    REJECTED: 'Rejected',
    WITHDRAWN: 'Withdrawn',
}

function normalizeStatus(status) {
    return String(status || 'APPLIED').toUpperCase()
}

function getStatusLabel(status) {
    const normalized = normalizeStatus(status)

    return STATUS_LABELS[normalized] || normalized
}

function getStatusClass(status) {
    switch (normalizeStatus(status)) {
        case 'APPLIED':
            return 'border-cyan-500/20 bg-cyan-500/10 text-cyan-300'

        case 'SHORTLISTED':
            return 'border-purple-500/20 bg-purple-500/10 text-purple-300'

        case 'INTERVIEW':
            return 'border-yellow-500/20 bg-yellow-500/10 text-yellow-300'

        case 'SELECTED':
            return 'border-emerald-500/20 bg-emerald-500/10 text-emerald-300'

        case 'REJECTED':
            return 'border-red-500/20 bg-red-500/10 text-red-300'

        case 'WITHDRAWN':
            return 'border-slate-500/20 bg-slate-500/10 text-slate-300'

        default:
            return 'border-slate-700 bg-slate-800 text-slate-300'
    }
}

function getStatusDotClass(status) {
    switch (normalizeStatus(status)) {
        case 'APPLIED':
            return 'bg-cyan-400'

        case 'SHORTLISTED':
            return 'bg-purple-400'

        case 'INTERVIEW':
            return 'bg-yellow-400'

        case 'SELECTED':
            return 'bg-emerald-400'

        case 'REJECTED':
            return 'bg-red-400'

        case 'WITHDRAWN':
            return 'bg-slate-400'

        default:
            return 'bg-slate-400'
    }
}

function formatDate(date) {
    if (!date) {
        return '—'
    }

    const parsedDate = new Date(date)

    if (Number.isNaN(parsedDate.getTime())) {
        return String(date)
    }

    return parsedDate.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
    })
}

function formatSalary(salary) {
    if (
        salary === null ||
        salary === undefined ||
        salary === ''
    ) {
        return 'Not specified'
    }

    const numericSalary = Number(salary)

    if (Number.isNaN(numericSalary)) {
        return String(salary)
    }

    return `₹${numericSalary.toLocaleString('en-IN')}`
}

function StatusBadge({ status }) {
    return (
        <span
            className={`inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-semibold ${getStatusClass(
                status
            )}`}
        >
            <span
                className={`h-1.5 w-1.5 rounded-full ${getStatusDotClass(
                    status
                )}`}
            />

            {getStatusLabel(status)}
        </span>
    )
}

function SummaryCard({
                         label,
                         value,
                         icon,
                         className = '',
                     }) {
    return (
        <div
            className={`rounded-2xl border bg-slate-900/70 p-5 ${className}`}
        >
            <div className="flex items-start justify-between">
                <div>
                    <p className="text-sm font-medium text-slate-400">
                        {label}
                    </p>

                    <p className="mt-2 text-3xl font-bold text-white">
                        {value}
                    </p>
                </div>

                <div className="text-slate-500">
                    {icon}
                </div>
            </div>
        </div>
    )
}

function Applications() {
    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [searchTerm, setSearchTerm] = useState('')
    const [activeFilter, setActiveFilter] = useState('ALL')
    const [updatingId, setUpdatingId] = useState(null)

    useEffect(() => {
        if (
            authLoading ||
            !studentId ||
            !token
        ) {
            return
        }

        loadApplications()
    }, [
        authLoading,
        studentId,
        token,
    ])

    async function loadApplications() {
        try {
            setLoading(true)
            setError('')

            const response = await fetch(
                `${API_BASE_URL}/api/applications/student/${studentId}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            )

            if (!response.ok) {
                throw new Error(
                    `Unable to load applications. Status: ${response.status}`
                )
            }

            const data = await response.json()

            setApplications(
                Array.isArray(data)
                    ? data
                    : []
            )
        } catch (err) {
            console.error(
                'Load applications error:',
                err
            )

            setError(
                err.message ||
                'Unable to load your applications.'
            )
        } finally {
            setLoading(false)
        }
    }

    async function updateStatus(
        applicationId,
        newStatus
    ) {
        try {
            setUpdatingId(applicationId)
            setError('')

            const currentApplication =
                applications.find(
                    (application) =>
                        application.id ===
                        applicationId
                )

            if (!currentApplication) {
                throw new Error(
                    'Application not found.'
                )
            }

            if (
                !STATUS_OPTIONS.includes(
                    newStatus
                )
            ) {
                throw new Error(
                    `Invalid application status: ${newStatus}`
                )
            }

            const jobDescription =
                currentApplication.jobDescription ||
                null

            const legacyJob =
                currentApplication.job ||
                null

            const payload = {
                student: {
                    id: studentId,
                },

                studentName:
                currentApplication.studentName,

                appliedDate:
                currentApplication.appliedDate,

                status: newStatus,

                notes:
                currentApplication.notes,
            }

            /*
             * Preserve the Job Pool relationship
             * when the application came from it.
             */
            if (
                jobDescription?.id
            ) {
                payload.jobDescription = {
                    id: jobDescription.id,
                }
            } else if (
                legacyJob?.id
            ) {
                payload.job = {
                    id: legacyJob.id,
                }
            }

            const response = await fetch(
                `${API_BASE_URL}/api/applications/${applicationId}`,
                {
                    method: 'PUT',

                    headers: {
                        'Content-Type':
                            'application/json',

                        Authorization:
                            `Bearer ${token}`,
                    },

                    body: JSON.stringify(
                        payload
                    ),
                }
            )

            const responseText =
                await response.text()

            if (!response.ok) {
                let message =
                    'Could not update the application status.'

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

            let savedApplication =
                null

            if (
                responseText &&
                responseText.trim()
            ) {
                try {
                    savedApplication =
                        JSON.parse(
                            responseText
                        )
                } catch {
                    savedApplication =
                        null
                }
            }

            const updatedApplication = {
                ...currentApplication,

                ...(savedApplication ||
                    {}),

                jobDescription:
                    savedApplication?.jobDescription ||
                    currentApplication.jobDescription,

                job:
                    savedApplication?.job ||
                    currentApplication.job,

                student:
                    savedApplication?.student ||
                    currentApplication.student,

                appliedDate:
                    savedApplication?.appliedDate ??
                    currentApplication.appliedDate,

                status:
                    savedApplication?.status ||
                    newStatus,
            }

            setApplications(
                (previous) =>
                    previous.map(
                        (application) =>
                            application.id ===
                            applicationId
                                ? updatedApplication
                                : application
                    )
            )
        } catch (err) {
            console.error(
                'Update application error:',
                err
            )

            setError(
                err.message ||
                'Could not update the application status.'
            )
        } finally {
            setUpdatingId(null)
        }
    }

    const filteredApplications =
        useMemo(() => {
            const search =
                searchTerm
                    .trim()
                    .toLowerCase()

            return applications.filter(
                (application) => {
                    const status =
                        normalizeStatus(
                            application.status
                        )

                    const job =
                        application.jobDescription ||
                        application.job ||
                        {}

                    const company =
                        job.companyName ||
                        job.company ||
                        ''

                    const title =
                        job.jobTitle ||
                        job.title ||
                        ''

                    const location =
                        job.location ||
                        ''

                    const matchesSearch =
                        !search ||
                        company
                            .toLowerCase()
                            .includes(
                                search
                            ) ||
                        title
                            .toLowerCase()
                            .includes(
                                search
                            ) ||
                        location
                            .toLowerCase()
                            .includes(
                                search
                            )

                    const matchesFilter =
                        activeFilter ===
                        'ALL' ||
                        status ===
                        activeFilter

                    return (
                        matchesSearch &&
                        matchesFilter
                    )
                }
            )
        }, [
            applications,
            searchTerm,
            activeFilter,
        ])

    const totalCount =
        applications.length

    const appliedCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'APPLIED'
        ).length

    const shortlistedCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'SHORTLISTED'
        ).length

    const interviewCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'INTERVIEW'
        ).length

    const selectedCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'SELECTED'
        ).length

    const rejectedCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'REJECTED'
        ).length

    const withdrawnCount =
        applications.filter(
            (application) =>
                normalizeStatus(
                    application.status
                ) === 'WITHDRAWN'
        ).length

    if (
        authLoading ||
        !studentId ||
        !token ||
        loading
    ) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center">
                <div className="flex items-center gap-3 text-slate-400">
                    <Loader2
                        size={24}
                        className="animate-spin"
                    />

                    <span>
                        Loading your applications...
                    </span>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-slate-950 px-4 py-6 text-white md:px-8 lg:px-10">
            <div className="mx-auto max-w-7xl">

                {/* HEADER */}

                <section className="mb-8">
                    <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-end">

                        <div>
                            <p className="text-sm font-medium uppercase tracking-wider text-cyan-400">
                                Application Intelligence
                            </p>

                            <h1 className="mt-2 text-3xl font-bold tracking-tight text-white md:text-4xl">
                                Applications
                            </h1>

                            <p className="mt-2 max-w-2xl text-slate-400">
                                Track your job applications, monitor progress, and keep your application pipeline organized.
                            </p>
                        </div>

                        <Link
                            to="/jobs"
                            className="inline-flex w-fit items-center gap-2 rounded-xl border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-200 transition hover:border-cyan-500/40 hover:bg-slate-800 hover:text-white"
                        >
                            <BriefcaseBusiness
                                size={17}
                            />

                            Explore Jobs
                        </Link>

                    </div>
                </section>

                {/* ERROR */}

                {error && (
                    <div className="mb-6 flex items-start justify-between gap-4 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">

                        <div className="flex items-start gap-3">
                            <XCircle
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
                            className="text-red-400 hover:text-red-200"
                        >
                            ×
                        </button>

                    </div>
                )}

                {/* SUMMARY */}

                <section className="mb-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">

                    <SummaryCard
                        label="Total"
                        value={totalCount}
                        icon={
                            <ClipboardList
                                size={21}
                            />
                        }
                        className="border-slate-800"
                    />

                    <SummaryCard
                        label="Applied"
                        value={appliedCount}
                        icon={
                            <CheckCircle2
                                size={21}
                            />
                        }
                        className="border-cyan-500/20"
                    />

                    <SummaryCard
                        label="Shortlisted"
                        value={shortlistedCount}
                        icon={
                            <Sparkles
                                size={21}
                            />
                        }
                        className="border-purple-500/20"
                    />

                    <SummaryCard
                        label="Interviews"
                        value={interviewCount}
                        icon={
                            <CalendarDays
                                size={21}
                            />
                        }
                        className="border-yellow-500/20"
                    />

                    <SummaryCard
                        label="Selected"
                        value={selectedCount}
                        icon={
                            <CheckCircle2
                                size={21}
                            />
                        }
                        className="border-emerald-500/20"
                    />

                    <SummaryCard
                        label="Rejected"
                        value={rejectedCount}
                        icon={
                            <XCircle
                                size={21}
                            />
                        }
                        className="border-red-500/20"
                    />

                </section>

                {withdrawnCount >
                    0 && (
                        <div className="mb-6 rounded-xl border border-slate-800 bg-slate-900/50 px-4 py-3 text-sm text-slate-400">
                            Withdrawn applications:{' '}
                            <span className="font-semibold text-slate-200">
                            {withdrawnCount}
                        </span>
                        </div>
                    )}

                {/* SEARCH */}

                <section className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-4">

                    <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">

                        <div className="relative w-full lg:max-w-md">

                            <Search
                                size={18}
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
                                placeholder="Search company, role or location..."
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 py-3 pl-11 pr-4 text-sm text-white outline-none placeholder:text-slate-600 focus:border-cyan-500"
                            />

                        </div>

                        <div className="flex flex-wrap gap-2">

                            {[
                                ['All', 'ALL'],
                                ['Applied', 'APPLIED'],
                                ['Shortlisted', 'SHORTLISTED'],
                                ['Interview', 'INTERVIEW'],
                                ['Selected', 'SELECTED'],
                                ['Rejected', 'REJECTED'],
                                ['Withdrawn', 'WITHDRAWN'],
                            ].map(
                                ([label, value]) => (
                                    <button
                                        key={value}
                                        type="button"
                                        onClick={() =>
                                            setActiveFilter(
                                                value
                                            )
                                        }
                                        className={`rounded-lg border px-4 py-2 text-xs font-semibold transition ${
                                            activeFilter ===
                                            value
                                                ? 'border-cyan-500/30 bg-cyan-500/10 text-cyan-300'
                                                : 'border-slate-700 bg-slate-950 text-slate-400 hover:bg-slate-800 hover:text-white'
                                        }`}
                                    >
                                        {label}
                                    </button>
                                )
                            )}

                        </div>

                    </div>

                </section>

                {/* APPLICATIONS */}

                {filteredApplications.length ===
                0 ? (
                    <section className="rounded-2xl border border-dashed border-slate-800 bg-slate-900/50 p-12 text-center">

                        <ClipboardList
                            size={42}
                            className="mx-auto text-slate-600"
                        />

                        <h2 className="mt-4 text-lg font-semibold text-white">
                            {applications.length ===
                            0
                                ? 'No applications yet'
                                : 'No matching applications'}
                        </h2>

                        <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-500">
                            {applications.length ===
                            0
                                ? 'Start applying to jobs and track your progress here.'
                                : 'Try changing your search or status filter.'}
                        </p>

                        {applications.length ===
                            0 && (
                                <Link
                                    to="/jobs"
                                    className="mt-6 inline-flex items-center gap-2 rounded-xl bg-cyan-500 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                                >
                                    <BriefcaseBusiness
                                        size={17}
                                    />

                                    Explore Jobs
                                </Link>
                            )}

                    </section>
                ) : (
                    <section className="space-y-5">

                        {filteredApplications.map(
                            (application) => {
                                const job =
                                    application.jobDescription ||
                                    application.job ||
                                    {}

                                const status =
                                    normalizeStatus(
                                        application.status
                                    )

                                const isUpdating =
                                    updatingId ===
                                    application.id

                                return (
                                    <article
                                        key={
                                            application.id
                                        }
                                        className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 transition hover:border-slate-700 md:p-6"
                                    >

                                        <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-start">

                                            <div>

                                                <div className="flex flex-wrap items-center gap-2">

                                                    <StatusBadge
                                                        status={
                                                            status
                                                        }
                                                    />

                                                    <span className="text-xs text-slate-600">
                                                        Application #
                                                        {
                                                            application.id
                                                        }
                                                    </span>

                                                </div>

                                                <h2 className="mt-4 text-xl font-bold text-white md:text-2xl">
                                                    {job.jobTitle ||
                                                        job.title ||
                                                        'Job Title Not Available'}
                                                </h2>

                                                <p className="mt-1 text-sm font-medium text-slate-400">
                                                    {job.companyName ||
                                                        job.company ||
                                                        'Company not specified'}
                                                </p>

                                            </div>

                                            <div className="w-full lg:w-52">

                                                <label className="mb-2 block text-xs font-medium uppercase tracking-wider text-slate-500">
                                                    Update Status
                                                </label>

                                                <select
                                                    value={
                                                        status
                                                    }
                                                    disabled={
                                                        isUpdating
                                                    }
                                                    onChange={(
                                                        event
                                                    ) =>
                                                        updateStatus(
                                                            application.id,
                                                            event.target.value
                                                        )
                                                    }
                                                    className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm font-semibold text-white outline-none focus:border-cyan-500 disabled:cursor-not-allowed disabled:opacity-60"
                                                >

                                                    {STATUS_OPTIONS.map(
                                                        (
                                                            option
                                                        ) => (
                                                            <option
                                                                key={
                                                                    option
                                                                }
                                                                value={
                                                                    option
                                                                }
                                                            >
                                                                {
                                                                    STATUS_LABELS[
                                                                        option
                                                                        ]
                                                                }
                                                            </option>
                                                        )
                                                    )}

                                                </select>

                                                {isUpdating && (
                                                    <p className="mt-2 text-xs text-cyan-400">
                                                        Saving status...
                                                    </p>
                                                )}

                                            </div>

                                        </div>

                                        {/* JOB DETAILS */}

                                        <div className="mt-6 grid gap-4 border-y border-slate-800 py-5 sm:grid-cols-2 lg:grid-cols-4">

                                            <div>
                                                <p className="text-xs uppercase tracking-wider text-slate-600">
                                                    Location
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-200">
                                                    {job.location ||
                                                        'Not specified'}
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs uppercase tracking-wider text-slate-600">
                                                    Job Type
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-200">
                                                    {job.jobType ||
                                                        'Not specified'}
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs uppercase tracking-wider text-slate-600">
                                                    Salary
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-200">
                                                    {formatSalary(
                                                        job.salary
                                                    )}
                                                </p>
                                            </div>

                                            <div>
                                                <p className="text-xs uppercase tracking-wider text-slate-600">
                                                    Applied Date
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-200">
                                                    {formatDate(
                                                        application.appliedDate
                                                    )}
                                                </p>
                                            </div>

                                        </div>

                                        {/* FOOTER */}

                                        <div className="flex flex-col justify-between gap-4 pt-5 sm:flex-row sm:items-center">

                                            <div>
                                                <p className="text-xs text-slate-500">
                                                    Current status
                                                </p>

                                                <p className="mt-1 text-sm font-semibold text-slate-300">
                                                    {getStatusLabel(
                                                        status
                                                    )}
                                                </p>
                                            </div>

                                            <div className="flex flex-wrap gap-2">

                                                {job.id && (
                                                    <Link
                                                        to={`/match?job=${job.id}`}
                                                        className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-4 py-2.5 text-sm font-semibold text-slate-300 transition hover:bg-slate-800 hover:text-white"
                                                    >
                                                        <Sparkles
                                                            size={
                                                                16
                                                            }
                                                        />

                                                        Analyze Match
                                                    </Link>
                                                )}

                                                {job.id && (
                                                    <Link
                                                        to={`/skill-gaps?job=${job.id}`}
                                                        className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950 px-4 py-2.5 text-sm font-semibold text-slate-300 transition hover:bg-slate-800 hover:text-white"
                                                    >
                                                        Skill Gaps
                                                    </Link>
                                                )}

                                                {job.applicationUrl && (
                                                    <a
                                                        href={
                                                            job.applicationUrl
                                                        }
                                                        target="_blank"
                                                        rel="noreferrer"
                                                        className="inline-flex items-center gap-2 rounded-xl bg-cyan-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                                                    >
                                                        <ExternalLink
                                                            size={
                                                                16
                                                            }
                                                        />

                                                        Job Link
                                                    </a>
                                                )}

                                            </div>

                                        </div>

                                    </article>
                                )
                            }
                        )}

                    </section>
                )}

            </div>
        </div>
    )
}

export default Applications