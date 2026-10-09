import { useEffect, useMemo, useState } from 'react'
import { useAuth } from './auth/AuthContext'
import {
    User,
    Mail,
    Phone,
    MapPin,
    GraduationCap,
    Building2,
    CalendarDays,
    Code2,
    Pencil,
    Save,
    X,
    CheckCircle2,
    AlertCircle,
    Loader2,
    ShieldCheck,
    BriefcaseBusiness,
    Link2,
    GitBranch,
    Globe2,
} from 'lucide-react'

const API_BASE_URL = 'http://localhost:8080'

function Profile() {
    const {
        studentId,
        token,
        loading: authLoading,
    } = useAuth()

    const [student, setStudent] = useState(null)

    const [formData, setFormData] = useState({
        name: '',
        email: '',
        phone: '',
        location: '',
        college: '',
        branch: '',
        graduationYear: '',
        cgpa: '',
        preferredRole: '',
        preferredLocation: '',
        workMode: '',
        linkedinUrl: '',
        githubUrl: '',
        portfolioUrl: '',
    })

    const [editing, setEditing] = useState(false)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    useEffect(() => {
        if (authLoading || !studentId || !token) {
            return
        }

        loadProfile()
    }, [authLoading, studentId, token])

    async function loadProfile() {
        try {
            setLoading(true)
            setError('')
            setSuccess('')

            const response = await fetch(
                `${API_BASE_URL}/api/students/${studentId}`,
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            )

            if (!response.ok) {
                throw new Error(
                    'Failed to load student profile.'
                )
            }

            const data = await response.json()

            if (!data) {
                throw new Error(
                    'Student profile was not found.'
                )
            }

            setStudent(data)
            setFormData(createFormData(data))
        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to load your profile.'
            )
        } finally {
            setLoading(false)
        }
    }

    function createFormData(data) {
        return {
            name: data?.name || '',
            email: data?.email || '',
            phone: data?.phone || '',
            location: data?.location || '',
            college: data?.college || '',
            branch: data?.branch || '',
            graduationYear:
                data?.graduationYear || '',
            cgpa:
                data?.cgpa ?? '',
            preferredRole:
                data?.preferredRole || '',
            preferredLocation:
                data?.preferredLocation || '',
            workMode:
                data?.workMode || '',
            linkedinUrl:
                data?.linkedinUrl || '',
            githubUrl:
                data?.githubUrl || '',
            portfolioUrl:
                data?.portfolioUrl || '',
        }
    }

    function handleChange(event) {
        const { name, value } = event.target

        setFormData((previous) => ({
            ...previous,
            [name]: value,
        }))
    }

    function startEditing() {
        setSuccess('')
        setError('')

        if (student) {
            setFormData(createFormData(student))
        }

        setEditing(true)
    }

    function cancelEditing() {
        if (student) {
            setFormData(createFormData(student))
        }

        setEditing(false)
        setError('')
    }

    async function saveProfile(event) {
        event.preventDefault()

        if (!formData.name.trim()) {
            setError('Name is required.')
            return
        }

        if (!formData.email.trim()) {
            setError('Email is required.')
            return
        }

        try {
            setSaving(true)
            setError('')
            setSuccess('')

            const payload = {
                name: formData.name.trim(),

                email: formData.email.trim(),

                phone:
                    formData.phone.trim() ||
                    null,

                location:
                    formData.location.trim() ||
                    null,

                college:
                    formData.college.trim() ||
                    null,

                branch:
                    formData.branch.trim() ||
                    null,

                graduationYear:
                    formData.graduationYear
                        ? Number(
                            formData.graduationYear
                        )
                        : null,

                cgpa:
                    formData.cgpa !== '' &&
                    formData.cgpa !== null
                        ? Number(formData.cgpa)
                        : null,

                preferredRole:
                    formData.preferredRole.trim() ||
                    null,

                preferredLocation:
                    formData.preferredLocation.trim() ||
                    null,

                workMode:
                    formData.workMode.trim() ||
                    null,

                linkedinUrl:
                    formData.linkedinUrl.trim() ||
                    null,

                githubUrl:
                    formData.githubUrl.trim() ||
                    null,

                portfolioUrl:
                    formData.portfolioUrl.trim() ||
                    null,

                skills:
                    student?.skills || [],
            }

            const response = await fetch(
                `${API_BASE_URL}/api/students/${studentId}`,
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type':
                            'application/json',
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify(payload),
                }
            )

            if (!response.ok) {
                const message =
                    await response.text()

                throw new Error(
                    message ||
                    'Failed to save profile.'
                )
            }

            const updatedStudent =
                await response.json()

            setStudent(updatedStudent)

            setFormData(
                createFormData(updatedStudent)
            )

            setEditing(false)

            setSuccess(
                'Profile updated successfully.'
            )
        } catch (err) {
            console.error(err)

            setError(
                err.message ||
                'Unable to save your profile.'
            )
        } finally {
            setSaving(false)
        }
    }

    const skills = useMemo(() => {
        if (!student?.skills) {
            return []
        }

        return student.skills.filter(
            (skill) =>
                skill &&
                skill.name &&
                skill.name.trim()
        )
    }, [student])

    const completeness = useMemo(() => {
        if (!student) {
            return 0
        }

        const socialLinkCompleted =
            Boolean(
                student.linkedinUrl?.trim() ||
                student.githubUrl?.trim() ||
                student.portfolioUrl?.trim()
            )

        const checks = [
            Boolean(student.name?.trim()),
            Boolean(student.email?.trim()),
            Boolean(student.phone?.trim()),
            Boolean(student.location?.trim()),
            Boolean(student.college?.trim()),
            Boolean(student.branch?.trim()),
            Boolean(student.graduationYear),
            student.cgpa !== null &&
            student.cgpa !== undefined &&
            student.cgpa !== '',
            skills.length > 0,
            Boolean(student.preferredRole?.trim()),
            Boolean(student.preferredLocation?.trim()),
            Boolean(student.workMode?.trim()),
            socialLinkCompleted,
        ]

        const completed =
            checks.filter(Boolean).length

        return Math.round(
            (completed / checks.length) * 100
        )
    }, [student, skills])

    const careerPreferencesComplete =
        Boolean(
            student?.preferredRole?.trim()
        ) &&
        Boolean(
            student?.preferredLocation?.trim()
        ) &&
        Boolean(student?.workMode?.trim())

    const socialLinksComplete =
        Boolean(
            student?.linkedinUrl?.trim() ||
            student?.githubUrl?.trim() ||
            student?.portfolioUrl?.trim()
        )

    if (loading) {
        return (
            <div className="flex min-h-[70vh] items-center justify-center">
                <div className="text-center">
                    <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl border border-blue-500/20 bg-blue-500/10">
                        <Loader2 className="h-6 w-6 animate-spin text-blue-400" />
                    </div>

                    <p className="mt-4 text-sm text-slate-400">
                        Loading your profile...
                    </p>
                </div>
            </div>
        )
    }

    if (error && !student) {
        return (
            <div className="mx-auto max-w-5xl px-8 py-10">
                <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-6">
                    <div className="flex items-start gap-4">
                        <AlertCircle className="mt-0.5 h-6 w-6 shrink-0 text-red-400" />

                        <div>
                            <h2 className="text-lg font-semibold text-white">
                                Unable to load profile
                            </h2>

                            <p className="mt-2 text-sm text-slate-400">
                                {error}
                            </p>

                            <button
                                type="button"
                                onClick={loadProfile}
                                className="mt-5 rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-500"
                            >
                                Try Again
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        )
    }

    return (
        <div className="mx-auto max-w-6xl px-8 py-8">

            {/* HEADER */}
            <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
                <div>
                    <p className="text-sm font-medium uppercase tracking-wider text-cyan-400">
                        Student Profile
                    </p>

                    <h1 className="mt-2 text-4xl font-bold tracking-tight text-white">
                        Profile
                    </h1>

                    <p className="mt-2 max-w-2xl text-slate-400">
                        Manage your personal, academic and
                        career information used by Placement
                        Intelligence.
                    </p>
                </div>

                {!editing && (
                    <button
                        type="button"
                        onClick={startEditing}
                        className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-700 bg-slate-900/80 px-5 py-3 text-sm font-semibold text-white transition hover:border-blue-500/50 hover:bg-slate-800"
                    >
                        <Pencil className="h-4 w-4" />
                        Edit Profile
                    </button>
                )}
            </div>

            {/* SUCCESS */}
            {success && (
                <div className="mt-6 flex items-center gap-3 rounded-xl border border-emerald-500/20 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
                    <CheckCircle2 className="h-5 w-5" />
                    {success}
                </div>
            )}

            {/* ERROR */}
            {error && student && (
                <div className="mt-6 flex items-center gap-3 rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300">
                    <AlertCircle className="h-5 w-5" />
                    {error}
                </div>
            )}

            <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_320px]">

                {/* MAIN PROFILE */}
                <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6 shadow-xl shadow-black/10">

                    {/* PROFILE HEADER */}
                    <div className="flex items-center gap-4 border-b border-slate-800 pb-6">
                        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-500/10 ring-1 ring-blue-500/20">
                            <User className="h-8 w-8 text-blue-400" />
                        </div>

                        <div>
                            <h2 className="text-xl font-semibold text-white">
                                {student?.name ||
                                    'Student'}
                            </h2>

                            <p className="mt-1 text-sm text-slate-400">
                                {student?.branch ||
                                    'Branch not specified'}
                            </p>
                        </div>
                    </div>

                    {editing ? (
                        <form
                            onSubmit={saveProfile}
                            className="mt-6"
                        >

                            {/* PERSONAL */}
                            <SectionTitle
                                title="Personal Information"
                                subtitle="Basic information about you"
                            />

                            <div className="mt-5 grid gap-5 md:grid-cols-2">

                                <InputField
                                    icon={User}
                                    label="Full Name"
                                    name="name"
                                    value={
                                        formData.name
                                    }
                                    onChange={
                                        handleChange
                                    }
                                    required
                                />

                                <InputField
                                    icon={Mail}
                                    label="Email"
                                    name="email"
                                    type="email"
                                    value={
                                        formData.email
                                    }
                                    onChange={
                                        handleChange
                                    }
                                    required
                                />

                                <InputField
                                    icon={Phone}
                                    label="Phone"
                                    name="phone"
                                    value={
                                        formData.phone
                                    }
                                    onChange={
                                        handleChange
                                    }
                                />

                                <InputField
                                    icon={MapPin}
                                    label="Current Location"
                                    name="location"
                                    value={
                                        formData.location
                                    }
                                    onChange={
                                        handleChange
                                    }
                                />
                            </div>

                            {/* ACADEMIC */}
                            <div className="mt-10">
                                <SectionTitle
                                    title="Academic Information"
                                    subtitle="Your education details"
                                />

                                <div className="mt-5 grid gap-5 md:grid-cols-2">

                                    <InputField
                                        icon={Building2}
                                        label="College / University"
                                        name="college"
                                        value={
                                            formData.college
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <InputField
                                        icon={
                                            GraduationCap
                                        }
                                        label="Branch"
                                        name="branch"
                                        value={
                                            formData.branch
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <InputField
                                        icon={
                                            CalendarDays
                                        }
                                        label="Graduation Year"
                                        name="graduationYear"
                                        type="number"
                                        value={
                                            formData.graduationYear
                                        }
                                        onChange={
                                            handleChange
                                        }
                                        min="2000"
                                        max="2100"
                                    />

                                    <InputField
                                        icon={
                                            GraduationCap
                                        }
                                        label="CGPA"
                                        name="cgpa"
                                        type="number"
                                        value={
                                            formData.cgpa
                                        }
                                        onChange={
                                            handleChange
                                        }
                                        min="0"
                                        max="10"
                                        step="0.01"
                                    />
                                </div>
                            </div>

                            {/* CAREER */}
                            <div className="mt-10">
                                <SectionTitle
                                    title="Career Preferences"
                                    subtitle="Tell the system what kind of opportunities you want"
                                />

                                <div className="mt-5 grid gap-5 md:grid-cols-2">

                                    <InputField
                                        icon={
                                            BriefcaseBusiness
                                        }
                                        label="Preferred Role"
                                        name="preferredRole"
                                        placeholder="e.g. Software Engineer"
                                        value={
                                            formData.preferredRole
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <InputField
                                        icon={MapPin}
                                        label="Preferred Location"
                                        name="preferredLocation"
                                        placeholder="e.g. Hyderabad / Bangalore"
                                        value={
                                            formData.preferredLocation
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <SelectField
                                        icon={
                                            BriefcaseBusiness
                                        }
                                        label="Preferred Work Mode"
                                        name="workMode"
                                        value={
                                            formData.workMode
                                        }
                                        onChange={
                                            handleChange
                                        }
                                        options={[
                                            {
                                                value: '',
                                                label: 'Select work mode',
                                            },
                                            {
                                                value: 'REMOTE',
                                                label: 'Remote',
                                            },
                                            {
                                                value: 'HYBRID',
                                                label: 'Hybrid',
                                            },
                                            {
                                                value: 'ONSITE',
                                                label: 'On-site',
                                            },
                                        ]}
                                    />
                                </div>
                            </div>

                            {/* SOCIAL */}
                            <div className="mt-10">
                                <SectionTitle
                                    title="Social & Portfolio"
                                    subtitle="Add professional links that recruiters can use"
                                />

                                <div className="mt-5 grid gap-5">

                                    <InputField
                                        icon={Link2}
                                        label="LinkedIn URL"
                                        name="linkedinUrl"
                                        type="url"
                                        placeholder="https://linkedin.com/in/your-profile"
                                        value={
                                            formData.linkedinUrl
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <InputField
                                        icon={GitBranch}
                                        label="GitHub URL"
                                        name="githubUrl"
                                        type="url"
                                        placeholder="https://github.com/your-username"
                                        value={
                                            formData.githubUrl
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />

                                    <InputField
                                        icon={Globe2}
                                        label="Portfolio URL"
                                        name="portfolioUrl"
                                        type="url"
                                        placeholder="https://yourportfolio.com"
                                        value={
                                            formData.portfolioUrl
                                        }
                                        onChange={
                                            handleChange
                                        }
                                    />
                                </div>
                            </div>

                            {/* SKILLS */}
                            <div className="mt-10">

                                <div className="mb-3 flex items-center gap-2">
                                    <Code2 className="h-4 w-4 text-blue-400" />

                                    <label className="text-sm font-medium text-slate-300">
                                        Current Skills
                                    </label>
                                </div>

                                {skills.length > 0 ? (
                                    <div className="flex flex-wrap gap-2">
                                        {skills.map(
                                            (skill) => (
                                                <span
                                                    key={
                                                        skill.id ??
                                                        skill.name
                                                    }
                                                    className="rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1.5 text-sm text-blue-300"
                                                >
                                                    {
                                                        skill.name
                                                    }
                                                </span>
                                            )
                                        )}
                                    </div>
                                ) : (
                                    <p className="rounded-xl border border-dashed border-slate-700 px-4 py-4 text-sm text-slate-500">
                                        No skills have been
                                        added yet.
                                    </p>
                                )}

                                <p className="mt-3 text-xs text-slate-500">
                                    Skills are currently managed
                                    through the resume/skill system.
                                </p>
                            </div>

                            {/* SAVE BUTTONS */}
                            <div className="mt-8 flex flex-wrap gap-3 border-t border-slate-800 pt-6">

                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
                                >
                                    {saving ? (
                                        <>
                                            <Loader2 className="h-4 w-4 animate-spin" />
                                            Saving...
                                        </>
                                    ) : (
                                        <>
                                            <Save className="h-4 w-4" />
                                            Save Profile
                                        </>
                                    )}
                                </button>

                                <button
                                    type="button"
                                    onClick={
                                        cancelEditing
                                    }
                                    disabled={saving}
                                    className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-300 transition hover:bg-slate-800 disabled:opacity-60"
                                >
                                    <X className="h-4 w-4" />
                                    Cancel
                                </button>

                            </div>
                        </form>
                    ) : (
                        <div className="mt-6">

                            {/* PERSONAL */}
                            <SectionTitle
                                title="Personal Information"
                                subtitle="Your basic details"
                            />

                            <div className="mt-4 grid gap-4 md:grid-cols-2">

                                <InfoCard
                                    icon={User}
                                    label="Full Name"
                                    value={
                                        student?.name
                                    }
                                />

                                <InfoCard
                                    icon={Mail}
                                    label="Email"
                                    value={
                                        student?.email
                                    }
                                />

                                <InfoCard
                                    icon={Phone}
                                    label="Phone"
                                    value={
                                        student?.phone
                                    }
                                />

                                <InfoCard
                                    icon={MapPin}
                                    label="Location"
                                    value={
                                        student?.location
                                    }
                                />
                            </div>

                            {/* ACADEMIC */}
                            <div className="mt-8">

                                <SectionTitle
                                    title="Academic Information"
                                    subtitle="Your education details"
                                />

                                <div className="mt-4 grid gap-4 md:grid-cols-2">

                                    <InfoCard
                                        icon={Building2}
                                        label="College / University"
                                        value={
                                            student?.college
                                        }
                                    />

                                    <InfoCard
                                        icon={
                                            GraduationCap
                                        }
                                        label="Branch"
                                        value={
                                            student?.branch
                                        }
                                    />

                                    <InfoCard
                                        icon={
                                            CalendarDays
                                        }
                                        label="Graduation Year"
                                        value={
                                            student?.graduationYear
                                        }
                                    />

                                    <InfoCard
                                        icon={
                                            GraduationCap
                                        }
                                        label="CGPA"
                                        value={
                                            student?.cgpa
                                        }
                                    />
                                </div>
                            </div>

                            {/* CAREER */}
                            <div className="mt-8">

                                <SectionTitle
                                    title="Career Preferences"
                                    subtitle="Your preferred opportunities"
                                />

                                <div className="mt-4 grid gap-4 md:grid-cols-2">

                                    <InfoCard
                                        icon={
                                            BriefcaseBusiness
                                        }
                                        label="Preferred Role"
                                        value={
                                            student?.preferredRole
                                        }
                                    />

                                    <InfoCard
                                        icon={MapPin}
                                        label="Preferred Location"
                                        value={
                                            student?.preferredLocation
                                        }
                                    />

                                    <InfoCard
                                        icon={
                                            BriefcaseBusiness
                                        }
                                        label="Work Mode"
                                        value={
                                            formatWorkMode(
                                                student?.workMode
                                            )
                                        }
                                    />
                                </div>
                            </div>

                            {/* SKILLS */}
                            <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-900/40 p-5">

                                <div className="flex items-center gap-2">
                                    <Code2 className="h-5 w-5 text-blue-400" />

                                    <h3 className="font-semibold text-white">
                                        Skills
                                    </h3>
                                </div>

                                {skills.length > 0 ? (
                                    <div className="mt-4 flex flex-wrap gap-2">
                                        {skills.map(
                                            (skill) => (
                                                <span
                                                    key={
                                                        skill.id ??
                                                        skill.name
                                                    }
                                                    className="rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1.5 text-sm text-blue-300"
                                                >
                                                    {
                                                        skill.name
                                                    }
                                                </span>
                                            )
                                        )}
                                    </div>
                                ) : (
                                    <p className="mt-3 text-sm text-slate-500">
                                        No skills added yet.
                                    </p>
                                )}
                            </div>

                            {/* LINKS */}
                            <div className="mt-8">

                                <SectionTitle
                                    title="Social & Portfolio"
                                    subtitle="Professional links"
                                />

                                <div className="mt-4 grid gap-4 md:grid-cols-2">

                                    <LinkCard
                                        icon={Link2}
                                        label="LinkedIn"
                                        value={
                                            student?.linkedinUrl
                                        }
                                    />

                                    <LinkCard
                                        icon={GitBranch}
                                        label="GitHub"
                                        value={
                                            student?.githubUrl
                                        }
                                    />

                                    <LinkCard
                                        icon={Globe2}
                                        label="Portfolio"
                                        value={
                                            student?.portfolioUrl
                                        }
                                    />

                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* RIGHT SIDE */}
                <div className="space-y-6">

                    {/* COMPLETENESS */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6 shadow-xl shadow-black/10">

                        <div className="flex items-start justify-between">

                            <div>
                                <p className="text-sm text-slate-400">
                                    Profile Completeness
                                </p>

                                <p className="mt-2 text-4xl font-bold text-white">
                                    {completeness}%
                                </p>
                            </div>

                            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-500/10">
                                <ShieldCheck className="h-6 w-6 text-emerald-400" />
                            </div>

                        </div>

                        <div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-800">
                            <div
                                className="h-full rounded-full bg-blue-500 transition-all duration-500"
                                style={{
                                    width: `${completeness}%`,
                                }}
                            />
                        </div>

                        <p className="mt-4 text-xs leading-5 text-slate-500">
                            A complete profile gives the placement
                            system more information for job matching,
                            recommendations and skill-gap analysis.
                        </p>
                    </div>

                    {/* CONNECTION */}
                    <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-6">

                        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/10">
                            <CheckCircle2 className="h-5 w-5 text-blue-400" />
                        </div>

                        <h3 className="mt-4 font-semibold text-white">
                            Connected to Placement Intelligence
                        </h3>

                        <p className="mt-2 text-sm leading-6 text-slate-400">
                            Your profile information is connected
                            to the student record used by the
                            placement matching system.
                        </p>

                        <div className="mt-4 rounded-xl border border-slate-800 bg-slate-950/60 px-4 py-3">

                            <p className="text-xs uppercase tracking-wide text-slate-500">
                                Student ID
                            </p>

                            <p className="mt-1 font-mono text-sm text-slate-300">
                                #{studentId}
                            </p>

                        </div>
                    </div>

                    {/* ROADMAP */}
                    <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-6">

                        <h3 className="font-semibold text-white">
                            Profile Roadmap
                        </h3>

                        <p className="mt-1 text-xs text-slate-500">
                            Complete your profile step by step.
                        </p>

                        <div className="mt-5 space-y-4">

                            <RoadmapItem
                                completed={
                                    Boolean(
                                        student?.name &&
                                        student?.email
                                    )
                                }
                                text="Basic information"
                            />

                            <RoadmapItem
                                completed={
                                    Boolean(
                                        student?.college &&
                                        student?.branch &&
                                        student?.graduationYear
                                    )
                                }
                                text="Academic information"
                            />

                            <RoadmapItem
                                completed={
                                    skills.length > 0
                                }
                                text="Skills"
                            />

                            <RoadmapItem
                                completed={
                                    careerPreferencesComplete
                                }
                                text="Career preferences"
                            />

                            <RoadmapItem
                                completed={
                                    socialLinksComplete
                                }
                                text="Social & portfolio links"
                            />

                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

/* ============================================================
   SECTION TITLE
============================================================ */

function SectionTitle({
                          title,
                          subtitle,
                      }) {
    return (
        <div>
            <h3 className="text-base font-semibold text-white">
                {title}
            </h3>

            <p className="mt-1 text-xs text-slate-500">
                {subtitle}
            </p>
        </div>
    )
}

/* ============================================================
   INPUT FIELD
============================================================ */

function InputField({
                        icon: Icon,
                        label,
                        name,
                        type = 'text',
                        value,
                        onChange,
                        required = false,
                        min,
                        max,
                        step,
                        placeholder,
                    }) {
    return (
        <div>
            <label
                htmlFor={name}
                className="mb-2 block text-sm font-medium text-slate-300"
            >
                {label}

                {required && (
                    <span className="ml-1 text-red-400">
                        *
                    </span>
                )}
            </label>

            <div className="relative">

                <Icon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />

                <input
                    id={name}
                    name={name}
                    type={type}
                    value={value ?? ''}
                    onChange={onChange}
                    required={required}
                    min={min}
                    max={max}
                    step={step}
                    placeholder={placeholder}
                    className="w-full rounded-xl border border-slate-700 bg-slate-900/80 py-3 pl-11 pr-4 text-sm text-white outline-none transition placeholder:text-slate-600 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                />

            </div>
        </div>
    )
}

/* ============================================================
   SELECT FIELD
============================================================ */

function SelectField({
                         icon: Icon,
                         label,
                         name,
                         value,
                         onChange,
                         options,
                     }) {
    return (
        <div>

            <label
                htmlFor={name}
                className="mb-2 block text-sm font-medium text-slate-300"
            >
                {label}
            </label>

            <div className="relative">

                <Icon className="pointer-events-none absolute left-4 top-1/2 z-10 h-4 w-4 -translate-y-1/2 text-slate-500" />

                <select
                    id={name}
                    name={name}
                    value={value ?? ''}
                    onChange={onChange}
                    className="w-full appearance-none rounded-xl border border-slate-700 bg-slate-900/80 py-3 pl-11 pr-4 text-sm text-white outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500/10"
                >
                    {options.map(
                        (option) => (
                            <option
                                key={
                                    option.value ||
                                    option.label
                                }
                                value={
                                    option.value
                                }
                                className="bg-slate-900"
                            >
                                {
                                    option.label
                                }
                            </option>
                        )
                    )}
                </select>

            </div>
        </div>
    )
}

/* ============================================================
   INFO CARD
============================================================ */

function InfoCard({
                      icon: Icon,
                      label,
                      value,
                  }) {
    const displayValue =
        value !== null &&
        value !== undefined &&
        String(value).trim()
            ? String(value)
            : 'Not specified'

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">

            <div className="flex items-center gap-3">

                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-800">
                    <Icon className="h-4 w-4 text-slate-400" />
                </div>

                <div className="min-w-0">

                    <p className="text-xs uppercase tracking-wide text-slate-500">
                        {label}
                    </p>

                    <p className="mt-1 truncate text-sm font-medium text-slate-200">
                        {displayValue}
                    </p>

                </div>
            </div>
        </div>
    )
}

/* ============================================================
   LINK CARD
============================================================ */

function LinkCard({
                      icon: Icon,
                      label,
                      value,
                  }) {
    const hasValue =
        value &&
        String(value).trim()

    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">

            <div className="flex items-center gap-3">

                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-800">
                    <Icon className="h-4 w-4 text-slate-400" />
                </div>

                <div className="min-w-0 flex-1">

                    <p className="text-xs uppercase tracking-wide text-slate-500">
                        {label}
                    </p>

                    {hasValue ? (
                        <a
                            href={value}
                            target="_blank"
                            rel="noreferrer"
                            className="mt-1 block truncate text-sm font-medium text-blue-400 hover:text-blue-300"
                        >
                            {value}
                        </a>
                    ) : (
                        <p className="mt-1 text-sm font-medium text-slate-500">
                            Not specified
                        </p>
                    )}

                </div>
            </div>
        </div>
    )
}

/* ============================================================
   ROADMAP ITEM
============================================================ */

function RoadmapItem({
                         completed,
                         text,
                     }) {
    return (
        <div className="flex items-center gap-3">

            <div
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${
                    completed
                        ? 'bg-emerald-500/10'
                        : 'bg-slate-800'
                }`}
            >
                {completed ? (
                    <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                ) : (
                    <div className="h-2 w-2 rounded-full bg-slate-600" />
                )}
            </div>

            <span
                className={`text-sm ${
                    completed
                        ? 'text-slate-300'
                        : 'text-slate-500'
                }`}
            >
                {text}
            </span>

        </div>
    )
}

/* ============================================================
   WORK MODE FORMATTER
============================================================ */

function formatWorkMode(value) {
    if (!value) {
        return ''
    }

    const normalized =
        String(value).toUpperCase()

    if (normalized === 'REMOTE') {
        return 'Remote'
    }

    if (normalized === 'HYBRID') {
        return 'Hybrid'
    }

    if (normalized === 'ONSITE') {
        return 'On-site'
    }

    return value
}

export default Profile