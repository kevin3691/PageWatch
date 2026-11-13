-- now() is constant inside a transaction. A wall-clock timestamp preserves the
-- order of successive snapshots and check events, including bulk operations.
alter table snapshot alter column created_at set default clock_timestamp();
alter table check_run alter column checked_at set default clock_timestamp();
