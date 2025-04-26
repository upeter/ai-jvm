# LangChain4j Ktor Server

This module provides a Ktor server implementation of the LangChain4j AI assistant, mirroring the functionality of the Spring Boot implementation in the `langchain4j` module.

## Features

- Simple chat endpoint: `/ai/ask?message=your message here`
- Streaming chat endpoint: `/ai/ask/stream?message=your message here`

## Configuration

The server is configured using the `application.conf` file in the `src/main/resources` directory. The following configuration options are available:

- Server port: 8082 (can be overridden with the `PORT` environment variable)
- OpenAI API key: Set via the `OPENAI_API_KEY` environment variable
- OpenAI model configuration:
  - Model name: gpt-4o
  - Temperature: 0.0
  - Timeout: 60 seconds
  - Logging: Requests and responses are logged

## Running the Server

To run the server, use the following command:

```bash
./gradlew :langchain4j-ktor:run
```

## Usage

### Simple Chat

```bash
curl "http://localhost:8082/ai/ask?message=Hello"
```

### Streaming Chat

```bash
curl "http://localhost:8082/ai/ask/stream?message=Hello"
```

## Implementation Details

This Ktor implementation provides the same functionality as the Spring Boot implementation, but uses Ktor-specific features:

- Routing is configured using Ktor's routing DSL
- Dependency injection is handled manually rather than using Spring's DI
- Configuration is loaded from `application.conf` using Ktor's configuration API
- Streaming responses use Ktor's `respondTextWriter` and Kotlin's Flow