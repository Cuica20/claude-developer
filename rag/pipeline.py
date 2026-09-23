"""
LoanApp FAQ Assistant — Pipeline RAG
M11 del curso Claude Code for Developers

Instrucciones:
  1. Instalar dependencias: pip install -r requirements.txt
  2. Levantar Qdrant: docker run -d -p 6333:6333 qdrant/qdrant
  3. Crear .env con ANTHROPIC_API_KEY=tu_clave
  4. Completar los TODOs con Claude Code:
     cat SPEC_rag.md | claude "implementa los TODOs en pipeline.py
       según la spec, sin cambiar las firmas de las funciones"
  5. Ejecutar: python pipeline.py
"""

import os
from dotenv import load_dotenv

load_dotenv()

# TODO (paso 1): importar los componentes de LangChain necesarios
# Hint: DirectoryLoader, TextLoader, RecursiveCharacterTextSplitter,
#       QdrantVectorStore, HuggingFaceEmbeddings, ChatAnthropic,
#       create_retrieval_chain, create_stuff_documents_chain, ChatPromptTemplate


def index_documents(docs_path: str = "docs", collection_name: str = "loanapp_faq"):
    """
    Carga los .md de docs_path, divide en chunks, indexa en Qdrant.
    Retorna el QdrantVectorStore listo para consultas.
    """
    # TODO (paso 2): cargar documentos con DirectoryLoader
    # TODO (paso 3): dividir con RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
    # TODO (paso 4): crear QdrantVectorStore.from_documents con HuggingFaceEmbeddings
    raise NotImplementedError("Completa este TODO con Claude Code")


def build_qa_chain(vectorstore):
    """
    Construye y retorna la cadena RAG (retriever + prompt + LLM).
    """
    # TODO (paso 5): crear retriever con k=4
    # TODO (paso 6): definir el prompt del sistema (ver SPEC_rag.md)
    # TODO (paso 7): crear doc_chain y retrieval_chain
    raise NotImplementedError("Completa este TODO con Claude Code")


def ask(chain, question: str) -> dict:
    """
    Hace una pregunta al asistente.
    Retorna {"answer": str, "sources": list[str]}
    """
    # TODO (paso 8): llamar a chain.invoke({"input": question})
    # TODO (paso 9): extraer answer y sources del resultado
    raise NotImplementedError("Completa este TODO con Claude Code")


if __name__ == "__main__":
    print("Indexando documentos...")
    vs = index_documents()

    print("Construyendo cadena QA...")
    chain = build_qa_chain(vs)

    preguntas = [
        "¿Cuál es el credit score mínimo para aprobar un préstamo?",
        "¿Qué pasa si el solicitante tiene menos de 18 años?",
        "¿Cuánto tiempo tarda el proceso de aprobación?",
        "¿Cuánto cuesta un café?",   # Pregunta fuera del dominio — debe decir "no sé"
    ]

    print("\n" + "=" * 60)
    for q in preguntas:
        result = ask(chain, q)
        print(f"\n❓ {q}")
        print(f"💬 {result['answer']}")
        print(f"📎 Fuentes: {result['sources']}")
        print("-" * 60)
