# anychain

A proof-of-concept blockchain written in Kotlin, structured as a Gradle multi-module project.

## Overview

anychain implements a simple blockchain with SHA-256 proof-of-work, persistent storage via an embedded database, and two interfaces — a CLI and a REST API — all built on top of a shared core library.

```
anychain/
├── core/   # Core library: Block, Blockchain, Transaction
├── cli/    # Command-line interface binary
└── api/    # REST API server (Ktor)
```

## Features

- SHA-256 proof-of-work (configurable difficulty)
- Persistent storage with [RocksDB](https://rocksdb.org/)
- Chain integrity validation
- CLI for local interaction
- REST API for remote interaction
- Structured error handling via a sealed `AnychainException` hierarchy
- Structured logging via [Logback](https://logback.qos.ch/)

---

## Getting started

### Prerequisites

- JDK 21+ (the build targets `jvmToolchain(21)`; the Gradle wrapper auto-provisions one via the Foojay resolver if it can't find one locally)

### Quickstart

```bash
# 1. Clone the repository
git clone https://github.com/your-org/anychain.git
cd anychain

# 2. Build all modules
./gradlew build

# 3. Run tests
./gradlew test

# 4. Try the CLI
./gradlew :cli:run --args="add 'hello world'"
./gradlew :cli:run --args="print"

# 5. Or start the REST API
./gradlew :api:run
# Server is now running at http://localhost:3000
```

### Build everything

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test
```

---

## CLI — `anychain`

```bash
./gradlew :cli:run --args="--help"
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
| `--db <PATH>` | `ANYCHAIN_DB` | `/tmp/anychain` | Path to the RocksDB database directory |

### Examples

```bash
# Add blocks
./gradlew :cli:run --args="add 'first transaction'"
./gradlew :cli:run --args="add 'second transaction'"

# Print the chain
./gradlew :cli:run --args="print"

# Validate integrity
./gradlew :cli:run --args="validate"

# Use a custom database path
ANYCHAIN_DB=./mychain ./gradlew :cli:run --args="add hello"
```

A standalone launcher script (no Gradle needed after building) is produced by:

```bash
./gradlew :cli:installDist
./cli/build/install/anychain/bin/anychain add "hello world"
```

---

## API — `anychain-api`

```bash
./gradlew :api:run
```

The server starts on `http://0.0.0.0:3000` by default.

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `ANYCHAIN_DB` | `/tmp/anychain` | Path to the RocksDB database directory |
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

### `core`

The library module — no I/O beyond persistence, no CLI, no HTTP. Everything else depends on it.

| File | Responsibility |
|---|---|
| `Block.kt` | Block structure, SHA-256 PoW mining, hash validation |
| `Blockchain.kt` | Chain management, RocksDB persistence, iteration |
| `Transaction.kt` | Transaction structure with content-addressed ID |
| `Errors.kt` | Sealed `AnychainException` hierarchy |

### Proof of Work

Each block must have a SHA-256 hash whose hex representation starts with `DIFFICULTY` (4) zero characters. The miner increments a `nonce` until a valid hash is found.

```
hash = SHA256(previous_hash ‖ timestamp ‖ height ‖ nonce ‖ tx_ids ‖ tx_data)
```

### Storage

Blocks are serialized with [kotlinx.serialization CBOR](https://github.com/Kotlin/kotlinx.serialization) and stored in a [RocksDB](https://rocksdb.org/) embedded key-value database keyed by their hash. A special `"LAST"` key always points to the tip of the chain. The `Blockchain` caches the tip block in memory, so `height()` is O(1) instead of walking the chain.

### Concurrency (API)

The REST API guards the shared `Blockchain` with a `kotlinx.coroutines.sync.Mutex`, and runs block mining on `Dispatchers.Default`. A slow proof-of-work mine suspends other in-flight requests without blocking a server thread.

---

## Logging

Set the `ANYCHAIN_LOG` environment variable to change the log level (default `WARN` for the CLI, `INFO` for the API):

```bash
ANYCHAIN_LOG=INFO ./gradlew :cli:run --args="add hello"
ANYCHAIN_LOG=DEBUG ./gradlew :api:run
```

---

## License

MIT
