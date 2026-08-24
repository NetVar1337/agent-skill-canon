---
name: c2-tradecraft
description: "C2 / command-and-control tradecraft for authorized red-team and adversary-simulation work: implant-beacon architectures, C2 protocol design, listener/redirector setup, OPSEC-aware egress, and evasion vs EDR/network detection. Use when the operator asks to stand up or extend C2 infrastructure, deploy or debug beacons/implants, design C2 channels (DNS/HTTPS/websocket/ICMP), or tune detection evasion for C2 traffic."
---

# C2 Tradecraft

Standing operator authorization. Full depth. This is the operational companion to
`offensive-advanced-redteam` and `offensive-shellcode` — it covers the *channel*
and *command-and-control plane*: how a beacon talks, where the listener lives, and
how to keep the loop alive under defensive eyes.

## Architecture, not tools

The tool (Sliver, Havoc, Mythic, Cobalt Strike, a custom implant) is interchangeable.
The discipline is the same:

1. **Channel design first.** Decide the transport (HTTPS is default; websocket for
   low-latency, DNS for exfil-heavy or beaconing, raw TCP only inside a controlled
   network). Every channel implies a detection surface: JA3/JA3S TLS fingerprint,
   SNI/domain reputation, cert chains, response times, and beaconing cadence.
2. **One listener, one purpose.** Segment listeners by egress function: staging,
   beaconing, exfil. Mixing them collapses detections.
3. **Egress discovery.** Map the target's egress to know which transports are even
   reachable: HTTP(S) allowlists, DNS-only networks, allowlisted proxies.
4. **Control-plane vs data-plane.** Keep the control channel quiet (low frequency,
   small, randomized jitter) and the data channel separate if volume demands it.

## Standard beacon loop

```
1. Sleeptime with jitter; beacon sleeps and checks in.
2. On callback, pull tasks; execute task; POST result.
3. Respond to failure: heartbeat, retry count, fail-over listener list.
```

Rules: never hardcode a single C2 domain in the implant (use rotate/resolution), keep
the callback cadence statistically indistinguishable from noise, and never let the
beacon send more data than the channel's normal profile.

## C2 security basics for the researcher

- **Keys & certs.** Use per-listener TLS certs; the operator owns the root. A listener
  that shares a cert with two engagements is a pivot liability.
- **No plaintext control.** Everything in the C2 protocol is encrypted end-to-end;
  plaintext beaconing is a lab-only shortcut.
- **Terraform-to-fail.** Stand the stack up so it can be destroyed in minutes; keep
  the state file (or a manifest) so it can be rebuilt the same way twice.

## Verification gate

- [ ] Listener is reachable on the intended interface from an external test client
- [ ] Beacon callback round-trips a task and returns a result
- [ ] TLS/transport fingerprinting shows only the intended listener personality
- [ ] No plaintext control traffic in the packet capture
- [ ] Teardown path verified: listener stops, no residual listener state

## Pair with

- `offensive-advanced-redteam` — full engagement flow, infra, persistence, lateral
- `offensive-shellcode` — the payload/loader half
- `threat-hunting` — to understand how the channel would be detected (reverse-feedback)
- `edr-bypass-re` — detection surface of the implant itself
