# TrustDeck - Open-Source Tools for Identity Management in Translational Medicine

[![Version](https://img.shields.io/badge/version-v2.0.1-green)](https://github.com/TrustDeck/trustdeck-backend/releases/tag/v2.0.1)
[![License](https://img.shields.io/badge/License-Apache_2.0-green)](https://github.com/TrustDeck/trustdeck-backend/blob/main/LICENSE)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/TrustDeck/trustdeck-backend)

TrustDeck provides a robust solution for pseudonymization and identity management and features high scalability and an integrated audit trail. Its architecture enables the creation of domain hierarchies with inheritable properties that provide both configuration and customization capabilities. The service allows for defining entities via type definition to maximize customizability. Complemented by a modern REST interface and state-of-the-art web technologies, TrustDeck is well suited for the use by trusted third party personnel as well as integration into data processing pipelines. It is built with Java and SpringBoot, the service integrates with Keycloak for authentication, HikariCP for efficient database connection pooling, jOOQ for type-safe SQL query construction, and PostgreSQL as the backend database.

## Features

- Pseudonymize sensitive data in real-time
- Define and register entities
- Secure user authentication using Keycloak
- High scalability by using PostgreSQL with HikariCP
- Type-safe SQL queries with jOOQ

## Prerequisites

- Java 21 or later
- Maven
- PostgreSQL 15.3 or later
- Keycloak 23.0.5 or later
- jOOQ

## Setup & Installation

Please refer to the official [documentation](https://github.com/TrustDeck/trustdeck-docs) for details on how to setup and run TrustDeck.

## Benchmarking TrustDeck

Please refer to the [benchmarking repository](https://github.com/TrustDeck/ace-benchmark) for details and code.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## How to cite

If you use this software in your research, please cite the accompanying article:

> Müller A, Wündisch E, Wirth FN, Meier Zu Ummeln S, Weber J, Prasser F. **The Advanced Confidentiality Engine as a Scalable Tool for the Pseudonymization of Biomedical Data in Translational Settings: Development and Usability Study.** *J Med Internet Res.* 2025 Nov 5;27:e71822. doi:10.2196/71822. PMID: 41191920.
