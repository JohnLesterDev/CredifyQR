<div align="center">
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
  <a href="#introduction--description">Introduction</a> ●
  <a href="#features">Features</a> ●
  <a href="#system-architecture">System Architecture</a> ●
  <a href="#installation--user-guide">Installation & User Guide</a> ●
  <a href="#acknowledgements">Acknowledgements</a> ●
  <a href="#contacts">Contacts</a>
</div>

<br><br>

## Introduction & Description
The integrity of academic credentials is under constant threat from a globalized market of document forgery that undermines the value of legitimate degrees. Registrar offices are currently bogged down by manual verification requests that are slow, error-prone, and inefficient. 

CredifyQR is a web-based prototype designed to automate the generation and validation of academic credentials. The system addresses the fundamental disconnect between document issuance and authenticity validation. By automating document issuance and providing instant, tamper-evident validation, CredifyQR modernizes institutional workflows while keeping the core database isolated and secure. This eliminates the "request and wait" cycle that delays students' professional mobility, and provides external verifiers a low-friction process to confirm qualifications instantly.

## Features
- **Instant QR-Based Verification:** Generates academic credentials embedded with QR codes containing high-entropy validation tokens. External verifiers can scan these codes to access a public verification page instantly, requiring no account creation or proprietary software.
- **Role-Based Access Control (RBAC):** Enforces a strict hierarchical workflow involving Students, Registrar Staff, and the Campus Director. Credentials must pass through an institutional authorization stage before official issuance.
- **Hexagonal Architecture:** Decouples core business logic from external interfaces. Utilizing Ports and Adapters, the system isolates identity management from core document processing, ensuring high maintainability and error-resilience.
- **Bounded Contexts:** Adopts Domain-Driven Design principles to strictly separate the *Identity Context* (user authentication and authorization) from the *Credential Context* (document lifecycle, token generation, and QR creation).
- **Lightweight Infrastructure:** Developed using Java, the Javalin web framework, and SQLite, resulting in a highly efficient, server-independent prototype tailored for localized institutional deployment.

## System Architecture
CredifyQR is engineered using Java, the lightweight Javalin web framework, and SQLite for local data storage.

The structural foundation relies on **Hexagonal Architecture (Ports and Adapters)** to separate core domain logic from external interfaces (database, web server), ensuring high maintainability and system fault tolerance. 

To manage institutional data complexity, operations are isolated into strict **Bounded Contexts**:
* **Identity Context:** Encapsulates all user authentication, authorization, and Role-Based Access Control (RBAC) logic.
* **Credential Context:** Operates independently to handle the document lifecycle, data encoding, unique validation token generation, and the public verification query endpoint.

Third-party validation operates on a **Token-Based Verification Model**. Documents are stamped with embedded QR codes containing high-entropy, cryptographically hard-to-guess tokens. External scanners are directed to a tightly restricted public verification portal to view validity status instantly, requiring zero account creation.

## Installation & User Guide

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher.
* **Build Tool:** Gradle 8.5+.

### Installation Steps
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/JohnLesterDev/CredifyQR.git](https://github.com/JohnLesterDev/CredifyQR.git)
    cd CredifyQR
    ```
2.  **Environment Configuration:**
    Create a `.env` file in the `credifyqr` directory. You must supply a secure JWT secret.
    ```env
    HOST=0.0.0.0
    PORT=8080
    JWT_SECRET=YourSuperSecretKeyHereMustBe32Chars+
    DB_URL=jdbc:sqlite:credifyqr.db
    ```
3.  **Build and Execute:**
    Run the application utilizing the Gradle wrapper.
    ```bash
    ./gradlew run
    ```
4.  **Access:** Navigate to `http://localhost:8080` in your browser.

### User Guide & Workflow
CredifyQR enforces strict hierarchical workflows through Role-Based Access Control (RBAC). 

#### 1. System Administrator
* **Initial Setup:** Access the `/admin` portal. Authenticate using the default bootstrap credentials (`sysadmin` / `admin123`).
* **Domain Configuration:** Immediately configure the **Institutional Domain** (e.g., `ctu.edu.ph`). The system restricts QR generation and staff account claiming until this domain is locked.
* **Provisioning:** Stage Employee identities (Campus Directors and Registrar Staff) via the System Overview panel.

#### 2. Registrar Staff
* **Account Claiming:** Navigate to the `/admin` portal and use the Secure Staff Claim feature utilizing the Employee ID staged by the SysAdmin, your birthdate, and a valid institutional email.
* **Student Provisioning:** Stage new student accounts via the dashboard using the student's ID, Name, and Birthdate.
* **Document Processing:** Monitor the "Pending Uploads" queue. Upload physical documents requested by students. The system strictly permits and verifies `.pdf` files.

#### 3. Student
* **Account Claiming:** Access the root `/login` portal. Claim your staged identity using your 8-digit Student ID and Birthdate to generate a temporary secure password.
* **Request Credentials:** Select and request necessary academic documents (`LEDGER`, `COR`, `GRADES`).
* **Retrieval:** Once approved and stamped by the administration, download your verifiable digital credential directly from the dashboard.

#### 4. Campus Director
* **Authorization:** Approve claimed Registrar Staff accounts to grant them write-access to the system.
* **Document Signature:** Review pending documents uploaded by Registrars. Approving a document executes the PDF Stamper Port, cryptographically attaching the unique QR code and officially issuing the document.

#### 5. External Verifiers
* **Instant Verification:** Scan the QR code embedded on the credential. You will be routed immediately to the system's public verification endpoint (`/verify/{id}`) confirming authenticity, student name, and issue date without requiring system login credentials.

## Acknowledgements
This capstone project is presented to the Faculty of the College of Technology, Management, and Entrepreneurship (CTME Department) at **Cebu Technological University - Consolacion Campus** in partial fulfillment of the requirements for the degree Bachelor of Science in Information Technology.

**Development Team:**
* Donita C. Pilapil
* Johana B. Aquino
* Andrew Orriesga
* John Lester L. Licayan
* John Andrew B. Stewart
* Danniela Ysabelle Y. Rabadon

We extend our deepest gratitude to our project adviser, **Prof. Jamaica C. Burgos**, for her invaluable guidance and rigorous academic oversight.

## Contacts
For inquiries regarding the system architecture, technical implementation, or to report any issues, please contact the development lead at [johnlesterincbusiness@gmail.com](mailto:johnlesterincbusiness@gmail.com).