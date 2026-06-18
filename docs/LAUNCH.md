# Launch Plan

The launch goal is to make KAI OS legible as a new infrastructure category: a local-first Evidence OS for AI agents in Kotlin.

## Positioning

Lead with:

```text
Local-first Evidence OS for AI agents in Kotlin
Agent = Process, Workflow = Scheduler, Tool = Syscall
```

Avoid positioning it as:

- a chatbot framework
- a Kotlin LangChain clone
- a thin SDK wrapper
- a Homebrew package

Homebrew, hosted installer, release ZIP, and source builds are adoption paths. The product is KAI OS.

## Primary Demo Hook

Use the first-run tour:

```bash
curl -fsSL https://morning-verlu.github.io/KAI/install.sh | sh
export PATH="$HOME/.kaios/bin:$PATH"
kaios tour
```

The tour is the clearest proof because it shows the full Evidence OS loop without requiring an API key or a real project:

- disposable Git workspace
- current-change review
- process table
- process trace
- replayable capsule
- evidence summary
- recovery dry-run report

## Launch Channels

- GitHub Discussion announcement
- GitHub README and launch site
- Hacker News "Show HN"
- Kotlin Slack
- X/LinkedIn thread with the evidence proof image
- DEV.to or Medium technical post after the first discussion feedback
- Newsletter or curated-list submissions after the first two public readouts

## First 100-Star Checklist

- [x] CI/test command is green.
- [x] Hosted install command works.
- [x] GitHub Codespaces path exists for no-local-install trials.
- [x] `kaios tour` works without API keys.
- [x] README has a clear first screen.
- [x] GitHub Pages has a direct launch landing page.
- [x] Social card asset is published for launch posts.
- [x] Demo GIF shows the CLI flow.
- [x] Release ZIP/TAR assets are attached.
- [x] Roadmap reflects v0.3.1 Evidence OS, not stale milestones.
- [x] Contributing guide points new contributors at tour/review/evidence.
- [x] Repository is pinned on the maintainer profile.
- [x] Custom GitHub social preview image is uploaded from `docs/assets/kaios-social-card.png`.
- [x] Short social post is published.
- [x] GitHub Discussion is updated with v0.3.1 tour CTA.
- [ ] Kotlin community post is published.
- [ ] Show HN post is published.
- [ ] Early questions are answered within 24 hours.

## 1000-Star Reality

Stars should come from real interest, not automation or artificial engagement. The controllable path is:

- sharp concept
- no-key working demo
- strong README first screen
- repeated but respectful launch posts
- fast replies to real questions
- quick follow-up releases from user feedback
- visible issues for contributors

See [STAR_GROWTH.md](STAR_GROWTH.md) for the 1000-stars operating plan. See [LAUNCH_KIT.md](LAUNCH_KIT.md) for copy-paste launch posts, channel-specific drafts, reply guidance, and metrics to capture. Use [launch-posts/post-now.md](launch-posts/post-now.md) for the next manual post queue and [launch-posts/community-targets.md](launch-posts/community-targets.md) for channel priority. Use [LAUNCH_METRICS.md](LAUNCH_METRICS.md) and `./scripts/launch-metrics.sh` for the repeatable +2h/+24h/+72h readout. Standalone drafts live in [launch-posts](launch-posts/).
Use the [Contributor Board](CONTRIBUTOR_BOARD.md) when someone asks how to help without a large PR.
Latest saved operating snapshot: [2026-06-17](launch-snapshots/2026-06-17.md).
Public repository CI is prepared but still blocked on GitHub `workflow` token
scope; the maintainer procedure is tracked in [CI_ENABLE_RUNBOOK.md](CI_ENABLE_RUNBOOK.md).

## First External Wave

Use [launch-posts/first-wave.md](launch-posts/first-wave.md) when posting externally. The first wave is intentionally narrow:

1. Show HN with the GitHub repo URL and a first comment pointing to the Proof Pack.
2. Kotlin/JVM community post with a feedback-first angle.
3. X/LinkedIn technical thread with the evidence proof image.
4. DEV.to or Medium article after the first two public readouts clarify objections.

Lead with the Evidence OS model, then offer Proof Pack, Evidence Viewer, and `kaios tour`.

If GitHub views remain near zero after a live post, switch channel before changing product copy. If views rise but stars stay flat, improve README, Proof Pack, or Why Star before the next post.

## Contributor Intake

Visible, scoped issues help turn early attention into participation. Keep these queues maintained during launch:

- Contributor Board: https://github.com/morning-verlu/KAI/blob/main/docs/CONTRIBUTOR_BOARD.md
- Good first issues: https://github.com/morning-verlu/KAI/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22
- Help wanted issues: https://github.com/morning-verlu/KAI/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22help%20wanted%22
- Feedback issues: https://github.com/morning-verlu/KAI/issues?q=is%3Aissue%20state%3Aopen%20label%3Afeedback
- Kotlin/JVM API feedback: https://github.com/morning-verlu/KAI/issues/new?template=kotlin_api_feedback.yml
- Kotlin/JVM feedback discussion: https://github.com/morning-verlu/KAI/discussions/17
- Public CI enablement runbook: [CI_ENABLE_RUNBOOK.md](CI_ENABLE_RUNBOOK.md)

## Manual GitHub Settings Tasks

GitHub's documented path for repository social preview image upload is the repository settings UI. This repository has uploaded:

```text
docs/assets/kaios-social-card.png
```

Verification:

```bash
gh repo view morning-verlu/KAI --json usesCustomOpenGraphImage,openGraphImageUrl
```

`usesCustomOpenGraphImage` is expected to be `true`.

Reference: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/customizing-your-repositorys-social-media-preview
