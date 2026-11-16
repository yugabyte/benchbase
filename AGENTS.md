# AGENTS.md

This file is guidance for AI coding agents working in this repo (Claude Code, Cursor, Codex, …).

- [CLAUDE.md](CLAUDE.md) is the always-on summary of the repo: what the fork is, build/run/test, framework architecture, FeatureBench, and the regression pipelines.
- [skills/](skills/README.md) holds **agent skills for any AI agent**. Each skill is a `SKILL.md`; read the `description` lines to pick one. Current skills:
  - `featurebench-microbenchmark`: create new FeatureBench microbenchmarks. It covers the plan, the category and name, the yugabyte YAML, the postgres / yb_colocated / yugabyte_range variants, and lint. It does not run them, commit or open PRs.
