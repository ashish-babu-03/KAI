# Community Targets

Status: operating notes, not proof that anything has been posted.

Use this after `post-now.md` when choosing where to publish the next KAI OS update. The current launch diagnosis is still distribution failure, so the goal is to reach Kotlin/JVM and infrastructure audiences without posting the same copy everywhere.
After any post goes live, use [follow-up-playbook.md](follow-up-playbook.md) to decide whether the next action is replying, switching channel, or improving conversion.

## Priority Order

| Priority | Channel | Why it fits | Use | Risk |
| --- | --- | --- | --- | --- |
| 1 | Show HN | Good fit because KAI OS is runnable, no-key, and has checked-in proof | Use the Show HN version from `show-hn.md` | Needs maintainer available to answer comments |
| 2 | Kotlin Slack / Kotlin community | Official Kotlin community surface; best chance of Kotlin API feedback | Use the Kotlin/JVM version in `post-now.md` | Requires joining or having access |
| 3 | X / LinkedIn engineering audience | Useful for short visual explanation and second-order sharing | Use the technical-thread hook in `x-linkedin-thread.md` | Often weaker for deep feedback |
| 4 | DEV.to / Medium | Best for a durable technical explanation after objections are known | Use the article outline in `second-wave.md` | Slower feedback loop |
| 5 | Newsletters / curated lists | Useful after there is a proof page and public discussion trail | Submit Proof Pack + repo | Delayed and uncertain publication |

## Source Notes

- Kotlin's official community page links Slack, Reddit, StackOverflow, YouTube, LinkedIn, the Kotlin blog, and issue tracker as "Keep in Touch" surfaces: https://kotlinlang.org/community/
- Kotlin Discussions points people to the Kotlin Slack sign-up link from the community page: https://discuss.kotlinlang.org/t/how-to-join-kotlinlang-slack-com/29771
- r/Kotlin rules emphasize being civil, avoiding spam, staying on Kotlin topics, and avoiding low-effort fluff: https://www.reddit.com/r/Kotlin/
- Show HN is for something people can try, and the guidelines ask for a title beginning with `Show HN`: https://news.ycombinator.com/showhn.html

## Recommended Next Post

Post Show HN first if the maintainer can answer comments for the first 2-3 hours:

```text
Show HN: KAI OS - an Evidence OS for Kotlin AI agents
```

Use the GitHub repository as the submitted URL:

```text
https://github.com/morning-verlu/KAI
```

Then add a first comment with the Proof Pack and Evidence Viewer:

```text
Proof Pack: https://morning-verlu.github.io/KAI/proof-pack.html
Evidence Viewer: https://morning-verlu.github.io/KAI/evidence-viewer.html
```

If Show HN cannot be monitored, use the Kotlin/JVM feedback version first:

```text
I would especially like feedback on whether the Kotlin runtime API feels idiomatic, whether the Agent = Process model is useful for JVM teams, and whether replayable capsules / CI evidence gates would help maintainers trust agent reviews.
```

## Post-Publish Loop

After any post:

1. Save the URL in issue #7.
2. Run `./scripts/launch-metrics.sh`.
3. Use [follow-up-playbook.md](follow-up-playbook.md) at roughly +2h, +24h, and +72h.
4. If GitHub views stay at 0, switch channel before changing the project page again.
5. If views rise but stars stay flat, inspect the README first screen and Evidence Viewer conversion path.

## Do Not Do

- Do not post the same text to all communities.
- Do not ask for upvotes.
- Do not claim production maturity; say it is early and local-first.
- Do not make the post about installation mechanics. Lead with inspectable runtime evidence.
- Do not post Show HN unless the maintainer can stay around to answer questions.
