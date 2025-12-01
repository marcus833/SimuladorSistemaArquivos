# Simulador de Sistema de Arquivos (com Journaling)

**Disciplina:** Proj de sistema operacional  
**Autores:** Marcus Vinicius e João Pedro  
**Entrega:** Relatório em PDF  
**Repositório GitHub:** https://github.com/marcus833/SimuladorSistemaArquivos
---

## Resumo

Este projeto implementa um **simulador de sistema de arquivos** em Java com funcionalidades básicas de manipulação de arquivos e diretórios (copiar, apagar, renomear, criar/apagar diretórios, listar) e suporte Simulador.FsNode **journaling** para garantir integridade e permitir recuperação após falhas.

---

## Objetivo

Desenvolver um simulador de sistema de arquivos em Java que:

- Implemente operações básicas de arquivo/diretório;
- Use journaling (write-ahead log) para registrar operações antes de aplicá-las;
- Permita execução em modo **shell** (linha de comando) e salve Simulador.FsNode imagem do sistema em um arquivo (`fs.img`) e o journal em (`fs.journal`);
- Forneça mecanismo de recuperação (replay do journal) ao iniciar.

---

## Parte 1 — Introdução ao Sistema de Arquivos com Journaling

**O que é um sistema de arquivos?**  
Um sistema de arquivos organiza e gerencia armazenamento persistente (arquivos e diretórios) e oferece operações (criar, apagar, renomear, listar, copiar). É parte essencial de um SO.

**Journaling:**  
Journaling é uma técnica que registra operações pendentes em um log (journal) antes de aplicá-las à estrutura principal. Caso ocorra uma falha, o sistema pode usar o journal para completar ou desfazer operações, mantendo Simulador.FsNode consistência.

Tipos comuns:
- Write-Ahead Logging (WAL) — registra antes de aplicar.
- Log-structured — estrutura orientada Simulador.FsNode log para toda Simulador.FsNode escrita.

Neste simulador usamos um esquema simples do tipo WAL: gravamos Simulador.FsNode intenção da operação no journal e depois aplicamos; após sucesso marcamos como completo.

---

## Parte 2 — Arquitetura do Simulador

**Estruturas de dados principais**

- `Simulador.FsNode` (abstrata): representa um nó (arquivo ou diretório).
- `Simulador.FileNode`: nó que contém conteúdo (string/bytes) e metadados.
- `Simulador.DirectoryNode`: contém mapa de nomes → `Simulador.FsNode`.
- `Simulador.FileSystemSimulator`: gerencia raiz, operações e persistência (`fs.img`).
- `Simulador.Journal`: gerencia arquivo `fs.journal` com entradas de operações (JSON simples por linha).

**Journaling**

- Cada operação geradora de alteração grava uma entrada no journal:
  - id, tipo (CREATE, DELETE, RENAME, COPY, MKDIR, RMDIR), parâmetros, status (PENDING/COMMIT).
- Após aplicar com sucesso, marcada como COMMIT.
- Na inicialização, o simulador lê `fs.journal` e reaplica entradas PENDING.

---

## Parte 3 — Implementação em Java

### Arquivos principais (exemplos)
- `src/Simulador/Simulador.FileSystemSimulator.java` — classe principal com API das operações.
- `src/Simulador/Simulador.FsNode.java` — nó abstrato.
- `src/Simulador/Simulador.FileNode.java` — representacao de arquivo.
- `src/Simulador/Simulador.DirectoryNode.java` — representacao de diretório.
- `src/Simulador/Simulador.Journal.java` — gerenciamento do journal (append/read/replay).
- `src/Simulador/MainShell.java` — modo shell, lê comandos do usuário.

---

## Parte 4 — Instalação e funcionamento

### Requisitos
- Java 11+ (JDK)
- Sistema operacional qualquer com terminal
- (Opcional) `pandoc` para gerar PDF localmente

### Compilar
1. Organize o código em `src/Simulador/*.java`
2. No diretório raiz:
```bash
javac -d out src/Simulador/*.java
java -cp out Simulador.MainShell
