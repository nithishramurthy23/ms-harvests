-- DATABASE & USERS TABLE SETUP
CREATE DATABASE IF NOT EXISTS farm_fresh;
USE farm_fresh;

-- Drop tables in correct order due to foreign key dependencies
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS discount_rules;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- USERS TABLE
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  address TEXT, -- Nullable as per entity
  phone VARCHAR(20), -- Nullable as per entity
  role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
  otp_code VARCHAR(6), -- OTP code (6 digits)
  otp_expiry TIMESTAMP, -- Correctly stores Instant as UTC timestamp
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  failed_attempts INT NOT NULL DEFAULT 0, -- Added to track OTP retry attempts
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CATEGORIES TABLE
CREATE TABLE categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  icon_class VARCHAR(50)
);

-- PRODUCTS TABLE
CREATE TABLE products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NOT NULL,
  weight VARCHAR(50),
  image_url VARCHAR(255),
  category_id BIGINT,
  stock_quantity INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_products_category_id ON products(category_id);

-- DISCOUNT RULES TABLE
CREATE TABLE discount_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
  category_id BIGINT NOT NULL,
  discount_percent DECIMAL(5,2) NOT NULL CHECK (discount_percent BETWEEN 0 AND 100),
  active BOOLEAN DEFAULT true,
  UNIQUE (day_of_week, category_id),
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE INDEX idx_discount_category_id ON discount_rules(category_id);

-- CART TABLE
CREATE TABLE cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_user_id ON cart(user_id);

-- CART ITEMS TABLE
CREATE TABLE cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL DEFAULT 1,
  added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  original_price DECIMAL(10,2) NOT NULL,
  discounted_price DECIMAL(10,2) NOT NULL,
  discount_applied BOOLEAN DEFAULT false,
  FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

