-- GKFXL Automate cloud schema. Apply in Supabase SQL Editor.
create table if not exists public.automation_rules (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  payload jsonb not null,
  updated_at timestamptz not null default now()
);
create index if not exists automation_rules_user_updated_idx on public.automation_rules(user_id, updated_at desc);
alter table public.automation_rules enable row level security;
create policy "Users read own automation rules" on public.automation_rules for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users insert own automation rules" on public.automation_rules for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update own automation rules" on public.automation_rules for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "Users delete own automation rules" on public.automation_rules for delete to authenticated using ((select auth.uid()) = user_id);
create table if not exists public.automation_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  rule_id text,
  event_type text not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists automation_history_user_created_idx on public.automation_history(user_id, created_at desc);
alter table public.automation_history enable row level security;
create policy "Users read own automation history" on public.automation_history for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users add own automation history" on public.automation_history for insert to authenticated with check ((select auth.uid()) = user_id);
grant select, insert, update, delete on public.automation_rules to authenticated;
grant select, insert on public.automation_history to authenticated;
