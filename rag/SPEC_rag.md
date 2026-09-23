# SPEC: LoanApp FAQ Assistant — Pipeline RAG

## Descripción
Asistente conversacional que responde preguntas sobre las políticas de
préstamos de LoanApp basándose únicamente en los documentos del directorio
`rag/docs/`. NO debe inventar información.

## Stack obligatorio
- **Embeddings:** `HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")` (gratuito, sin API key)
- **Vector store:** Qdrant en localhost:6333 (Docker)
- **LLM:** `ChatAnthropic(model="claude-opus-4-8", temperature=0)`
- **Framework:** LangChain (`langchain`, `langchain-anthropic`, `langchain-qdrant`)

## Funciones a implementar en pipeline.py

### `index_documents(docs_path, collection_name) -> QdrantVectorStore`
- Carga todos los archivos .md de `docs_path` con `DirectoryLoader`
- Divide con `RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)`
- Indexa en Qdrant con los embeddings configurados
- Imprime cuántos chunks se indexaron

### `build_qa_chain(vectorstore) -> Runnable`
- Crea un retriever con `search_kwargs={"k": 4}`
- Usa el prompt del sistema: "Eres el asistente de LoanApp. Responde SOLO 
  basándote en el contexto. Si no está en el contexto, di exactamente:
  'No tengo esa información en la documentación disponible.'"
- Retorna una cadena compatible con `.invoke({"input": pregunta})`

### `ask(chain, question) -> dict`
- Llama a chain.invoke y retorna `{"answer": str, "sources": list[str]}`
- Sources son los `metadata["source"]` de los documentos recuperados

## Criterios de aceptación
- [ ] `python pipeline.py` indexa sin errores con Qdrant corriendo
- [ ] Responde correctamente: "¿Cuál es el score mínimo?" → cita el documento
- [ ] Responde correctamente: "¿Cuánto cuesta un café?" → "No tengo esa información"
- [ ] Sources muestra los archivos de origen de los chunks usados

## Extensión opcional (pipeline_minimal.py)
Reimplementar `ask()` sin LangChain: solo `anthropic` + `qdrant-client`.