-- ORDERS TABLE
CREATE TABLE orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  total_amount DECIMAL(10,2) NOT NULL,
  status ENUM('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  shipping_address TEXT NOT NULL,
  shipping_name VARCHAR(255),
  shipping_city VARCHAR(100),
  shipping_postal_code VARCHAR(20),
  shipping_phone VARCHAR(20),
  shipping_email VARCHAR(100),
  payment_method VARCHAR(50) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_orders_user_id ON orders(user_id);

-- ORDER ITEMS TABLE
CREATE TABLE order_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INTEGER NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  product_price DECIMAL(10,2) NOT NULL,
  product_weight VARCHAR(50),
  product_image_url VARCHAR(255),
  discount_applied BOOLEAN DEFAULT false,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- INITIAL DATA

-- Categories
INSERT INTO categories (name, description) VALUES
('Vegetables', 'Fresh seasonal vegetables'),
('Fruits', 'Sweet and juicy fruits'),
('Herbs & Spices', 'Fresh and dried herbs'),
('Dairy Products', 'Fresh dairy and alternatives'),
('Eggs', 'Farm fresh eggs'),
('Honey & Jams', 'Sweet natural preserves');

-- Discount rules (match src/utils/discounts.js, which is what customers see)
INSERT INTO discount_rules (day_of_week, category_id, discount_percent)
VALUES
('MONDAY', (SELECT id FROM categories WHERE name = 'Vegetables'), 15),
('TUESDAY', (SELECT id FROM categories WHERE name = 'Fruits'), 10),
('WEDNESDAY', (SELECT id FROM categories WHERE name = 'Dairy Products'), 12),
('THURSDAY', (SELECT id FROM categories WHERE name = 'Herbs & Spices'), 20),
('FRIDAY', (SELECT id FROM categories WHERE name = 'Honey & Jams'), 18);

-- Products
-- IDs match src/data/products.js so the frontend cart and the backend agree.
-- Prices here are what customers are charged; keep them in sync with the frontend.
INSERT INTO products (id, name, description, price, original_price, weight, image_url, category_id, stock_quantity)
VALUES
(101, 'Ooty Plums', NULL, 199.00, 199.00, '1 kg', 'https://images.hollandandbarrettimages.co.uk/health-hub/Batch1/plums-wood-730486915.jpg',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(102, 'Organic Bananas', NULL, 49.00, 49.00, '1 dozen', 'https://images.unsplash.com/photo-1603833665858-e61d17a86224',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(103, 'Fresh Jamun Fruits', NULL, 60.00, 60.00, '1 kg', 'https://images.healthshots.com/healthshots/en/uploads/2022/05/18184712/jamun-1600x900.jpg',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(104, 'Ooty Oranges', NULL, 149.00, 149.00, '1 kg', 'https://images.unsplash.com/photo-1582979512210-99b6a53386f9',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(105, 'Fresh Pomegranate', NULL, 199.00, 199.00, '1 kg', 'https://images.unsplash.com/photo-1541344999736-83eca272f6fc',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(108, 'Fresh Strawberries', NULL, 199.00, 199.00, '250g', 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6',
 (SELECT id FROM categories WHERE name = 'Fruits'), 100),
(202, 'Organic Potatoes', NULL, 79.00, 79.00, '1 kg', 'https://5.imimg.com/data5/ANDROID/Default/2023/1/CF/SX/MJ/140128550/product-jpeg-500x500.jpg',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(203, 'Garlic', NULL, 100.00, 100.00, '1 kg', 'https://westernliving.ca/wp-content/uploads/Garlic_Feature.jpg',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(204, 'Beetroot', NULL, 59.00, 59.00, '500g', 'https://5.imimg.com/data5/HV/YI/MY-12413219/capture-500x500.png',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(205, 'Fresh Spinach', NULL, 39.00, 39.00, 'bunch', 'https://iheartumami.com/wp-content/uploads/2022/08/Taiwanese-spinach.jpg',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(206, 'Cauliflower', NULL, 49.00, 49.00, 'piece', 'https://images.unsplash.com/photo-1568584711075-3d021a7c3ca3',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(207, 'Fresh Carrots', NULL, 45.00, 45.00, '500g', 'https://5.imimg.com/data5/ANDROID/Default/2020/10/JW/PL/XD/66104455/product-jpeg-500x500.jpg',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(208, 'Green Peas', NULL, 59.00, 59.00, '500g', 'https://images.unsplash.com/photo-1591279222235-27c75538636d',
 (SELECT id FROM categories WHERE name = 'Vegetables'), 100),
(601, 'Fresh Coriander', NULL, 19.00, 19.00, 'bunch', 'https://img.freepik.com/premium-photo/fresh-bunch-coriander-leaves_525574-13939.jpg',
 (SELECT id FROM categories WHERE name = 'Herbs & Spices'), 100),
(602, 'Curry Leaves', NULL, 29.00, 29.00, 'bunch', 'https://thumbs.dreamstime.com/b/bunch-fresh-indian-curry-leaves-leaf-wooden-background-185982662.jpg',
 (SELECT id FROM categories WHERE name = 'Herbs & Spices'), 100),
(603, 'Fresh Mint', NULL, 29.00, 29.00, 'bunch', 'https://plus.unsplash.com/premium_photo-1673264303561-de2ab31df03c',
 (SELECT id FROM categories WHERE name = 'Herbs & Spices'), 100),
(701, 'Fresh Cow Milk', NULL, 62.00, 62.00, '1 L', 'https://images.unsplash.com/photo-1550583724-b2692b85b150',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(702, 'Organic Curd', NULL, 49.00, 49.00, '400g', 'https://shorturl.at/AxIyk',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(703, 'Fresh Paneer', NULL, 299.00, 299.00, '500g', 'https://shorturl.at/ZX9wD',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(704, 'Pure Ghee', NULL, 599.00, 599.00, '1 L', 'https://shorturl.at/mJSbF',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(705, 'Greek Yogurt', NULL, 89.00, 89.00, '400g', 'https://shorturl.at/ipord',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(706, 'Butter', NULL, 249.00, 249.00, '500g', 'https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d',
 (SELECT id FROM categories WHERE name = 'Dairy Products'), 100),
(801, 'Farm Fresh Eggs', NULL, 89.00, 89.00, '6 pcs', 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f',
 (SELECT id FROM categories WHERE name = 'Eggs'), 100),
(802, 'Organic Eggs', NULL, 119.00, 119.00, '6 pcs', 'https://images.unsplash.com/photo-1598965675045-45c5e72c7d05',
 (SELECT id FROM categories WHERE name = 'Eggs'), 100),
(803, 'Duck Eggs', NULL, 99.00, 99.00, '6 pcs', 'https://shorturl.at/A8MiL',
 (SELECT id FROM categories WHERE name = 'Eggs'), 100),
(804, 'Brown Eggs', NULL, 109.00, 109.00, '6 pcs', 'https://images.unsplash.com/photo-1587486913049-53fc88980cfc',
 (SELECT id FROM categories WHERE name = 'Eggs'), 100),
(1101, 'Raw Honey', NULL, 299.00, 299.00, '500g', 'https://images.unsplash.com/photo-1558642452-9d2a7deb7f62',
 (SELECT id FROM categories WHERE name = 'Honey & Jams'), 100),
(1102, 'Mango Jam', NULL, 199.00, 199.00, '250g', 'https://shorturl.at/RoxdB',
 (SELECT id FROM categories WHERE name = 'Honey & Jams'), 100),
(1103, 'Mixed Fruit Jam', NULL, 179.00, 179.00, '250g', 'https://shorturl.at/hXJJJ',
 (SELECT id FROM categories WHERE name = 'Honey & Jams'), 100),
(1104, 'Grape Jam', NULL, 349.00, 349.00, '500g', 'https://tinyurl.com/49vvthew',
 (SELECT id FROM categories WHERE name = 'Honey & Jams'), 100),
(1105, 'Strawberry Jam', NULL, 189.00, 189.00, '250g', 'https://shorturl.at/lrHLV',
 (SELECT id FROM categories WHERE name = 'Honey & Jams'), 100);
