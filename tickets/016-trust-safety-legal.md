---
id: 016
title: Trust, safety, and legal compliance
area: marketplace
priority: low
status: open
---

# Trust, safety, and legal compliance

## Context

Rental marketplaces attract scams and are covered by fair housing law. Both have to be handled before a public launch. Part of epic 011.

## Acceptance criteria

- Fair housing review: listing text checks for discriminatory wording, and search and ranking features do not filter or score by protected characteristics or demographic proxies. Get the review done by someone qualified.
- Landlord verification step (for example ownership or management proof, or a manual review) before listings go live.
- Scam controls: report-listing button, rate limits on inquiries, admin moderation queue, and warnings about wire transfers and deposits paid before a viewing.
- Terms of service, privacy policy, and data-retention policy.
- Secure handling of user data (hashed passwords, HTTPS, no secrets in the repo).
- Check local rules in each launch market. NYC in particular has its own listing and broker-fee rules.

## Notes

- Neighborhood "quality" features should rely on objective, address-level data such as building violations, reported crime, and transit access, not demographics. This keeps the product legal and useful.
