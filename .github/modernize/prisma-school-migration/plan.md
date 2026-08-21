# Modernization Plan: Prisma School Cloud Migration to Azure

**Project**: prisma-school

---

## Technical Framework

- **Language**: Java 25
- **Framework**: Hibernate ORM / Jakarta Persistence (JPA)
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL (local)
- **Key Dependencies**: Hibernate, Logback, Java NIO/IO file operations

---

## Overview

This migration plan modernizes the Prisma School application for cloud deployment on Azure. The application currently uses local PostgreSQL, file-based logging, local file storage, and contains JNI native code dependencies. The new architecture will:

- **Secure database access** through managed identity authentication to Azure Database for PostgreSQL
- **Adopt cloud-native logging** by migrating from file-based appenders to console output for container aggregation
- **Externalize configuration** using environment variables for cloud deployment flexibility
- **Enable containerization** with Docker for deployment to Azure Container Apps and AKS
- **Migrate file storage** from local file system to Azure Storage Account File Shares for scalability
- **Remove localhost dependencies** by migrating all resource access to Azure cloud services
- **Ensure encoding consistency** across all Java I/O operations
- **Handle native code packaging** within container images for JNI support

The migration is phased to first address foundational changes (configuration, logging, file storage), followed by containerization, and finally cloud service integration.

---

## Migration Impact Summary

| Component | Current State | Target State | Impact |
|-----------|---------------|--------------|--------|
| Database Access | Local PostgreSQL + Password | Azure PostgreSQL + Managed Identity | Security improvement, cloud-native auth |
| Configuration | Code/properties files | Environment variables | Enhanced security, cloud flexibility |
| Logging | File-based (logback) | Console output | Cloud-native observability |
| File Storage | Local file system (Java I/O, NIO) | Azure Storage File Shares | Scalable, persistent cloud storage |
| Containerization | No Docker | Containerized with Dockerfile | AKS, Container Apps support |
| Native Code | JNI dependencies | JNI in container | Container support with native libs |
| Resource Access | Localhost connections | Azure services | Cloud-native architecture |
| Jakarta JPA | Current version | Azure-compatible version | Cloud readiness |

---

## Modernization Tasks

### Phase 1: Foundation & Configuration

**Task 1.1: Secure Azure Database for PostgreSQL with Managed Identity** (001-transform-migration-mi-postgresql)
- Migrate database authentication from password-based to managed identity
- Update Spring Cloud Azure dependencies
- Remove hardcoded database credentials from configuration
- Enable passwordless PostgreSQL connections to Azure Database for PostgreSQL

**Task 1.2: Configure System Environment Variables** (002-transform-configuration-management)
- Externalize all configuration values to environment variables
- Remove hardcoded configuration from code and properties files
- Document required environment variables for cloud deployment
- Support both local and cloud environment configurations

**Task 1.3: Migrate to Console Logging** (008-transform-logging-console)
- Remove file appenders from logback configuration
- Configure all logging to stdout/console
- Ensure proper log formatting for cloud log aggregation services
- Maintain existing log levels and categories

### Phase 2: Storage & File System Migration

**Task 2.1: Migrate to Azure Storage Account File Share mounts** (006-transform-local-files-azure-storage)
- Replace java.io and java.nio file operations with Azure Storage File Share paths
- Update file path references to use mounted Azure storage paths
- Ensure file operations work with cloud-based storage
- Handle authentication and permissions for file access

**Task 2.2: Migrate the Local Resource to Azure** (007-transform-localhost-resources)
- Remove localhost resource dependencies
- Update JDBC connection strings to use Azure endpoints
- Migrate local service calls to Azure cloud services
- Ensure all resource access uses cloud endpoints

### Phase 3: Containerization & Runtime

**Task 3.1: Containerize Java Application for Container Readiness** (004-containerization)
- Create Dockerfile with appropriate base image
- Implement multi-stage builds for optimization
- Ensure all dependencies are properly packaged
- Optimize image size and security
- Enable deployment to Azure Container Apps and AKS

**Task 3.2: Build Native Process into Container Image** (005-transform-jni-native-code)
- Package native code and JNI libraries in container image
- Configure native library paths in container environment
- Ensure JNI code loads correctly at runtime
- Handle platform-specific native dependencies

### Phase 4: API & Standards Compliance

**Task 4.1: Migrate Jakarta JPA to Azure** (003-transform-jakarta-jpa-migration)
- Update Jakarta Persistence configuration for Azure compatibility
- Ensure JPA mappings work with Azure databases
- Validate ORM functionality in cloud environment
- Update query syntax if needed for Azure SQL/PostgreSQL

**Task 4.2: Check Encoding in the Code** (009-validate-encoding)
- Review Java I/O constructor encoding specifications
- Verify UTF-8 encoding is used consistently
- Document any encoding-specific requirements
- Ensure cross-platform encoding compatibility

---

## Success Criteria

- All code successfully compiles with Maven
- Unit tests pass for each migration task
- Container image builds successfully
- Application starts correctly in container environment
- Database connections use managed identity authentication
- No hardcoded credentials in source code or configuration
- All file I/O uses Azure Storage paths
- Logging output only to console (no files)
- JNI native code functions properly in container

---

## Timeline & Effort Estimation

Based on assessment findings, total effort: **99 story points**

- Phase 1 (Configuration & Logging): 6 story points
- Phase 2 (Storage & File System): 11 story points
- Phase 3 (Containerization & Native Code): 11 story points
- Phase 4 (API & Standards): 6 story points
- Testing & Validation: 15 story points
- Deployment Preparation: 15 story points

---

## Dependencies & Prerequisites

- Azure subscription with appropriate permissions
- Azure Database for PostgreSQL instance
- Azure Storage Account for file storage
- Docker runtime for local containerization testing
- Maven 3.9 or higher
- Java 25 development environment

---

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| JNI compatibility in containers | Test native code in container early, document all native dependencies |
| PostgreSQL migration issues | Maintain backward compatibility, plan rollback strategy |
| File system path changes | Update all file path handling, comprehensive testing with Azure Storage |
| Configuration externalization | Implement configuration validation, test all environment combinations |

---

## Notes

- This plan focuses on the 9 selected assessment categories with solutions
- Additional modernization tasks may be identified during implementation
- All tasks are designed to be independently testable
- Cloud service endpoints will be configured during deployment phase
- Managed identity must be enabled in Azure before database migration

---

## Open Questions & Questionnaire

- [ ] Azure PostgreSQL instance endpoint (to be provided during execution)
- [ ] Azure Storage Account connection details (to be provided during execution)
- [ ] Target Azure Container Registry location
- [ ] Required environment variables list (to be documented in task 1.2)
