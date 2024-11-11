# Sources for JFall 2024 talk 
## From 0 to H-AI-ro: how to unlock the power of generative AI with LangChain4J and Spring AI
## Quick Youtube intro from foojay.io interview with Frank Delporte:
- https://www.youtube.com/watch?v=O9ODm4ojg8I&ab_channel=FrankDelporte

## Run the examples
- Install the Kotlin Notebooks plugin in IntelliJ
- You need an OpenAI API key

### LangChain4J in module: `/langchain4j`

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

### SpringAI in module: `/spring-ai`

#### Run SpringAI sample application
- Provide the OpenAI key OPENAI_API_KEY in the environment
- Start docker-compose: docker-compose up
- Run application: [SpringAIDemoApplication.kt](spring-ai/src/main/kotlin/dev/example/SpringAIDemoApplication.kt)
  - The first time it takes a while since the vector db is ingested using the spring-ai way
- Run Samples in the notebook: [spring-ai-demo.ipynb](spring-ai/spring-ai-demo.ipynb)


#### Run CLI Chat Client
- Run CLI Chat Client `main` in  [ChatClient.kt](spring-ai/src/test/kotlin/dev/example/ChatClient.kt) in the `spring-ai` module
- Type `help` to see the available commands
