# Expected outcomes after synthetic seed (`05_synthetic_transaction_seed.sql`)

Apply scripts **01 → 02 → 03 → 05**, then optionally run **04** (analytics).

The seed file ends with a small **verification UNION** (`orders`, `order_items`, payments by status). Typical counts:

| Metric | Approx. expectation |
| --- | ---: |
| `orders` rows | **10** (`03` carts untouched as orders; synthetic adds **9** net new headers — actually `03` has 0 orders, so **9** synthetic from O1–O9) |
| `order_items` rows | **14** detail lines across paid + pending tickets O1–O9 |
| `payments_completed` | **8** (O9 stays `INITIATED`) |
| `payments_pending` | **1** |

> Exact inventory after loads differs because **`order_items` triggers deduct stock**; row counts above are stable.

### `04_reporting_queries.sql` — what you should see

1. **Top-selling (paid, last 30 days)** — `Latte Medium` should lead (orders O1 + O6 = **23** units); **`Veg Combo Box`** spikes from O8 (**8** units on one receipt); **`Cold Brew Large`** (**6**) from Carol’s Quad order; **`Chicken Teriyaki`** aggregates across Bob + Dave (**5** total units); **`Medit Bowl`** appears from Alice + Bob (**6** combined).

2. **Heavy users** — **Bob Li** ranks high (paid orders **O2, O6, O7** = **3**); **Alice** (**O1, O3** = **2**); Dave/Carol each have **multiple** completions depending on join windows.

3. **Daily revenue** — at least **one** calendar day reflecting **Dave’s same-day STEM order (O8)** with paid revenue **\$77.33** grouped on `DATE(placed_at)`.

4. `CALL sp_report_restaurant_daily_revenue(CURDATE());` — **STEM Bytes** (and possibly others if your `CURDATE()` matches seeded `DATE(placed_at)`) shows non-zero **`revenue_completed_that_day`** when O8 lands on “today.”

Run this project’s MySQL locally to capture your machine’s literal result grids; JDBC apps then read the same dataset via `db.properties`.
