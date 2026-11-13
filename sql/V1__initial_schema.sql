create table monitor (
    id uuid primary key,
    name varchar(120) not null,
    url varchar(2048) not null,
    selector varchar(500) not null default '',
    ignore_selector varchar(500) not null default '',
    interval_minutes integer not null check (interval_minutes between 1 and 10080),
    active boolean not null default true,
    unread boolean not null default false,
    created_at timestamptz not null default now(),
    last_checked_at timestamptz,
    next_check_at timestamptz not null default now(),
    last_change_at timestamptz,
    last_error text
);
create table snapshot (
    id uuid primary key,
    monitor_id uuid not null references monitor(id) on delete cascade,
    previous_id uuid references snapshot(id) on delete set null,
    content text not null,
    content_hash varchar(64) not null,
    created_at timestamptz not null default now(),
    added_count integer not null default 0,
    removed_count integer not null default 0
);
create index snapshot_monitor_time on snapshot(monitor_id, created_at desc);
create table check_run (
    id uuid primary key,
    monitor_id uuid not null references monitor(id) on delete cascade,
    checked_at timestamptz not null default now(),
    outcome varchar(20) not null check (outcome in ('BASELINE', 'CHANGED', 'UNCHANGED', 'FAILED')),
    duration_ms bigint not null,
    error text
);
create index check_run_monitor_time on check_run(monitor_id, checked_at desc);
create index monitor_due on monitor(next_check_at) where active;
