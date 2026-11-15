# Security Policy

## Supported Versions

The following versions of Sobji Dokan are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

We take the security of Sobji Dokan seriously. If you believe you have found a
security vulnerability, please report it to us responsibly.

### How to Report

Please do not disclose security vulnerabilities in public issues or pull
requests. Instead, send a report to the maintainers with the following details:

- A clear description of the vulnerability
- Steps to reproduce the issue
- The impact of the vulnerability (e.g., data exposure, unauthorized access)
- Any suggested mitigation or fix

### What to Expect

1. We will acknowledge receipt of your report within 5 business days.
2. We will investigate the issue and provide an initial assessment within 10
   business days.
3. We will keep you informed of progress throughout the remediation process.
4. Once the issue is resolved, we will credit you in the release notes (unless
   you prefer to remain anonymous).

### Scope

This security policy covers the Sobji Dokan Android application, including:

- The application source code
- The local Room database
- Android DataStore preferences
- The backup and restore functionality

### Out of Scope

The following are out of scope for this security policy:

- Issues in third-party libraries or dependencies
- Issues related to device-level security (e.g., rooted devices, unlocked
  bootloaders)
- Social engineering attacks
- Issues requiring physical access to the device

## Security Best Practices for Users

To keep your data secure when using Sobji Dokan:

- Enable the PIN lock feature to protect app access
- Regularly back up your data using the built-in backup manager
- Keep your device and the app updated to the latest version
- Avoid installing the app from untrusted sources
- Do not share your device with untrusted individuals while the app is unlocked

## Disclosure Policy

We follow a coordinated disclosure process. We ask security researchers to:

- Give us reasonable time to investigate and fix the issue before public
  disclosure
- Avoid accessing or modifying user data beyond what is necessary to
  demonstrate the vulnerability
- Avoid actions that could disrupt service for other users

In return, we will:

- Acknowledge your report promptly
- Work with you to understand and resolve the issue
- Credit you for your contribution (with your permission)
- Notify you before publishing any fix or advisory
