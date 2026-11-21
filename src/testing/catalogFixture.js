// Catalogue as the backend returns it (GET /api/products), for frontend tests.
export const fixtureProducts = [
  { id: 101, name: "Ooty Plums", price: 199, image: "https://images.hollandandbarrettimages.co.uk/health-hub/Batch1/plums-wood-730486915.jpg", weight: "1 kg", category: "fruits", stock: 20, inStock: true },
  { id: 102, name: "Organic Bananas", price: 49, image: "https://images.unsplash.com/photo-1603833665858-e61d17a86224", weight: "1 dozen", category: "fruits", stock: 20, inStock: true },
  { id: 103, name: "Fresh Jamun Fruits", price: 60, image: "https://images.healthshots.com/healthshots/en/uploads/2022/05/18184712/jamun-1600x900.jpg", weight: "1 kg", category: "fruits", stock: 20, inStock: true },
  { id: 104, name: "Ooty Oranges", price: 149, image: "https://images.unsplash.com/photo-1582979512210-99b6a53386f9", weight: "1 kg", category: "fruits", stock: 20, inStock: true },
  { id: 105, name: "Fresh Pomegranate", price: 199, image: "https://images.unsplash.com/photo-1541344999736-83eca272f6fc", weight: "1 kg", category: "fruits", stock: 20, inStock: true },
  { id: 108, name: "Fresh Strawberries", price: 199, image: "https://images.unsplash.com/photo-1464965911861-746a04b4bca6", weight: "250g", category: "fruits", stock: 20, inStock: true },
  { id: 202, name: "Organic Potatoes", price: 79, image: "https://5.imimg.com/data5/ANDROID/Default/2023/1/CF/SX/MJ/140128550/product-jpeg-500x500.jpg", weight: "1 kg", category: "vegetables", stock: 20, inStock: true },
  { id: 203, name: "Garlic", price: 100, image: "https://westernliving.ca/wp-content/uploads/Garlic_Feature.jpg", weight: "1 kg", category: "vegetables", stock: 20, inStock: true },
  { id: 204, name: "Beetroot", price: 59, image: "https://5.imimg.com/data5/HV/YI/MY-12413219/capture-500x500.png", weight: "500g", category: "vegetables", stock: 20, inStock: true },
  { id: 205, name: "Fresh Spinach", price: 39, image: "https://iheartumami.com/wp-content/uploads/2022/08/Taiwanese-spinach.jpg", weight: "bunch", category: "vegetables", stock: 20, inStock: true },
  { id: 206, name: "Cauliflower", price: 49, image: "https://images.unsplash.com/photo-1568584711075-3d021a7c3ca3", weight: "piece", category: "vegetables", stock: 20, inStock: true },
  { id: 207, name: "Fresh Carrots", price: 45, image: "https://5.imimg.com/data5/ANDROID/Default/2020/10/JW/PL/XD/66104455/product-jpeg-500x500.jpg", weight: "500g", category: "vegetables", stock: 20, inStock: true },
  { id: 208, name: "Green Peas", price: 59, image: "https://images.unsplash.com/photo-1591279222235-27c75538636d", weight: "500g", category: "vegetables", stock: 20, inStock: true },
  { id: 601, name: "Fresh Coriander", price: 19, image: "https://img.freepik.com/premium-photo/fresh-bunch-coriander-leaves_525574-13939.jpg", weight: "bunch", category: "herbs-&-spices", stock: 20, inStock: true },
  { id: 602, name: "Curry Leaves", price: 29, image: "https://thumbs.dreamstime.com/b/bunch-fresh-indian-curry-leaves-leaf-wooden-background-185982662.jpg", weight: "bunch", category: "herbs-&-spices", stock: 20, inStock: true },
  { id: 603, name: "Fresh Mint", price: 29, image: "https://plus.unsplash.com/premium_photo-1673264303561-de2ab31df03c", weight: "bunch", category: "herbs-&-spices", stock: 20, inStock: true },
  { id: 701, name: "Fresh Cow Milk", price: 62, image: "https://images.unsplash.com/photo-1550583724-b2692b85b150", weight: "1 L", category: "dairy-products", stock: 20, inStock: true },
  { id: 702, name: "Organic Curd", price: 49, image: "https://shorturl.at/AxIyk", weight: "400g", category: "dairy-products", stock: 20, inStock: true },
  { id: 703, name: "Fresh Paneer", price: 299, image: "https://shorturl.at/ZX9wD", weight: "500g", category: "dairy-products", stock: 20, inStock: true },
  { id: 704, name: "Pure Ghee", price: 599, image: "https://shorturl.at/mJSbF", weight: "1 L", category: "dairy-products", stock: 20, inStock: true },
  { id: 705, name: "Greek Yogurt", price: 89, image: "https://shorturl.at/ipord", weight: "400g", category: "dairy-products", stock: 20, inStock: true },
  { id: 706, name: "Butter", price: 249, image: "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d", weight: "500g", category: "dairy-products", stock: 20, inStock: true },
  { id: 801, name: "Farm Fresh Eggs", price: 89, image: "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f", weight: "6 pcs", category: "eggs", stock: 20, inStock: true },
  { id: 802, name: "Organic Eggs", price: 119, image: "https://images.unsplash.com/photo-1598965675045-45c5e72c7d05", weight: "6 pcs", category: "eggs", stock: 20, inStock: true },
  { id: 803, name: "Duck Eggs", price: 99, image: "https://shorturl.at/A8MiL", weight: "6 pcs", category: "eggs", stock: 20, inStock: true },
  { id: 804, name: "Brown Eggs", price: 109, image: "https://images.unsplash.com/photo-1587486913049-53fc88980cfc", weight: "6 pcs", category: "eggs", stock: 20, inStock: true },
  { id: 1101, name: "Raw Honey", price: 299, image: "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62", weight: "500g", category: "honey-&-jams", stock: 20, inStock: true },
  { id: 1102, name: "Mango Jam", price: 199, image: "https://shorturl.at/RoxdB", weight: "250g", category: "honey-&-jams", stock: 20, inStock: true },
  { id: 1103, name: "Mixed Fruit Jam", price: 179, image: "https://shorturl.at/hXJJJ", weight: "250g", category: "honey-&-jams", stock: 20, inStock: true },
  { id: 1104, name: "Grape Jam", price: 349, image: "https://tinyurl.com/49vvthew", weight: "500g", category: "honey-&-jams", stock: 20, inStock: true },
  { id: 1105, name: "Strawberry Jam", price: 189, image: "https://shorturl.at/lrHLV", weight: "250g", category: "honey-&-jams", stock: 20, inStock: true },
];

// A CatalogProvider loader that resolves immediately with the fixture.
export const loadFixture =
  (products = fixtureProducts, offer = null) =>
  async () => ({ products, offer });
