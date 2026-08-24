# TDD Enforcement

The loop that keeps the red→green discipline honest. Consult before and during
every cycle when the user has asked for strict TDD.

## Iron Law

```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

Write code before the test? Delete it. Start over. No exceptions:

- Don't keep it as "reference"
- Don't "adapt" it while writing tests
- Don't look at it — delete means delete
- Implement fresh from tests. Period.

## Verify RED — watch it fail (mandatory)

```bash
pytest tests/test_feature.py::test_specific_behavior -v
```

- The test fails (not errors from typos)
- The failure message is the expected one
- It fails because the feature is missing
- Passes immediately? You're testing existing behavior — fix the test
- Errors? Fix the error, re-run until it fails correctly

## Verify GREEN — watch it pass (mandatory)

```bash
pytest tests/test_feature.py::test_specific_behavior -v
pytest tests/ -q
```

- Test passes, other tests still pass, output pristine
- Fails? Fix the code, not the test
- Other tests fail? Fix regressions now

## Refactor

Only after green. Remove duplication, improve names, extract helpers. Keep tests green throughout. If tests fail during refactor: undo immediately, take smaller steps.

## Anti-patterns that end the loop

| Tell | Reality |
|------|---------|
| "Too simple to test" | Simple code breaks. Test takes 30s. |
| "I'll test after" | Passing immediately proves nothing. |
| "Already manually tested" | Ad-hoc ≠ systematic. No record, no re-run. |
| "Deleting X hours is wasteful" | Sunk cost. Keeping unverified code is debt. |
| "Keep as reference, write tests first" | You'll adapt it. That's testing after. |
| "TDD is dogmatic" | TDD IS pragmatic — finds bugs before commit. |
| Test passes immediately on first run | You're testing existing behavior. |
| Horizontal slicing (all tests, then all impl) | Bulk tests verify imagined behavior. |

**All of these mean: delete code, start over with TDD.**

## Completion checklist

Before marking work complete:

- [ ] Every new function/method has a test
- [ ] Watched each test fail before implementing
- [ ] Each test failed for the expected reason (feature missing, not typo)
- [ ] Wrote minimal code to pass each test
- [ ] All tests pass, output pristine
- [ ] Tests use real code (mocks only where unavoidable)
- [ ] Edge cases and errors covered

Can't check every box? You skipped TDD. Start over.
