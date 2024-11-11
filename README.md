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

#### Data Ingestion
- Start docker-compose: `docker-compose up`
- Open the notebook: `langchain4j-ingest.ipynb`
- Run code snippets to ingest data

### SpringAI in module: `/spring-ai`

#### Run SpringAI sample application
- Provide the OpenAI key OPENAI_API_KEY in the environment
- Run application: [SpringAIDemoApplication.kt](spring-ai/src/main/kotlin/dev/example/SpringAIDemoApplication.kt)

#### Run CLI Chat Client
- Run CLI Chat Client `main` in `ChatClient.kt` in the `spring-ai` module
- Type `help` to see the available commands
