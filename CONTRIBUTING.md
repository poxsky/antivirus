# Contributing to ShieldScan

Thanks for wanting to contribute! ShieldScan is community-built — every rule, whitelist entry, and translation helps.

## Ways to Contribute

- **Risk rules:** Add combo patterns in `risk/ComboRules.kt`
- **Whitelist:** Add top popular apps to `data/whitelist.json` to reduce false positives
- **Category expectations:** Improve `CategoryExpectations.kt` for new Play Store categories
- **Translations:** Hindi, Hinglish, Tamil, Bengali, etc.
- **UI/UX:** Compose screens, dark mode, accessibility

## Development Setup

```bash
git clone https://github.com/poxsky/antivirus.git
cd antivirus
# Open in Android Studio Koala+ (JDK 17, SDK 34)
./gradlew build
./gradlew test
```

## Branch & Commit

- Branch from `main`: `feature/your-feature` or `fix/bug-name`
- Use Conventional Commits: `feat(risk): add new trojan combo`, `fix(scanner): handle null installer`
- Keep PRs focused — one feature per PR

## Code Style

- Kotlin official style + `ktlint`
- Run `./gradlew ktlintCheck` before pushing
- Tests for new `RiskEngine` rules are required

## Pull Request Checklist

- [ ] Tests added / updated
- [ ] No new warnings in `./gradlew lint`
- [ ] README updated if user-facing change
- [ ] Screenshots for UI changes

## Reporting Issues

Use GitHub Issues with:
- Device: Android version, model
- Steps to reproduce
- Expected vs actual behavior
- Logs / screenshots (redact personal data)

## Code of Conduct

Be respectful. We follow the [Contributor Covenant](https://www.contributor-covenant.org/). Harassment or hate speech = immediate ban.

## Questions?

Open a Discussion or reach out via Issues. We'd love to help you get started.
