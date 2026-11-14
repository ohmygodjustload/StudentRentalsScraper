# Why this project exists

## The problem with the public site

[studentrentalslacrosse.com](https://www.studentrentalslacrosse.com) only publicly shows a listing when **both** of these are true:

1. The landlord is paying to promote that listing
2. The unit is currently vacant (or being advertised as available)

If either condition fails, the apartment disappears from the site’s public index and search. The listing page itself usually still exists under a sequential `listing_id` URL. It just isn’t linked from the homepage or search results.

Students typically live in these units for one year at a time. That means a large share of “hidden” pages are still real apartments with real landlords — they just aren’t vacant *right now*, or the landlord isn’t paying to advertise them this week.

## How the scraper exploits that

The pipeline does not scrape the site’s public search. It walks listing IDs from `0` to `N` (default 900), fetches each page, and keeps any HTML that parses as a valid listing.

That produces a dataset of apartments that may or may not be currently vacant, including units that never appear in the public index.

## The strategy: reach out early

Most student leases in this market run through roughly June. Students usually sign or re-sign for the following year around September and October. Landlords typically ask current tenants about renewing in late summer — before the busy “everyone is apartment hunting” rush.

Contacting a landlord **before** that renewal decision is the edge this tool is built for:

- **Best case:** you secure a unit months before most students start looking, because you found the listing while it was still off the public index (or before the landlord started advertising).
- **Worst case:** the current tenants renew. You still made contact. That landlord may own other units that were never listed publicly, and may show you one of those instead.

Even when a scraped listing isn’t available, the output is useful as a **landlord contact list**: landlord name, phone, email, address, price, and bed/bath — enough context to prioritize who to call and what to ask for.

## What “success” looks like

Success is not only “find a vacant listing on the website.” Success is:

1. Building a durable list of local student-housing landlords and their properties
2. Reaching out early enough that “are you renewing?” is still an open question
3. Turning that conversation into either this unit or another unit the landlord controls

The scrape → clean → geocode → enrich pipeline exists to make that contact list accurate, sortable, and easy to revisit each year.
