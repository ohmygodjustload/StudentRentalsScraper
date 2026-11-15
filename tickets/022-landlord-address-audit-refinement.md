---
id: 022
title: Landlord/address audit refinement and remaining bed/bath conflicts
area: clean
priority: low
status: open
---

# Landlord/address audit refinement and remaining bed/bath conflicts

## Context

`.\srs.ps1 audit` compares the title's landlord and street address with the site's Landlord field and breadcrumb address. On the 11/14/2026 scrape (401 listings) it reported 21 landlord conflicts and 2 address conflicts. None of these change rent, bed/bath, or location data, but most landlord conflicts are not real errors. They fall into three groups:

- **Building or complex name vs. management company:** Lancer Village vs. Wise Properties (3, 4, 5, 818), Three Sixty-Aguilera vs. Three Sixty Real Estate (111), Munson Realty vs. Common Ground Property (889), Benson Properties vs. Benson Management (833), Off-Campus La Crosse vs. Hilton Properties (460).
- **Name variants and site typos for the same company:** 608 Rentals vs. 608 Rental Properties / 608 Rental Properteis (484, 485, 486, 489, 492, 494, 497, 498, 593, 728), Dillaber vs. Dillabar (811), Coleman vs. Colemen (864), EKIM-PT vs. EKIM Properties (775).
- **Address:** 293 has a title typo (`330 21th St N` vs. breadcrumb `330 21st St N`). 542 is likely a false positive because its breadcrumb reads `624 8th St S Up La Crosse WI`.

Separately, seven bed/bath conflicts are still unresolved because their descriptions give no usable evidence: 23, 353, 371, 492, 810, 854, 881. Each needs a look at the listing page or the landlord's own site before an override is added.

## Acceptance criteria

- A landlord alias map (one CSV, for example `data/landlord_aliases.csv` with `alias,canonical`) used by both the audit and `DataCleaner.normalizeLandlord`, so 608 Rentals, Dillaber, Coleman, and EKIM variants compare as `agree` and clean to one name.
- A new landlord category, such as `building_vs_manager`, for known building or complex names (Lancer Village, Canterbury, Badger West). Driven by the same file or a small companion list, not hardcoded.
- Address comparison tolerates ordinal typos (`21th` vs. `21st`) and trailing unit words in the breadcrumb (`Up`, `Down`, `Upper`, `Lower`, `Apt N`), so 293 and 542 compare as `agree` or a distinct `minor_difference` category.
- The audit summary shows the new categories, and its tests cover each group above.
- The seven unresolved bed/bath listings are checked by hand. Add a row to `data/bedbath_overrides.csv` for each confirmed correction and note any that stay unresolved.

## Notes

- Overlaps with ticket 009 (externalize landlord/address maps). Do 009 first or together, so the alias map lives in one place.
- The audit only reads data. Any landlord renaming belongs in the cleaner, not the audit.
- Bed/bath listings already confirmed correct in the site field (title was wrong): 293, 456, 709, 747. They keep the `bedbath_conflict` flag, which is expected.
