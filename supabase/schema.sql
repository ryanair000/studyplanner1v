create extension if not exists pgcrypto;

create table if not exists public.courses (
    id uuid primary key default gen_random_uuid(),
    owner_user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    name text not null check (char_length(trim(name)) > 0),
    class_day_of_week integer,
    class_start_minute_of_day integer,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.assignments (
    id uuid primary key default gen_random_uuid(),
    owner_user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    course_id uuid not null references public.courses (id) on delete cascade,
    title text not null check (char_length(trim(title)) > 0),
    due_date bigint not null,
    is_completed boolean not null default false,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists public.exams (
    id uuid primary key default gen_random_uuid(),
    owner_user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    course_id uuid not null references public.courses (id) on delete cascade,
    title text not null check (char_length(trim(title)) > 0),
    date bigint not null,
    type text not null check (char_length(trim(type)) > 0),
    created_at timestamptz not null default timezone('utc', now())
);

alter table public.courses
    add column if not exists class_day_of_week integer,
    add column if not exists class_start_minute_of_day integer;

alter table public.courses
    drop constraint if exists courses_class_schedule_check;

alter table public.courses
    add constraint courses_class_schedule_check
    check (
        (class_day_of_week is null and class_start_minute_of_day is null)
        or (
            class_day_of_week between 1 and 7
            and class_start_minute_of_day between 0 and 1439
        )
    );

create index if not exists courses_owner_user_id_idx on public.courses (owner_user_id);
create index if not exists assignments_owner_user_id_idx on public.assignments (owner_user_id);
create index if not exists assignments_course_id_idx on public.assignments (course_id);
create index if not exists exams_owner_user_id_idx on public.exams (owner_user_id);
create index if not exists exams_course_id_idx on public.exams (course_id);

grant select, insert, update, delete on public.courses to authenticated;
grant select, insert, update, delete on public.assignments to authenticated;
grant select, insert, update, delete on public.exams to authenticated;

alter table public.courses enable row level security;
alter table public.assignments enable row level security;
alter table public.exams enable row level security;

drop policy if exists "Users manage their own courses" on public.courses;
create policy "Users manage their own courses"
on public.courses
for all
to authenticated
using (auth.uid() = owner_user_id)
with check (auth.uid() = owner_user_id);

drop policy if exists "Users manage their own assignments" on public.assignments;
create policy "Users manage their own assignments"
on public.assignments
for all
to authenticated
using (auth.uid() = owner_user_id)
with check (auth.uid() = owner_user_id);

drop policy if exists "Users manage their own exams" on public.exams;
create policy "Users manage their own exams"
on public.exams
for all
to authenticated
using (auth.uid() = owner_user_id)
with check (auth.uid() = owner_user_id);
