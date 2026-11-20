# Security policy

COMUNIDAD 360 handles community, property and financial data. Please do not open public issues
for vulnerabilities.

## Reporting

Use GitHub Security Advisories in this repository:

**Security → Advisories → Report a vulnerability**

Include:

- affected version or commit;
- steps to reproduce;
- impact and data affected;
- proof of concept, if available;
- suggested remediation.

Do not include real personal, financial or community data.

## Scope

In scope:

- authentication and session handling;
- tenant isolation;
- authorization bypass;
- document or photo upload/download;
- injection and XSS;
- sensitive data exposure;
- backup exposure;
- rate limiting and denial of service.

Out of scope:

- findings that require a compromised device or browser;
- social engineering;
- third-party services without a reproducible issue;
- missing security headers without an exploit path.

## Response

We will acknowledge valid reports, assess severity and coordinate disclosure. No bug bounty is
currently defined. The detailed security model is in [`docs/SECURITY.md`](../docs/SECURITY.md).
