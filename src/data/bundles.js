// src/data/bundles.js
// Ready-made baskets. Each one is priced from the products it contains
// (see utils/bundleUtils.js) and is split back into products at checkout.
export const bundles = [
  {
    id: 1,
    name: "Weekly Vegetable Basket",
    blurb: "Potatoes, garlic, beetroot, spinach, cauliflower, carrots and peas. Enough for a family of four.",
    image:
      "https://thumbs.dreamstime.com/b/selection-includes-carrot-potato-cucumber-tomato-cabbage-lettuce-beetroot-onion-garlic-radish-dill-parsley-group-fresh-raw-243748746.jpg",
    category: "vegetables",
    bundleItems: [201, 202, 203, 204, 205, 206, 207, 208],
  },
  {
    id: 2,
    name: "Hill Fruit Box",
    blurb: "Ooty plums and oranges, jamun, pomegranate, bananas and strawberries.",
    image:
      "https://static.vecteezy.com/system/resources/previews/000/737/013/large_2x/fruits-mango-lemon-plum-grape-pear-orange-apple-banana-strawberry-photo.jpg",
    category: "fruits",
    bundleItems: [101, 102, 103, 104, 105, 106, 108],
  },
  {
    id: 3,
    name: "Egg Sampler",
    blurb: "One box each of farm, organic, duck and brown eggs. 24 eggs in all.",
    image:
      "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?ixlib=rb-4.0.3",
    category: "eggs",
    bundleItems: [801, 802, 803, 804],
  },
  {
    id: 4,
    name: "Honey & Jam Box",
    blurb: "A jar of raw honey with four fruit jams.",
    image: "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62",
    category: "honey-&-jams",
    bundleItems: [1101, 1102, 1103, 1104, 1105],
  },
  {
    id: 5,
    name: "Dairy Week Pack",
    blurb: "Milk, curd, paneer, ghee, yogurt and butter for the week.",
    image:
      "https://images.unsplash.com/photo-1550583724-b2692b85b150?ixlib=rb-4.0.3",
    category: "dairy-products",
    bundleItems: [701, 702, 703, 704, 705, 706],
  },
];
