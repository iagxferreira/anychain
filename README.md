# anychain

A proof-of-concept blockchain written in Rust, structured as a production-ready monorepo.

## Overview

anychain implements a simple blockchain with SHA-256 proof-of-work, persistent storage via an embedded database, and two interfaces — a CLI and a REST API — all built on top of a shared core library.

```
anychain/
├── anychain-core/   # Core library: Block, Blockchain, Transaction
├── anychain-cli/    # Command-line interface binary
└── anychain-api/    # REST API server (axum)
```

## Features

- SHA-256 proof-of-work (configurable difficulty)
- Persistent storage with [sled](https://github.com/spacejam/sled)
- Chain integrity validation
- CLI for local interaction
- REST API for remote interaction
- Structured error handling with `thiserror` / `anyhow`
- Structured logging via `env_logger`

---

## Getting started

### Prerequisites

- Rust 1.75+ (`rustup update stable`)

### Quickstart

```bash
# 1. Clone the repository
git clone https://github.com/your-org/anychain.git
cd anychain

# 2. Build all crates
cargo build --release

# 3. Run tests
cargo test

# 4. Try the CLI
cargo run -p anychain-cli -- add "hello world"
cargo run -p anychain-cli -- print

# 5. Or start the REST API
cargo run -p anychain-api
# Server is now running at http://localhost:3000
```

### Build everything

```bash
cargo build --release
```

### Run tests

```bash
cargo test
```

---

## CLI — `anychain`

```bash
cargo run -p anychain-cli -- --help
```

### Commands

| Command | Description |
|---|---|
| `add <DATA>` | Mine a new block containing `DATA` |
| `print` | Print all blocks from tip to genesis |
| `validate` | Validate the integrity of the chain |

### Options

| Flag | Env var | Default | Description |
|---|---|---|---|
| `--db <PATH>` | `ANYCHAIN_DB` | `/tmp/anychain` | Path to the sled database |

### Examples

```bash
# Add blocks
cargo run -p anychain-cli -- add "first transaction"
cargo run -p anychain-cli -- add "second transaction"

# Print the chain
cargo run -p anychain-cli -- print

# Validate integrity
cargo run -p anychain-cli -- validate

# Use a custom database path
ANYCHAIN_DB=./mychain cargo run -p anychain-cli -- add "hello"
```

---

## API — `anychain-api`

```bash
cargo run -p anychain-api
```

The server starts on `http://0.0.0.0:3000` by default.

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `ANYCHAIN_DB` | `/tmp/anychain` | Path to the sled database |
| `PORT` | `3000` | Port to listen on |

### Endpoints

#### `GET /blocks`
Returns all blocks from tip to genesis.

```bash
curl http://localhost:3000/blocks
```

```json
[
  {
    "hash": "0000a3f...",
    "previous_hash": "0000b1c...",
    "height": 1,
    "timestamp": 1700000000000,
    "nonce": 48291,
    "transactions": [
      { "id": "abc123...", "data": "first transaction", "timestamp": 1700000000000 }
    ]
  }
]
```

#### `POST /blocks`
Mines a new block with the given data.

```bash
curl -X POST http://localhost:3000/blocks \
  -H "Content-Type: application/json" \
  -d '{"data": "first transaction"}'
```

Returns the newly mined block as JSON.

#### `GET /blocks/:hash`
Returns a single block by its hash.

```bash
curl http://localhost:3000/blocks/0000a3f...
```

Returns `404` if the block does not exist.

#### `GET /validate`
Validates the integrity of the entire chain.

```bash
curl http://localhost:3000/validate
```

Returns `200 OK` if valid, `409 Conflict` if invalid.

---

## Architecture

### `anychain-core`

The library crate — no I/O, no CLI, no HTTP. Everything else depends on it.

| Module | Responsibility |
|---|---|
| `block` | Block structure, SHA-256 PoW mining, hash validation |
| `blockchain` | Chain management, sled persistence, iteration |
| `transaction` | Transaction structure with content-addressed ID |
| `error` | Typed errors via `thiserror` |

### Proof of Work

Each block must have a SHA-256 hash whose hex representation starts with `DIFFICULTY` (4) zero characters. The miner increments a `nonce` until a valid hash is found.

```
hash = SHA256(previous_hash ‖ timestamp ‖ height ‖ nonce ‖ tx_ids ‖ tx_data)
```

### Storage

Blocks are serialized with `bincode` and stored in a `sled` embedded key-value database keyed by their hash. A special `"LAST"` key always points to the tip of the chain.

---

## Logging

Set the `RUST_LOG` environment variable to enable logs:

```bash
RUST_LOG=info cargo run -p anychain-cli -- add "hello"
RUST_LOG=anychain_core=debug cargo run -p anychain-api
```

---

## License

MIT
