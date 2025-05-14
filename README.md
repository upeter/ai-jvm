# Sources for KotlinConf 2025 talk 
## From 0 to H-AI-ro: fast track to AI for Kotlin developers.

## Run the examples
- Install the Kotlin Notebooks plugin in IntelliJ
- You need an OpenAI API key

### LangChain4J with Spring Boot in module: `/langchain4j`

#### Run LangChain4J sample application
- Provide the OpenAI key OPENAI_API_KEY in the environment
- Run application: [LangChain4JDemoApplication.kt](langchain4j/src/main/kotlin/dev/example/LangChain4JDemoApplication.kt)
- Run Samples in the notebook: [langchain4j-demo.ipynb](langchain4j/langchain4j-demo.ipynb)


#### Play with Embeddings and Similarity
- Run Samples in the notebook: [langchain4j-similarity.ipynb](langchain4j/langchain4j-similarity.ipynb)


#### Data Ingestion
- Start docker-compose: `docker-compose up`
- Open the notebook: [langchain4j-ingest.ipynb](langchain4j/langchain4j-ingest.ipynb)
- Run code snippets to ingest data


### LangChain4J with ktor in module: `/langchain4j-ktor`

#### Run LangChain4J-ktor sample application
- Provide the OpenAI key OPENAI_API_KEY in the environment
- Run application: [Application.kt](langchain4j/src/main/kotlin/dev/example/LangChain4JDemoApplication.kt)
- Run Samples in the notebook: [/langchain4j/langchain4j-demo.ipynb](langchain4j/langchain4j-demo.ipynb)


### SpringAI in module: `/spring-ai`

#### Run SpringAI sample application
- Provide the OpenAI key OPENAI_API_KEY in the environment
- Start docker-compose: `docker-compose up`
- Run application: [SpringAIDemoApplication.kt](spring-ai/src/main/kotlin/dev/example/SpringAIDemoApplication.kt)
  - The first time it takes a while since the vector db is ingested using the spring-ai way
- Run Samples in the notebook: [spring-ai-demo.ipynb](spring-ai/spring-ai-demo.ipynb)


#### Run KMP chat / audio client
- Run KMP Chat Client `main` in  [Main.kt](spring-ai/src/test/kotlin/dev/example/ChatClient.kt) in the `chatclient-kmp` module
