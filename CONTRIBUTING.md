# Contributing

Thank you for your interest in contributing to this project!

## Branching Strategy

**Never commit directly to `main`.** Always work on a feature branch.

### Branch Naming Convention

```
main                    ← production-ready code only
├── feature/<name>      ← new features
├── bugfix/<name>       ← bug fixes
├── hotfix/<name>       ← urgent production fixes
├── docs/<name>         ← documentation changes
├── refactor/<name>     ← code restructuring
├── experiment/<name>   ← research/prototyping
└── release/<version>   ← release preparation
```

### Examples

```
feature/add-login-api
bugfix/fix-null-pointer
docs/update-readme
refactor/simplify-router
experiment/new-routing-algo
release/v1.2.0
```

### Avoid

```
new
test
temp
backup
final_final
```

## Workflow

1. **Create a branch** from `main`
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/my-feature
   ```

2. **Make changes** and commit with conventional messages
   ```bash
   git add -A
   git commit -m "feat: add user authentication"
   ```

3. **Push** your branch
   ```bash
   git push origin feature/my-feature
   ```

4. **Open a Pull Request** on GitHub
   - Fill out the PR template
   - Link related issues
   - Request review

5. **After review**, merge via GitHub (not force push)

6. **Delete** your feature branch after merge
   ```bash
   git checkout main
   git pull origin main
   git branch -d feature/my-feature
   ```

## Commit Message Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: Add OAuth authentication
fix: Prevent null pointer during startup
docs: Update installation guide
refactor: Simplify provider registry
test: Add integration tests
ci: Add release workflow
chore: Upgrade dependencies
```

## Development Setup

1. Clone your fork
2. Create a virtual environment
3. Install dependencies
4. Create a feature branch
5. Make your changes
6. Run tests before submitting

## Code Style

- Follow project-specific linting rules
- Use meaningful variable names
- Add docstrings to functions
- Keep functions focused and small

## Reporting Issues

- Use the GitHub issue tracker
- Include steps to reproduce
- Include environment details
- Be as descriptive as possible

## Pull Request Process

1. Update documentation if needed
2. Add tests for new features
3. Ensure all tests pass
4. Request review from maintainers
5. Address review feedback
6. Merge after approval
