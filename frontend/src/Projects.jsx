import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

const API_BASE_URL = 'http://localhost:8080'
const STUDENT_ID = 2

function SectionCard({ title, children }) {
    return (
        <section className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-bold text-white">
                {title}
            </h2>

            <div className="mt-5">
                {children}
            </div>
        </section>
    )
}

function Tag({ children, type = 'blue' }) {
    const styles = {
        blue: 'border-blue-500/30 bg-blue-500/10 text-blue-400',
        green: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400',
        orange: 'border-orange-500/30 bg-orange-500/10 text-orange-400',
        purple: 'border-purple-500/30 bg-purple-500/10 text-purple-400',
    }

    return (
        <span
            className={`inline-flex rounded-full border px-3 py-1.5 text-sm font-medium ${styles[type] || styles.blue}`}
        >
            {children}
        </span>
    )
}

function Projects() {
    const [jobs, setJobs] = useState([])
    const [selectedJobId, setSelectedJobId] = useState('')
    const [result, setResult] = useState(null)

    const [loadingJobs, setLoadingJobs] = useState(true)
    const [loadingRecommendation, setLoadingRecommendation] =
        useState(false)

    const [error, setError] = useState('')

    useEffect(() => {
        loadJobs()
    }, [])

    async function loadJobs() {
        try {
            setLoadingJobs(true)
            setError('')

            const response = await fetch(
                `${API_BASE_URL}/api/job-descriptions`
            )

            if (!response.ok) {
                throw new Error(
                    'Unable to load job descriptions'
                )
            }

            const data = await response.json()

            const jobList = Array.isArray(data) ? data : []

            setJobs(jobList)

            if (jobList.length > 0) {
                setSelectedJobId(String(jobList[0].id))
            }
        } catch (err) {
            console.error(err)

            setError(
                'Could not load jobs. Make sure the Spring Boot server is running on port 8080.'
            )
        } finally {
            setLoadingJobs(false)
        }
    }

    async function generateRecommendation() {
        if (!selectedJobId) {
            setError('Please select a target job first.')
            return
        }

        try {
            setLoadingRecommendation(true)
            setError('')
            setResult(null)

            const response = await fetch(
                `${API_BASE_URL}/api/project-recommendations/student/${STUDENT_ID}/job/${selectedJobId}`
            )

            if (!response.ok) {
                const message = await response.text()

                throw new Error(
                    message ||
                    'Unable to generate project recommendation'
                )
            }

            const data = await response.json()

            setResult(data)
        } catch (err) {
            console.error(err)

            setError(
                'Unable to generate the project recommendation. Check that the selected job and student profile exist.'
            )
        } finally {
            setLoadingRecommendation(false)
        }
    }

    const recommendation =
        result?.recommendation || null

    const matchedSkills =
        result?.matchedSkills || []

    const missingSkills =
        result?.missingSkills ||
        recommendation?.missingSkills ||
        []

    const requiredSkills =
        result?.requiredSkills || []

    const technologies =
        recommendation?.technologies || []

    const features =
        recommendation?.features || []

    const skillsDemonstrated =
        recommendation?.skillsDemonstrated || []

    const buildSteps =
        recommendation?.buildSteps || []

    const selectedJob =
        jobs.find(
            (job) =>
                String(job.id) ===
                String(selectedJobId)
        )

    return (
        <div className="space-y-8">
            {/* Header */}
            <section>
                <p className="text-sm font-semibold uppercase tracking-wider text-blue-400">
                    Placement Intelligence
                </p>

                <h1 className="mt-2 text-4xl font-bold tracking-tight text-white">
                    Project Recommendations
                </h1>

                <p className="mt-3 max-w-3xl text-base leading-7 text-slate-400">
                    Get a project recommendation designed around
                    your target role and current skill gaps. Build
                    something practical that strengthens your resume
                    evidence.
                </p>
            </section>

            {/* Job selector */}
            <section className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6">
                <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
                    <div className="flex-1">
                        <label
                            htmlFor="target-job"
                            className="text-sm font-medium text-slate-300"
                        >
                            Select a target job
                        </label>

                        <select
                            id="target-job"
                            value={selectedJobId}
                            onChange={(event) => {
                                setSelectedJobId(
                                    event.target.value
                                )
                                setResult(null)
                                setError('')
                            }}
                            disabled={loadingJobs}
                            className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-900 px-4 py-3 text-white outline-none transition focus:border-blue-500"
                        >
                            {loadingJobs ? (
                                <option value="">
                                    Loading jobs...
                                </option>
                            ) : jobs.length === 0 ? (
                                <option value="">
                                    No jobs available
                                </option>
                            ) : (
                                jobs.map((job) => (
                                    <option
                                        key={job.id}
                                        value={job.id}
                                    >
                                        {job.title}
                                    </option>
                                ))
                            )}
                        </select>

                        {selectedJob && (
                            <p className="mt-2 text-sm text-slate-500">
                                Recommendation will be tailored
                                to{' '}
                                <span className="text-slate-300">
                                    {selectedJob.title}
                                </span>
                                .
                            </p>
                        )}
                    </div>

                    <button
                        type="button"
                        onClick={generateRecommendation}
                        disabled={
                            loadingRecommendation ||
                            loadingJobs ||
                            !selectedJobId
                        }
                        className="rounded-xl bg-blue-600 px-6 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {loadingRecommendation
                            ? 'Generating...'
                            : 'Generate Recommendation'}
                    </button>
                </div>
            </section>

            {/* Error */}
            {error && (
                <div className="rounded-xl border border-red-500/30 bg-red-500/10 px-5 py-4 text-sm text-red-400">
                    {error}
                </div>
            )}

            {/* Empty state */}
            {!result && !loadingRecommendation && !error && (
                <section className="rounded-2xl border border-dashed border-slate-700 bg-slate-950/40 p-10 text-center">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-500/10 text-2xl">
                        💡
                    </div>

                    <h2 className="mt-5 text-2xl font-bold text-white">
                        Turn your skill gaps into a project
                    </h2>

                    <p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500">
                        Select a target job and generate a
                        project designed to improve your role-specific
                        preparation and resume evidence.
                    </p>
                </section>
            )}

            {/* Loading */}
            {loadingRecommendation && (
                <section className="rounded-2xl border border-slate-800 bg-slate-950/70 p-10 text-center">
                    <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-slate-700 border-t-blue-500" />

                    <p className="mt-5 text-sm text-slate-400">
                        Analyzing your skills and generating the
                        best project for this role...
                    </p>
                </section>
            )}

            {/* Recommendation */}
            {recommendation && !loadingRecommendation && (
                <>
                    {/* Main project */}
                    <section className="overflow-hidden rounded-2xl border border-blue-500/20 bg-gradient-to-br from-blue-500/10 via-slate-950/80 to-slate-950/80">
                        <div className="p-7">
                            <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                                <div>
                                    <div className="flex flex-wrap gap-2">
                                        {recommendation.priority && (
                                            <Tag type="orange">
                                                {recommendation.priority}{' '}
                                                PRIORITY
                                            </Tag>
                                        )}

                                        {recommendation.difficulty && (
                                            <Tag type="purple">
                                                {recommendation.difficulty}
                                            </Tag>
                                        )}

                                        {recommendation.roleFit && (
                                            <Tag type="green">
                                                {recommendation.roleFit}{' '}
                                                role fit
                                            </Tag>
                                        )}
                                    </div>

                                    <h2 className="mt-5 text-3xl font-bold text-white">
                                        {recommendation.title}
                                    </h2>

                                    <p className="mt-3 max-w-3xl text-base leading-7 text-slate-400">
                                        {recommendation.description}
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Why */}
                        <div className="border-t border-slate-800/80 p-7">
                            <div className="grid gap-6 lg:grid-cols-2">
                                <div>
                                    <p className="text-sm font-semibold uppercase tracking-wider text-blue-400">
                                        Why this project?
                                    </p>

                                    <p className="mt-2 text-sm leading-6 text-slate-300">
                                        {recommendation.whyRecommended ||
                                            'This project was selected based on your target role and current profile.'}
                                    </p>
                                </div>

                                <div>
                                    <p className="text-sm font-semibold uppercase tracking-wider text-emerald-400">
                                        Skill-gap closure
                                    </p>

                                    <p className="mt-2 text-sm leading-6 text-slate-300">
                                        {recommendation.gapClosure ||
                                            'This project helps strengthen the skills required by the target role.'}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Skill comparison */}
                    <div className="grid gap-6 lg:grid-cols-2">
                        <SectionCard title="Skills you already match">
                            {matchedSkills.length > 0 ? (
                                <div className="flex flex-wrap gap-2">
                                    {matchedSkills.map(
                                        (skill, index) => (
                                            <Tag
                                                key={`${skill}-${index}`}
                                                type="green"
                                            >
                                                ✓ {skill}
                                            </Tag>
                                        )
                                    )}
                                </div>
                            ) : (
                                <p className="text-sm text-slate-500">
                                    No matched skills were returned.
                                </p>
                            )}
                        </SectionCard>

                        <SectionCard title="Skills this project should develop">
                            {missingSkills.length > 0 ? (
                                <div className="flex flex-wrap gap-2">
                                    {missingSkills.map(
                                        (skill, index) => (
                                            <Tag
                                                key={`${skill}-${index}`}
                                                type="orange"
                                            >
                                                ! {skill}
                                            </Tag>
                                        )
                                    )}
                                </div>
                            ) : (
                                <p className="text-sm text-emerald-400">
                                    No major skill gaps detected.
                                </p>
                            )}
                        </SectionCard>
                    </div>

                    {/* Required skills */}
                    {requiredSkills.length > 0 && (
                        <SectionCard title="Target Job Requirements">
                            <div className="flex flex-wrap gap-2">
                                {requiredSkills.map(
                                    (skill, index) => {
                                        const isMissing =
                                            missingSkills.some(
                                                (missing) =>
                                                    String(
                                                        missing
                                                    ).toLowerCase() ===
                                                    String(
                                                        skill
                                                    ).toLowerCase()
                                            )

                                        return (
                                            <Tag
                                                key={`${skill}-${index}`}
                                                type={
                                                    isMissing
                                                        ? 'orange'
                                                        : 'green'
                                                }
                                            >
                                                {isMissing
                                                    ? '!'
                                                    : '✓'}{' '}
                                                {skill}
                                            </Tag>
                                        )
                                    }
                                )}
                            </div>
                        </SectionCard>
                    )}

                    {/* Technologies */}
                    <SectionCard title="Technologies to Use">
                        {technologies.length > 0 ? (
                            <div className="flex flex-wrap gap-3">
                                {technologies.map(
                                    (technology, index) => (
                                        <Tag
                                            key={`${technology}-${index}`}
                                            type="blue"
                                        >
                                            {technology}
                                        </Tag>
                                    )
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">
                                No technologies were returned.
                            </p>
                        )}
                    </SectionCard>

                    {/* Features */}
                    <SectionCard title="Recommended Features">
                        {features.length > 0 ? (
                            <div className="grid gap-3 md:grid-cols-2">
                                {features.map(
                                    (feature, index) => (
                                        <div
                                            key={`${feature}-${index}`}
                                            className="flex gap-3 rounded-xl bg-slate-900 p-4"
                                        >
                                            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-blue-500/10 text-sm font-bold text-blue-400">
                                                {index + 1}
                                            </span>

                                            <p className="text-sm leading-6 text-slate-300">
                                                {feature}
                                            </p>
                                        </div>
                                    )
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">
                                No features were returned.
                            </p>
                        )}
                    </SectionCard>

                    {/* Skills demonstrated */}
                    <SectionCard title="Skills Demonstrated">
                        {skillsDemonstrated.length > 0 ? (
                            <div className="flex flex-wrap gap-2">
                                {skillsDemonstrated.map(
                                    (skill, index) => (
                                        <Tag
                                            key={`${skill}-${index}`}
                                            type="purple"
                                        >
                                            {skill}
                                        </Tag>
                                    )
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">
                                No demonstrated skills were returned.
                            </p>
                        )}
                    </SectionCard>

                    {/* Build steps */}
                    <SectionCard title="Suggested Build Plan">
                        {buildSteps.length > 0 ? (
                            <div className="space-y-3">
                                {buildSteps.map(
                                    (step, index) => (
                                        <div
                                            key={`${step}-${index}`}
                                            className="flex gap-4 rounded-xl bg-slate-900 p-4"
                                        >
                                            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-500/10 text-sm font-bold text-blue-400">
                                                {index + 1}
                                            </div>

                                            <div>
                                                <p className="font-medium text-white">
                                                    Step {index + 1}
                                                </p>

                                                <p className="mt-1 text-sm leading-6 text-slate-400">
                                                    {step}
                                                </p>
                                            </div>
                                        </div>
                                    )
                                )}
                            </div>
                        ) : (
                            <p className="text-sm text-slate-500">
                                No build steps were returned.
                            </p>
                        )}
                    </SectionCard>

                    {/* Resume value */}
                    <section className="rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-7">
                        <p className="text-sm font-semibold uppercase tracking-wider text-emerald-400">
                            Resume Value
                        </p>

                        <h2 className="mt-2 text-2xl font-bold text-white">
                            Turn the project into resume evidence
                        </h2>

                        {recommendation.resumeValue && (
                            <p className="mt-3 text-sm leading-6 text-slate-400">
                                {recommendation.resumeValue}
                            </p>
                        )}

                        {recommendation.resumeBullet && (
                            <div className="mt-5 rounded-xl border border-slate-800 bg-slate-950/80 p-5">
                                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                                    Suggested Resume Bullet
                                </p>

                                <p className="mt-3 text-sm leading-7 text-slate-300">
                                    {recommendation.resumeBullet}
                                </p>
                            </div>
                        )}
                    </section>

                    {/* Navigation */}
                    <section className="flex flex-col gap-4 rounded-2xl border border-slate-800 bg-slate-950/50 p-6 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                            <p className="text-sm font-semibold text-blue-400">
                                Continue your preparation
                            </p>

                            <h2 className="mt-1 text-lg font-bold text-white">
                                Work on the skills this project targets.
                            </h2>
                        </div>

                        <Link
                            to="/skill-gaps"
                            className="rounded-xl border border-blue-500/30 bg-blue-500/10 px-5 py-3 text-center font-semibold text-blue-400 transition hover:bg-blue-500/20"
                        >
                            View Skill Gaps →
                        </Link>
                    </section>
                </>
            )}
        </div>
    )
}

export default Projects