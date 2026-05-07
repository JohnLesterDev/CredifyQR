<div align="center">
  <!-- 
    TODO: Add banner image here once assets are finalized.
    <a href="https://github.com/JohnLesterDev/CredifyQR"><img src=".static/credifyqr_banner.png"></a> 
  -->
  <h1>CredifyQR</h1>
  <p><b>Web-Based Student Credential Issuance and QR-Authentication System</b></p>
  <br>
  <img src="https://img.shields.io/badge/language-Java-red">
  <img src="https://img.shields.io/badge/framework-Javalin-orange">
  <img src="https://img.shields.io/badge/database-SQLite-blue">
  <img src="https://img.shields.io/badge/architecture-Hexagonal-green">
</div>

---
<h2 align="center" id="title-credifyqr">Table of Contents</h2>
<div align="center">
  <a href="#description">Description</a> ●
  <a href="#features">Features</a> ●
  <a href="#system-architecture">System Architecture</a> ●
  <a href="#installation">Installation</a> ●
  <a href="#acknowledgements">Acknowledgements</a> ●
  <a href="#contact-details">Contacts</a>
</div>

<br><br>

## Description

CredifyQR is a web-based prototype designed to automate the generation and validation of academic credentials. Engineered to address the inefficiencies of manual registrar workflows and the rising threat of credential forgery, this system ensures that student qualifications are instantly verifiable in a fast-paced, digital job market.

By automating document issuance and providing a secure, tamper-evident validation portal via embedded QR codes, CredifyQR modernizes institutional operations. It empowers external verifiers (such as employers) to authenticate documents instantly without requiring a system account, while ensuring the core academic database remains strictly isolated and secure.

[back to top](#title-credifyqr)

---

## Features

- **Instant QR-Based Verification:** Generates academic credentials embedded with QR codes containing high-entropy validation tokens. External verifiers can scan these codes to access a public verification page instantly, requiring no account creation or proprietary software.
- **Role-Based Access Control (RBAC):** Enforces a strict hierarchical workflow involving Students, Registrar Staff, and the Campus Director. Credentials must pass through an institutional authorization stage before official issuance.
- **Hexagonal Architecture:** Decouples core business logic from external interfaces. Utilizing Ports and Adapters, the system isolates identity management from core document processing, ensuring high maintainability and error-resilience.
- **Bounded Contexts:** Adopts Domain-Driven Design principles to strictly separate the *Identity Context* (user authentication and authorization) from the *Credential Context* (document lifecycle, token generation, and QR creation).
- **Lightweight Infrastructure:** Developed using Java, the Javalin web framework, and SQLite, resulting in a highly efficient, server-independent prototype tailored for localized institutional deployment.

[back to top](#title-credifyqr)

---

## System Architecture

CredifyQR leverages **Hexagonal Architecture (Ports and Adapters)** to enforce a clean separation of concerns. The core domain logic is insulated from external dependencies, ensuring that changes to the UI routing or database layer do not compromise credential processing rules.

System data and logic are managed within distinct **Bounded Contexts**:
* **Identity Context:** Manages user roles (Student, Registrar, Director), authentication, and security permissions.
* **Credential Context:** Handles the document lifecycle, data encoding, unique token generation, and the public verification endpoint.

[back to top](#title-credifyqr)

---

## Installation

As a Java-based application utilizing an embedded SQLite database, CredifyQR requires minimal local setup. Ensure the Java Development Kit (JDK 17 or higher) is installed on your machine.

1. Clone the repository: `git clone https://github.com/JohnLesterDev/CredifyQR.git`
2. Navigate to the project directory: `cd CredifyQR`
3. Build and run the application using your preferred build tool (e.g., Maven/Gradle) or IDE.
4. Access the local web server at `http://localhost:<configured-port>`.

*(Note: Detailed build instructions and environment variable configurations will be provided upon the stable release.)*

[back to top](#title-credifyqr)

---

## Acknowledgements

This capstone project is presented to the Faculty of the College of Technology, Management, and Entrepreneurship (CTME Department) at **Cebu Technological University - Consolacion Campus** in partial fulfillment of the requirements for the degree Bachelor of Science in Information Technology.

The development of CredifyQR is the result of the collaborative effort of the following team members:

- **Pilapil, Donita C.**
- **Aquino, Johana B.**
- **Orriesga, Andrew**
- **Licayan, John Lester L.**
- **Rabadon, Danniela Ysabelle Y.**

We extend our deepest gratitude to our project adviser, **Prof. Jamaica C. Burgos**, for her invaluable guidance, rigorous academic oversight, and continuous support throughout the research and development lifecycle of this project.

---

## Contact Details

For inquiries regarding the system architecture, technical implementation, or to report any issues, please contact the development lead at [johnlesterincbusiness@gmail.com](mailto:johnlesterincbusiness@gmail.com). 

[back to top](#title-credifyqr)