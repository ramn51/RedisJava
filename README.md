# JKRedis: High-Performance Key-Value Store

> **A multi-threaded, persistent Redis clone built from scratch in Java.**

JKRedis is a lightweight implementation of a Redis-like database main.java.server. It is built using standard Java I/O (ServerSocket) without external frameworks like Netty, designed to demonstrate deep understanding of **Systems Programming**, **Network Protocols (TCP/RESP)**, and **Concurrency**.

---

## 🚀 Features

### Core Capabilities

* **In-Memory Storage:** Thread-safe key-value main.java.storage using `ConcurrentHashMap`.
* **RESP Protocol:** Full implementation of the **Redis Serialization Protocol**, making it compatible with official Redis clients (`redis-cli`, Jedis, etc.).
* **Concurrent Networking:** Handles multiple concurrent clients using a custom **Thread Pool** architecture.

### Advanced Systems Features

* **Real-Time Pub/Sub:**  Implements the Publish/Subscribe messaging pattern for event-driven architectures.
* Supports `SUBSCRIBE` for listening to channels and `PUBLISH` for broadcasting.
* Uses **fan-out architecture** with `CopyOnWriteArrayList` to safely manage concurrent subscribers without blocking publishers.


* **Persistence (AOF):** Implements **Append-Only File** logging. Data survives main.java.server crashes and is replayed on startup (Crash Recovery).
* **Expiry & Eviction (TTL):** Supports `PX` (milliseconds) expiration.
* **Lazy Eviction:** Keys checked on access.
* **Active Eviction:** Background thread (Probabilistic Algorithm) cleans up expired keys every 100ms.


* **Master-Replica Replication:**
* Supports `--replicaof` configuration.
* Implements the full **PSYNC Handshake** (`PING`  `REPLCONF`  `PSYNC`  `FULLRESYNC`).
* Real-time command propagation from Master to Replicas.



---

## 🛠 Architecture

### High-Level Design

* **Language:** Java 17+
* **Architecture:** Thread-per-client (Managed via ExecutorService).
* **Storage Engine:** In-memory Heap with AOF disk flushing.

### Class Structure

* **`JKServer`:** The entry point. Handles argument parsing, initialization, and the accept loop.
* **`ClientHandler`:** The worker logic. Parses RESP streams and executes commands.
* **`Storage`:** The thread-safe wrapper around the data structure (K-V Store).
* **`PubSubManager`:** Manages channel subscriptions and handles thread-safe message broadcasting to active socket streams.
* **`AofHandler`:** Manages low-level File I/O for persistence.
* **`ReplicaManager`:** (Master Side) Manages the registry of connected replicas for data propagation.

---

## ⚡ Getting Started

### Prerequisites

* Java Development Kit (JDK) 8 or higher.
* Netcat (optional, for testing) or the included `JKClient`.

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/jkredis.git
cd jkredis

```


2. **Compile**
```bash
javac -d out src/main.java.server/*.java src/main.java.storage/*.java src/main.java.parser/*.java

```



---

## 📖 Usage

### 1. Running a Master Node

By default, the main.java.server runs on port `6379`.

```bash
java -cp out main.java.server.JKServer --port 6379

```

### 2. Running a Replica Node

To start a distributed system, run a second instance in a new terminal:

```bash
java -cp out main.java.server.JKServer --port 6380 --replicaof localhost 6379

```

*The replica will perform a handshake, sync old data, and listen for updates.*

### 3. Using Pub/Sub (Real-Time Messaging)

Open two terminals to test the messaging system.

**Terminal A (Subscriber):**

```bash
# Connect and subscribe to the 'news' channel
nc localhost 6379
SUBSCRIBE news

```

*Terminal A will now hang, waiting for messages.*

**Terminal B (Publisher):**

```bash
# Publish a message to the 'news' channel
echo -e "*3\r\n$7\r\nPUBLISH\r\n$4\r\nnews\r\n$5\r\nHello\r\n" | nc localhost 6379

```

*Result: Terminal A immediately receives the "Hello" message.*

### 4. Standard Key-Value Operations

**Using Netcat:**

```bash
echo -e "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$3\r\nval\r\n" | nc localhost 6379

```

**Supported Commands:**

```text
SET key value [PX milliseconds]  # Set key with optional expiry
GET key                          # Get value
PUBLISH channel message          # Broadcast message
SUBSCRIBE channel                # Listen for messages
PING                             # Test connection
INFO                             # Server stats

```

---

## 🧪 Testing Persistence

1. Start the main.java.server.
2. Run `SET user "JK"`.
3. **Kill the main.java.server** (Ctrl+C).
4. Restart the main.java.server.
5. Run `GET user`.
* *Result:* It returns `"JK"`. The data was recovered from `database.aof`.



---

## 🚧 Roadmap (Coming Soon)

* **Transactions (ACID):** Implementing `MULTI`, `EXEC`, and `DISCARD` for atomic operations.
* **RDB Snapshots:** Binary point-in-time backups.
* **Cluster Mode:** Sharding data across multiple nodes (Redis Cluster).
