import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

const API_BASE_URL = 'http://localhost:8080'

const emptyForm = {
    title: '',
    companyName: '',
    location: '',
    jobType: '',
    workMode: '',
    salary: '',
    salaryText: '',
    experience: '',
    education: '',
    applicationUrl: '',
    postedDate: '',
    applicationDeadline: '',
    description: '',
    extractedResponsibilities: '',
    extractedRequirements: '',
}

function JobDescriptions() {
    const [form, setForm] = useState(emptyForm)
    const [jobDescriptions, setJobDescriptions] = useState([])

    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [deletingId, setDeletingId] = useState(null)

    const [editingId, setEditingId] = useState(null)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    useEffect(() => {
        loadJobDescriptions()
    }, [])

    async function loadJobDescriptions() {
        try {
            setLoading(true)
            setError('')

            const response = await fetch(
                `${API_BASE_URL}/api/job-descriptions`
            )

            if (!response.ok) {
                throw new Error(
                    `Request failed with status ${response.status}`
                )
            }

            const data = await response.json()

            setJobDescriptions(
                Array.isArray(data) ? data : []
            )
        } catch (err) {
            console.error(err)

            setError(
                'Unable to load job descriptions. Make sure the backend is running.'
            )
        } finally {
            setLoading(false)
        }
    }

    function handleChange(event) {
        const { name, value } = event.target

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }))
    }

    /*
     * =========================================================
     * ADD / UPDATE JOB DESCRIPTION
     * =========================================================
     */

    async function handleSubmit(event) {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (!form.title.trim()) {
            setError('Job title is required.')
            return
        }

        if (!form.description.trim()) {
            setError('Job description is required.')
            return
        }

        try {
            setSaving(true)

            const payload = {
                ...form,
                salary: form.salary
                    ? Number(form.salary)
                    : null,
            }

            const url = editingId
                ? `${API_BASE_URL}/api/job-descriptions/${editingId}`
                : `${API_BASE_URL}/api/job-descriptions`

            const response = await fetch(
                url,
                {
                    method: editingId
                        ? 'PUT'
                        : 'POST',

                    headers: {
                        'Content-Type': 'application/json',
                    },

                    body: JSON.stringify(payload),
                }
            )

            if (!response.ok) {
                const message =
                    await response.text()

                throw new Error(
                    message ||
                    `Request failed with status ${response.status}`
                )
            }

            const savedJob =
                await response.json()

            if (editingId) {
                /*
                 * Replace the existing record.
                 */
                setJobDescriptions(
                    (previous) =>
                        previous.map((job) =>
                            job.id === savedJob.id
                                ? savedJob
                                : job
                        )
                )

                setSuccess(
                    'Job description updated successfully. Skills were re-analyzed.'
                )
            } else {
                /*
                 * Add new record to the beginning.
                 */
                setJobDescriptions(
                    (previous) => [
                        savedJob,
                        ...previous,
                    ]
                )

                setSuccess(
                    'Job description added successfully.'
                )
            }

            setForm(emptyForm)
            setEditingId(null)

        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to save the job description.'
            )
        } finally {
            setSaving(false)
        }
    }

    /*
     * =========================================================
     * START EDITING
     * =========================================================
     */

    function handleEdit(job) {
        setError('')
        setSuccess('')

        setEditingId(job.id)

        setForm({
            title: job.title || '',
            companyName: job.companyName || '',
            location: job.location || '',
            jobType: job.jobType || '',
            workMode: job.workMode || '',
            salary:
                job.salary !== null &&
                job.salary !== undefined
                    ? String(job.salary)
                    : '',
            salaryText: job.salaryText || '',
            experience: job.experience || '',
            education: job.education || '',
            applicationUrl:
                job.applicationUrl || '',
            postedDate:
                formatDateForInput(
                    job.postedDate
                ),
            applicationDeadline:
                formatDateForInput(
                    job.applicationDeadline
                ),
            description:
                job.description || '',
            extractedResponsibilities:
                job.extractedResponsibilities || '',
            extractedRequirements:
                job.extractedRequirements || '',
        })

        /*
         * Scroll back to the form so the user
         * can immediately edit the JD.
         */
        window.scrollTo({
            top: 0,
            behavior: 'smooth',
        })
    }

    /*
     * =========================================================
     * CANCEL EDITING
     * =========================================================
     */

    function handleCancelEdit() {
        setEditingId(null)
        setForm(emptyForm)
        setError('')
        setSuccess('')
    }

    /*
     * =========================================================
     * DELETE JOB DESCRIPTION
     * =========================================================
     */

    async function handleDelete(job) {
        const confirmed =
            window.confirm(
                `Are you sure you want to delete "${job.title}"${job.companyName ? ` at ${job.companyName}` : ''}?\n\nThis job will be permanently removed from the Job Pool.`
            )

        if (!confirmed) {
            return
        }

        try {
            setDeletingId(job.id)
            setError('')
            setSuccess('')

            const response = await fetch(
                `${API_BASE_URL}/api/job-descriptions/${job.id}`,
                {
                    method: 'DELETE',
                }
            )

            if (!response.ok) {
                const message =
                    await response.text()

                throw new Error(
                    message ||
                    `Delete failed with status ${response.status}`
                )
            }

            /*
             * Remove the deleted job immediately
             * from the UI.
             */
            setJobDescriptions(
                (previous) =>
                    previous.filter(
                        (item) =>
                            item.id !== job.id
                    )
            )

            /*
             * If the user happened to be editing
             * this job, clear the form.
             */
            if (editingId === job.id) {
                setEditingId(null)
                setForm(emptyForm)
            }

            setSuccess(
                'Job description deleted successfully.'
            )

        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to delete the job description.'
            )
        } finally {
            setDeletingId(null)
        }
    }

    /*
     * =========================================================
     * DATE HELPERS
     * =========================================================
     */

    function formatDate(date) {
        if (!date) {
            return ''
        }

        try {
            return new Date(
                date
            ).toLocaleDateString()
        } catch {
            return date
        }
    }

    function formatDateForInput(date) {
        if (!date) {
            return ''
        }

        /*
         * If the backend already returns YYYY-MM-DD,
         * use it directly.
         */
        if (
            typeof date === 'string' &&
            /^\d{4}-\d{2}-\d{2}$/.test(date)
        ) {
            return date
        }

        try {
            const parsedDate =
                new Date(date)

            if (
                Number.isNaN(
                    parsedDate.getTime()
                )
            ) {
                return ''
            }

            return parsedDate
                .toISOString()
                .split('T')[0]
        } catch {
            return ''
        }
    }

    /*
     * =========================================================
     * SKILL HELPERS
     * =========================================================
     */

    function getSkills(job) {
        if (!job.extractedSkills) {
            return []
        }

        return job.extractedSkills
            .split(',')
            .map((skill) => skill.trim())
            .filter(Boolean)
    }

    return (
        <div className="min-h-screen bg-slate-950 px-4 py-8 text-white sm:px-6 lg:px-8">

            <div className="mx-auto max-w-7xl">

                {/* =================================================
                    HEADER
                ================================================= */}

                <div className="mb-8">

                    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">

                        <div>

                            <p className="mb-2 text-sm font-medium uppercase tracking-wider text-cyan-400">
                                Placement Intelligence
                            </p>

                            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
                                Job Descriptions
                            </h1>

                            <p className="mt-2 max-w-2xl text-slate-400">
                                Add job descriptions to your placement pool.
                                The system will automatically extract relevant
                                skills so your resume can be compared against
                                multiple opportunities.
                            </p>

                        </div>

                        <Link
                            to="/match"
                            className="inline-flex items-center justify-center rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-semibold text-slate-200 transition hover:border-cyan-500/40 hover:bg-slate-800"
                        >
                            Analyze a Match
                        </Link>

                    </div>

                </div>

                {/* =================================================
                    MESSAGES
                ================================================= */}

                {error && (
                    <div className="mb-6 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                        {error}
                    </div>
                )}

                {success && (
                    <div className="mb-6 rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
                        {success}
                    </div>
                )}

                {/* =================================================
                    ADD / EDIT FORM
                ================================================= */}

                <section className="mb-10 rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-xl sm:p-7">

                    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">

                        <div>

                            <h2 className="text-xl font-semibold text-white">

                                {editingId
                                    ? 'Edit Job Description'
                                    : 'Add Job Description'}

                            </h2>

                            <p className="mt-1 text-sm text-slate-400">

                                {editingId
                                    ? 'Update the job information below. The system will re-analyze the job skills when you save.'
                                    : 'Enter the available information. Skill extraction is handled automatically by the backend.'}

                            </p>

                        </div>

                        {editingId && (
                            <span className="inline-flex w-fit rounded-lg border border-cyan-500/20 bg-cyan-500/10 px-3 py-1.5 text-xs font-semibold text-cyan-300">
                                Editing Job #{editingId}
                            </span>
                        )}

                    </div>

                    <form onSubmit={handleSubmit}>

                        <div className="grid gap-5 md:grid-cols-2">

                            <Input
                                label="Job Title *"
                                name="title"
                                value={form.title}
                                onChange={handleChange}
                                placeholder="Java Backend Developer"
                            />

                            <Input
                                label="Company"
                                name="companyName"
                                value={form.companyName}
                                onChange={handleChange}
                                placeholder="Google"
                            />

                            <Input
                                label="Location"
                                name="location"
                                value={form.location}
                                onChange={handleChange}
                                placeholder="Bangalore"
                            />

                            <Input
                                label="Job Type"
                                name="jobType"
                                value={form.jobType}
                                onChange={handleChange}
                                placeholder="Full Time"
                            />

                            <Input
                                label="Work Mode"
                                name="workMode"
                                value={form.workMode}
                                onChange={handleChange}
                                placeholder="Remote / Hybrid / On-site"
                            />

                            <Input
                                label="Salary"
                                name="salary"
                                type="number"
                                value={form.salary}
                                onChange={handleChange}
                                placeholder="1200000"
                            />

                            <Input
                                label="Salary Text"
                                name="salaryText"
                                value={form.salaryText}
                                onChange={handleChange}
                                placeholder="₹10 - ₹15 LPA"
                            />

                            <Input
                                label="Experience"
                                name="experience"
                                value={form.experience}
                                onChange={handleChange}
                                placeholder="0-2 years"
                            />

                            <Input
                                label="Education"
                                name="education"
                                value={form.education}
                                onChange={handleChange}
                                placeholder="B.Tech / B.E. Computer Science"
                            />

                            <Input
                                label="Application URL"
                                name="applicationUrl"
                                value={form.applicationUrl}
                                onChange={handleChange}
                                placeholder="https://..."
                            />

                            <Input
                                label="Posted Date"
                                name="postedDate"
                                type="date"
                                value={form.postedDate}
                                onChange={handleChange}
                            />

                            <Input
                                label="Application Deadline"
                                name="applicationDeadline"
                                type="date"
                                value={form.applicationDeadline}
                                onChange={handleChange}
                            />

                        </div>

                        <div className="mt-5">

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Responsibilities
                            </label>

                            <textarea
                                name="extractedResponsibilities"
                                value={
                                    form.extractedResponsibilities
                                }
                                onChange={handleChange}
                                rows={4}
                                placeholder="Describe the main responsibilities..."
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-500"
                            />

                        </div>

                        <div className="mt-5">

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Requirements
                            </label>

                            <textarea
                                name="extractedRequirements"
                                value={
                                    form.extractedRequirements
                                }
                                onChange={handleChange}
                                rows={4}
                                placeholder="Describe the required qualifications..."
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-500"
                            />

                        </div>

                        <div className="mt-5">

                            <label className="mb-2 block text-sm font-medium text-slate-300">
                                Job Description *
                            </label>

                            <textarea
                                name="description"
                                value={form.description}
                                onChange={handleChange}
                                rows={8}
                                placeholder="Paste the complete job description here..."
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm leading-6 text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-500"
                            />

                        </div>

                        <div className="mt-6 flex flex-col justify-end gap-3 sm:flex-row">

                            {editingId && (
                                <button
                                    type="button"
                                    onClick={handleCancelEdit}
                                    disabled={saving}
                                    className="rounded-xl border border-slate-700 px-6 py-3 text-sm font-bold text-slate-300 transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    Cancel
                                </button>
                            )}

                            <button
                                type="submit"
                                disabled={saving}
                                className="rounded-xl bg-cyan-500 px-6 py-3 text-sm font-bold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {saving
                                    ? editingId
                                        ? 'Saving Changes...'
                                        : 'Analyzing...'
                                    : editingId
                                        ? 'Save Changes'
                                        : 'Add Job Description'}
                            </button>

                        </div>

                    </form>

                </section>

                {/* =================================================
                    JOB POOL
                ================================================= */}

                <section>

                    <div className="mb-5 flex items-center justify-between">

                        <div>

                            <h2 className="text-2xl font-bold">
                                Job Pool
                            </h2>

                            <p className="mt-1 text-sm text-slate-400">

                                {jobDescriptions.length}{' '}

                                {jobDescriptions.length === 1
                                    ? 'job'
                                    : 'jobs'}{' '}

                                available for matching

                            </p>

                        </div>

                        <button
                            onClick={loadJobDescriptions}
                            className="rounded-lg border border-slate-700 px-3 py-2 text-xs font-semibold text-slate-300 transition hover:bg-slate-800"
                        >
                            Refresh
                        </button>

                    </div>

                    {loading ? (

                        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-10 text-center text-slate-400">
                            Loading job descriptions...
                        </div>

                    ) : jobDescriptions.length === 0 ? (

                        <div className="rounded-2xl border border-dashed border-slate-700 bg-slate-900/40 p-10 text-center">

                            <h3 className="text-lg font-semibold text-slate-200">
                                No job descriptions yet
                            </h3>

                            <p className="mt-2 text-sm text-slate-500">
                                Add your first job above to start building
                                your job pool.
                            </p>

                        </div>

                    ) : (

                        <div className="grid gap-5 lg:grid-cols-2">

                            {jobDescriptions.map((job) => {

                                const skills =
                                    getSkills(job)

                                const isDeleting =
                                    deletingId === job.id

                                return (

                                    <article
                                        key={job.id}
                                        className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-lg transition hover:border-slate-700"
                                    >

                                        <div className="flex flex-col gap-4">

                                            {/* Header */}

                                            <div className="flex items-start justify-between gap-4">

                                                <div>

                                                    <h3 className="text-xl font-bold text-white">
                                                        {job.title}
                                                    </h3>

                                                    {job.companyName && (
                                                        <p className="mt-1 text-sm font-medium text-cyan-400">
                                                            {job.companyName}
                                                        </p>
                                                    )}

                                                </div>

                                                {job.id && (
                                                    <span className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1 text-xs text-slate-500">
                                                        #{job.id}
                                                    </span>
                                                )}

                                            </div>

                                            {/* Metadata */}

                                            <div className="flex flex-wrap gap-2">

                                                {job.location && (
                                                    <Badge>
                                                        📍 {job.location}
                                                    </Badge>
                                                )}

                                                {job.jobType && (
                                                    <Badge>
                                                        💼 {job.jobType}
                                                    </Badge>
                                                )}

                                                {job.workMode && (
                                                    <Badge>
                                                        🏠 {job.workMode}
                                                    </Badge>
                                                )}

                                                {job.experience && (
                                                    <Badge>
                                                        🎯 {job.experience}
                                                    </Badge>
                                                )}

                                                {job.salaryText ? (
                                                    <Badge>
                                                        💰 {job.salaryText}
                                                    </Badge>
                                                ) : job.salary ? (
                                                    <Badge>
                                                        💰 {job.salary}
                                                    </Badge>
                                                ) : null}

                                            </div>

                                            {/* Extracted Skills */}

                                            {skills.length > 0 && (

                                                <div>

                                                    <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
                                                        Extracted Skills
                                                    </p>

                                                    <div className="flex flex-wrap gap-2">

                                                        {skills.map(
                                                            (skill) => (
                                                                <span
                                                                    key={skill}
                                                                    className="rounded-lg border border-cyan-500/20 bg-cyan-500/10 px-2.5 py-1 text-xs font-medium text-cyan-300"
                                                                >
                                                                    {skill}
                                                                </span>
                                                            )
                                                        )}

                                                    </div>

                                                </div>

                                            )}

                                            {/* Description */}

                                            {job.description && (

                                                <p className="line-clamp-4 text-sm leading-6 text-slate-400">
                                                    {job.description}
                                                </p>

                                            )}

                                            {/* Actions */}

                                            <div className="flex flex-wrap items-center gap-3 border-t border-slate-800 pt-4">

                                                <Link
                                                    to={`/match?job=${job.id}`}
                                                    className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400"
                                                >
                                                    Analyze Match
                                                </Link>

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        handleEdit(job)
                                                    }
                                                    disabled={isDeleting}
                                                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:border-cyan-500/40 hover:bg-slate-800 hover:text-cyan-300 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    ✏️ Edit
                                                </button>

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        handleDelete(job)
                                                    }
                                                    disabled={isDeleting}
                                                    className="rounded-lg border border-red-500/20 bg-red-500/5 px-4 py-2 text-sm font-semibold text-red-300 transition hover:bg-red-500/10 disabled:cursor-not-allowed disabled:opacity-50"
                                                >
                                                    {isDeleting
                                                        ? 'Deleting...'
                                                        : '🗑️ Delete'}
                                                </button>

                                                {job.applicationUrl && (

                                                    <a
                                                        href={
                                                            job.applicationUrl
                                                        }
                                                        target="_blank"
                                                        rel="noreferrer"
                                                        className="rounded-lg border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:bg-slate-800"
                                                    >
                                                        Apply
                                                    </a>

                                                )}

                                            </div>

                                            {/* Dates */}

                                            {(job.postedDate ||
                                                job.applicationDeadline) && (

                                                <div className="flex flex-wrap gap-4 text-xs text-slate-500">

                                                    {job.postedDate && (
                                                        <span>
                                                            Posted:{' '}
                                                            {formatDate(
                                                                job.postedDate
                                                            )}
                                                        </span>
                                                    )}

                                                    {job.applicationDeadline && (
                                                        <span>
                                                            Deadline:{' '}
                                                            {formatDate(
                                                                job.applicationDeadline
                                                            )}
                                                        </span>
                                                    )}

                                                </div>

                                            )}

                                        </div>

                                    </article>

                                )
                            })}

                        </div>

                    )}

                </section>

            </div>

        </div>
    )
}

/*
 * =============================================================
 * INPUT COMPONENT
 * =============================================================
 */

function Input({
                   label,
                   name,
                   value,
                   onChange,
                   placeholder,
                   type = 'text',
               }) {

    return (

        <div>

            <label className="mb-2 block text-sm font-medium text-slate-300">
                {label}
            </label>

            <input
                type={type}
                name={name}
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                className="w-full rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-cyan-500"
            />

        </div>

    )
}

/*
 * =============================================================
 * BADGE COMPONENT
 * =============================================================
 */

function Badge({ children }) {

    return (

        <span className="rounded-lg border border-slate-700 bg-slate-950 px-2.5 py-1 text-xs text-slate-300">
            {children}
        </span>

    )
}

export default JobDescriptions