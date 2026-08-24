---
name: web3-security
description: Audit smart contracts, analyze Web3 protocols, reproduce security flaws on local forks, investigate transaction behavior, and develop proof-of-concept exploit paths. Use for Solidity, EVM, DeFi, bridge, wallet, signing, MEV, and on-chain security tasks.
---

# Web3 Security

## Workflow

1. Identify the chain, protocol version, deployed addresses, source provenance, compiler settings, privileged roles, and value-bearing flows.
2. Map trust boundaries across contracts, upgrade paths, oracle inputs, bridges, off-chain signers, relayers, and user-controlled calldata.
3. Build a local or forked test environment before state-changing validation. Pin the block number and record RPC, chain ID, and contract bytecode hashes.
4. Review access control, accounting invariants, reentrancy, rounding, oracle manipulation, signature replay and domain separation, upgrade authorization, delegatecall, storage layout, and cross-chain message validation.
5. Reproduce confirmed findings with deterministic tests or scripts. Record prerequisites, transaction sequence, expected state deltas, and mitigation.
6. For live-chain work, inspect first and require an explicit instruction before broadcasting transactions or moving assets.

## Deliverables

Provide the affected contracts and functions, severity rationale, exploit preconditions, reproducible test or calldata, observed impact, and a minimal remediation with regression coverage.

## Tooling

Prefer the repository's existing stack. Typical tools are Foundry (`forge`, `cast`, `anvil`), Hardhat, Slither, Echidna, Medusa, and chain explorers or RPC clients. Verify tool versions and network targets before use.
