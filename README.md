# Build Your Own Claude Code (Java)

[![CodeCrafters Progress](https://backend.codecrafters.io/progress/claude-code/d63ba2bf-bdf8-4a88-9fa1-166b7eadcc12)](https://app.codecrafters.io/users/omkolte-2106?r=2qF)
![Java Version](https://img.shields.io/badge/Java-25-orange.svg)
![OpenAI Java SDK](https://img.shields.io/badge/OpenAI%20SDK-v0.36.0-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)

An autonomous, terminal-based AI coding assistant built from scratch in Java 25 as part of the **CodeCrafters "Build Your Own Claude Code"** challenge.

This agent leverages Large Language Models (LLMs) via OpenRouter to read codebases, analyze project structures, execute shell commands, edit files, and iteratively solve coding tasks through a multi-turn agentic loop.

---

## 🌟 Key Features

- 🔄 **Autonomous Agentic Loop**: Persists multi-turn conversation context across multiple model invocations, running iteratively until tasks are completely resolved.
- 📁 **`read_file` Tool**: Inspects file contents from the local workspace with robust path resolution (`path` and `file_path`).
- ✍️ **`write_file` Tool**: Creates or overwrites files with automated directory creation.
- ⚡ **`bash` Tool**: Executes arbitrary shell commands (`sh -c` on Unix/Linux, `cmd.exe /c` on Windows), capturing combined stdout and stderr directly into the model's feedback loop.
- 🌐 **OpenRouter & OpenAI SDK Integration**: Modern Java implementation utilizing `com.openai:openai-java:0.36.0` and Jackson JSON processing.

---

## 🏗️ Architecture & Control Flow

```
                      +-------------------+
                      |   User Prompt     |
                      +---------+---------+
                                |
                                v
               +----------------------------------+
               |  Initialize Agent Loop History   |
               +----------------+-----------------+
                                |
                                v
                +---------------+----------------+
                |  Call LLM API with Tools Specs | <------+
                +---------------+----------------+        |
                                |                         |
                                v                         |
                  /---------------------------\           |
                 /  LLM Response Choice Type?  \          |
                 \-----------------------------/          |
                       /                 \                |
        [Tool Call Requested]        [Final Output]       |
                     /                     \              |
                    v                       v             |
      +---------------------------+   +----------------+  |
      |   Execute Requested Tool  |   | Print Response |  |
      |  (read_file, write, bash) |   |  to Standard   |  |
      +-------------+-------------+   |     Output     |  |
                    |                 +-------+--------+  |
                    v                         |           |
      +---------------------------+           v           |
      | Append Tool Result to     |        [ Exit ]       |
      | Conversation History      |                       |
      +-------------+-------------+                       |
                    |                                     |
                    +-------------------------------------+
```

---

## 🛠️ Tools Specification

### 1. `read_file`
Reads text content from specified files in the local filesystem.
- **Parameters**: `file_path` (or `path`)

### 2. `write_file`
Creates new files or overwrites existing ones, automatically initializing missing parent directories.
- **Parameters**: `file_path` (or `path`), `content`

### 3. `bash`
Runs shell commands on the host machine and feeds execution logs back to the LLM context.
- **Parameters**: `command`

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK 25)**
- **Apache Maven 3.8+**
- **OpenRouter API Key** (Set as environment variable `OPENROUTER_API_KEY`)

### Environment Setup

Set your OpenRouter API key:

```bash
export OPENROUTER_API_KEY="your-openrouter-api-key"
```

### Running Locally

Execute the provided wrapper script to run the AI assistant with any prompt:

```bash
./your_program.sh -p "Read README.md and create a simple main.py file"
```

### Building manually

```bash
mvn clean package -Ddir=/tmp/codecrafters-build-claude-code-java
```

---

## 📁 Repository Structure

```
.
├── codecrafters.yml        # CodeCrafters buildpack configuration
├── pom.xml                 # Maven build dependencies & Java 25 preview settings
├── README.md               # Project documentation
├── your_program.sh         # Local runner script
└── src/
    └── main/
        └── java/
            └── Main.java   # Core CLI entry point, Agent Loop & Tool execution
```

---

## 🧪 Testing

To run the automated test suite against CodeCrafters:

```bash
codecrafters submit
```

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
