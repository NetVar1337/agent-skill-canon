---
name: aimbot-humanization
description: "Humanize aim assistance: smoothing, aim curves, reaction delay, FOV/stickiness, target switch, noise, anti-pattern AC."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "aimbot humanize"
    - "aim smoothing"
    - "aim curve"
    - "humanized aim"
---

# Aimbot humanization

Make aim assistance look like a skilled human, not a math function locked to the skull.

## Perceptual model
- Humans overshoot/correct; imperfect tracking on strafe
- Reaction delay 120–280ms typical; not constant
- Mouse has accel/raw input quirks; camera has sensitivity curves
- Target switches cost time; not instant snap

## Control stack
1. **Target select**: FOV cone, visibility/LOS, team/hp filters, priority scores
2. **Pathing**: angle delta → easing curve (ease-in-out, crit-damped spring, Bezier)
3. **Noise**: Ornstein–Uhlenbeck / band-limited noise on yaw/pitch
4. **Deadzone**: don't micro-twitch on tiny errors
5. **Velocity match**: track target velocity; lead for projectiles
6. **Breaks**: occasional intentional miss / recapture
7. **Input device model**: mouse counts vs controller stick acceleration

## Parameters (tune per game)
- max FOV, max angular velocity, acceleration, smoothing window
- reaction delay distribution (log-normal)
- bone blend (head/chest) with context (range/weapon)
- hit-chance scheduler independent of raw lock

## Anti-detection themes
- Avoid perfect bone lock every frame
- Avoid identical tick-aligned snaps
- Correlate with visible animation / ADS state
- Don't aim through freshly broken LOS without delay

## Implementation sketch
```text
each frame:
  if !should_engage(): release_soft(); return
  tgt = select_target()
  err = angle_to(tgt.aim_point + lead)
  err = apply_reaction_delay(err)
  delta = clamp(smooth(err) + noise(), max_rate)
  apply_mouse_or_view(delta)
```

## Concrete parameterizations (shipped-tested shapes)

### Reaction model
- Two-stage: detection (uniform 120-200ms) + motor onset (log-normal μ≈5.1 σ≈0.4, median ~165ms). First engagement of a match slower (+15%); target-switch faster than fresh acquisition (~60% of fresh reaction).
- Per-target memory: reacquisition of a recently-seen target uses 40-60% reaction — models expectation.

### Smoothing curves ranked by realism
| Curve | Trace look | Use |
|---|---|---|
| Linear lerp (α·err) | exponential decay, robotic | never |
| Critically-damped spring | fast middle, soft landing | default |
| Cubic ease-in-out on err magnitude | human-ish accel profile | good |
| Bezier w/ random control pts | best but needs per-target re-plan on movement bursts | advanced |
- Feed-forward: add target angular velocity term so tracking lags behind strafes by a small constant (30-60ms virtual latency) instead of locking.

### Noise that survives spectral analysis
- OU process: `dθ = -θ/τ dt + σ√dt·N(0,1)` with τ≈80-120ms — band-limited, no white-spike signature. Tune σ per-axis (yaw 2-3× pitch: humans correct yaw harder).
- Micro-tremor: 8-12Hz small-amplitude (0.05-0.15°) additive sinusoid — matches physiological hand tremor bands.
- Correlate noise with error magnitude (bigger err → bigger overshoot variance); uncorrelated noise is itself a detector.

### Overshoot/correction
- P(overshoot) ≈ 15-25% on fast flicks (>15°), near-zero on micro-corrections; overshoot magnitude 5-12% of flick angle, correction settle 1-2 counter-moves over 150-300ms.
- Miss scheduler: on low-visibility/high-motion shots, deliberately seed 3-8% miss offsets (first-bullet high on spray transfer is the human-tell you fake in reverse).

### Detectors to stay under (what AC measures)
- Angular-velocity spectrogram: pure lock = single delta repeated; human = broad spectrum with 1/f falloff — OU noise provides this.
- Crosshair-bone coincidence rate: cap total time-on-bone < 65% even when perfect tracking possible; blend bone target (head/neck/chest per-shot sampling).
- Reaction-time floor: never fire < 120ms after LOS gain; jitter never < 40ms from same stimulus twice.
- Tick alignment: apply aim on render frames (not net ticks) with ±half-frame jitter — tick-aligned actions = bot signature.

### Failsafe interlocks
- Disable on: death/spectate, scoreboard/chat/menu focus, loading screens (view matrix invalid), replay/editor capture active.
- Soft-release on target death: keep tracking velocity for 80-150ms then decay — instant release at exact death tick is a tell.

## Pair with
`aimbot-triggerbot`, `game-internals`, `game-hacking`.
