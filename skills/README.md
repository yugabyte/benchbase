# benchbase agent skills

These are task playbooks for any AI coding agent (Claude Code, Cursor, Codex, Gemini CLI, …) and for humans. The layout follows `perf-devops/skills/`. Each skill is a folder with a `SKILL.md` in the open Agent Skills format (YAML frontmatter with `name` and `description`, then markdown instructions), plus optional `references/`, `scripts/` and `assets/`. The scripts are Python 3 and need only PyYAML.

| Skill | Use it to | Scripts |
|---|---|---|
| [featurebench-microbenchmark](featurebench-microbenchmark/SKILL.md) | Turn a requirement into an approved plan and lint-clean FeatureBench YAMLs (yugabyte + postgres / yb_colocated / yugabyte_range) under `config/yugabyte/regression_pipelines/`. It does not run, commit or open PRs. | `fbtool.py next-id / variants / lint` |

Related skills live elsewhere:
- `.cursor/skills/announcing-new-benchmarks` drafts team announcements.
- The perf-devops repo has `add-featurebench-workload` (registering a new category for nightly), `trigger-nightly-test` (running YAMLs on a universe) and others.

## Using the skills

- **Any agent:** point it at this directory (see `AGENTS.md`). It reads the `description` lines to choose a skill, then reads that `SKILL.md`. Run scripts from the repo root: `python3 skills/<skill>/scripts/<script>.py --help`.
- **Claude Code in this repo:** `.claude/skills/<name>/SKILL.md` are thin generated wrappers that point here, because Claude Code does not reliably follow symlinked skill directories. After adding, renaming or changing the description of a skill, run `python3 skills/install.py claude-project` and commit the wrappers.
- **Claude Code everywhere, for you personally:** run `python3 skills/install.py claude-user`. It writes wrappers to `~/.claude/skills/` with absolute paths to this checkout, so a `git pull` updates them. `claude-user --remove` removes only the wrappers it created.
- **claude.ai / Claude Desktop:** run `python3 skills/install.py zip`. It writes one zip per skill to `skills/dist/` (gitignored), which you upload under Settings → Capabilities → Skills.
- **Check for drift:** `python3 skills/install.py check` exits 1 if a wrapper is stale.

## Conventions for skill authors

- `name` must equal the folder name: lowercase letters, digits and hyphens, at most 64 characters.
- `description` must be at most 1024 characters with no angle brackets. It should say both what the skill does and when to use it.
- Edit only `skills/<name>/`. Never hand-edit the generated wrappers.
- Scripts are read-only by default and must not print secrets.
