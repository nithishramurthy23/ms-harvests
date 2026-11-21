# Search, filters and sorting

The shop page and the search box at the top both use one search on the server, `GET /api/products/search`. This page explains what it does, in the order a customer would notice it.

## What customers can do

- **Search by word.** "carrot", "Carrots" and "CARROT" find the same things. Plurals like "tomatoes" and "strawberries" work too.
- **Search with several words.** Every word has to match, so "carrot cake" finds the cake mix but not plain carrots.
- **Spelling mistakes.** If nothing matches "carot", the page asks "Did you mean carrot?". Click it to search again.
- **Pick an aisle.** Each aisle chip shows how many results it has, for example "Fruits 6", so customers can see where to look before they click.
- **Set a price range.** Type a lowest and highest price. The boxes show the cheapest and dearest match as a hint. The filter uses today's price, including any discount, so it matches what's on the card.
- **In stock only.** Hides sold-out products. Without it, sold-out products are still shown, but always at the end.
- **Sort.** Best match, Popular, Price low to high, Price high to low, Biggest discount, or Name.
- **Pages.** 24 products per page, with Previous and Next.

Everything is kept in the address bar, for example:

```
/products?q=carrot&aisle=vegetables&max=100&instock=1&sort=price-asc&page=2
```

so a filtered page can be bookmarked, shared or opened in a new tab, and the browser's back button works.

## How "best match" decides the order

Each product gets points for every word typed:

| Where the word is found | Points |
|---|---|
| A whole word in the name ("carrot" in "Baby Carrot") | 30 |
| The start of a word in the name ("carr" in "Carrot") | 20 |
| Anywhere in the name | 12 |
| The aisle ("fruit" for Fruits) | 8 |
| The description | 3 |

If any word isn't found anywhere, the product isn't shown. Typing a product's exact name adds 50 extra points, so it comes first. Ties are broken by name.

With no search words, "best match" means **Popular**: the products sold most in the last 30 days, not counting cancelled orders.

## How "Did you mean" works

When nothing matches, the server compares what was typed with every product name and every word in those names. It counts how many single letters would have to be added, removed or changed to turn one into the other (the "edit distance"). "carot" to "carrot" is 1. If the closest name is 1 letter away (for words of 4 letters or fewer) or 2 letters away (for longer ones), it's offered as the suggestion.

## The API

```
GET /api/products/search
```

Anyone can call it, no login needed. Every parameter is optional.

| Parameter | Example | Meaning |
|---|---|---|
| `q` | `carrot cake` | Words to search for |
| `category` | `vegetables` | Aisle slug |
| `minPrice`, `maxPrice` | `50`, `150` | Today's price range, in rupees |
| `inStock` | `true` | Only products that are in stock |
| `sort` | `price-asc` | `relevance` (default), `popular`, `price-asc`, `price-desc`, `discount`, `name` |
| `page` | `0` | Page number, starting at 0 |
| `size` | `24` | Products per page, at most 60 |

Reply:

```json
{
  "items": [ { "id": 207, "name": "Fresh Carrots", "price": 45, "stock": 20, "inStock": true, "...": "..." } ],
  "page": 0,
  "size": 24,
  "totalItems": 3,
  "totalPages": 1,
  "categoryCounts": { "vegetables": 2, "bakery": 1 },
  "lowestPrice": 45,
  "highestPrice": 250,
  "didYouMean": null
}
```

`categoryCounts` is worked out with every filter except the aisle, so it tells you what you'd get by switching aisle.

## Why it runs in Java and not in SQL

The price a customer sees depends on today's discount rules, which are calculated in Java (`ProductService`). A price filter or sort written in SQL would use the stored price and get discounted products wrong. So the server loads the visible catalogue (a few hundred products), and filters, scores and sorts it in memory, which takes well under a millisecond.

This stops being a good idea at tens of thousands of products. The next step then would be to store today's price in a column, updated when the discount rules change, and search in SQL with a full-text index, or move to a search engine such as OpenSearch. The reasoning is in [design decision 13](design-decisions.md#13-search-in-memory-with-todays-prices).

## Where the code is

| File | What it does |
|---|---|
| `service/ProductSearchService.java` | Matching, scoring, filters, sorting, pages and "did you mean" |
| `controller/CatalogController.java` | The `/api/products/search` endpoint |
| `repository/OrderItemRepository.java` | Units sold in the last 30 days, for "Popular" |
| `src/api/search.js` | Calls the endpoint from the browser |
| `src/pages/ProductsPage.jsx` | The shop page: aisle chips, filters, sort and pages |
| `src/components/SearchBar.jsx` | Suggestions while typing |
| `ProductSearchServiceTest`, `CatalogIntegrationTest` | Backend tests |
| `ProductsPage.test.jsx` | Frontend tests |
