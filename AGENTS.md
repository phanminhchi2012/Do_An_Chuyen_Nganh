# Project map

- Backend: Java 21, Spring Boot 4.1.1, Maven, PostgreSQL. Frontend: React/Vite trong `frontend/`.
- Entry point: `src/main/java/com/example/scalys/ScalysApplication.java`.
- Product and order features: `models`, `repositories`, `services`, `dtos`, `controllers` dưới `com.example.scalys`; CORS ở `configs/WebConfig.java`.
- Database setup: shared Flyway migrations in `src/main/resources/db/migration/` (`V1__schema.sql`), sample fixture in `src/main/resources/db/sample/V2__seed_products.sql`. `application-dev.properties` and `application-demo.properties` add both Flyway locations; `prod` uses only the shared location from `application.properties`. Dev values come from ignored `local-dev.properties` or environment variables; demo and production use environment variables. Production deployment guide and Nginx/systemd templates live in `DEPLOY.md` and `deploy/`.
- The old `.postgresql-recovery/` portable server, credentials and backups were removed. A future local database setup uses a separate PostgreSQL installation and ignored `local-dev.properties`; never print or commit credentials.
- Frontend: `frontend/src/App.jsx` selects routes; `components/ProductCard.jsx` links catalog cards to `/products/:id`; `pages/ProductDetailPage.jsx` renders the detail; `components/OrderForm.jsx` and `pages/OrderThankYouPage.jsx` implement checkout. `services/api.js`, `products.js`, `orders.js` call the API. Vite proxies `/api` to Spring Boot in development.
- Admin: `frontend/src/pages/AdminProductsPage.jsx` renders `/admin/products`; `pages/AdminOrdersPage.jsx` renders `/admin/orders`; `components/AdminHeader.jsx` is shared navigation. Backend admin routes are in `controllers/AdminProductController.java` and `AdminOrderController.java`.
- Auth: `frontend/src/pages/AdminLoginPage.jsx` handles `/admin/login`; `components/AdminGate.jsx` guards `/admin/*`; `services/auth.js` handles session, CSRF and logout. The Vite `/api` proxy keeps React and Spring Security session cookies on one origin. New local admin hash belongs in ignored `local-dev.properties`; never print or commit it.
- Public `GET /api/products` returns `AVAILABLE` and `HELD`; admin `GET /api/admin/products` returns all. `SOLD` hides a product from the public catalog. Admin APIs require a Spring Security session and return 401 without one. `configs/SecurityConfig.java` owns authorization, CSRF and login/logout; `services/LoginAttemptService.java` locks login for 5 minutes after 5 failures.
- `GET /api/products/{id}` returns details for any status and 404 for a missing ID. The detail page omits the image block when no image URL is set.
- `POST /api/orders` validates a Vietnamese mobile number and delivery address, locks the product row, creates a `NEW` order and changes `AVAILABLE` to `HELD` atomically; a second order gets 409. `GET /api/admin/orders` lists newest first with optional status filter. `PATCH /api/admin/orders/{id}/status` locks the order and product: only `NEW` can become `CONFIRMED` (`SOLD`) or `CANCELLED` (`AVAILABLE`); later attempts get 409.
- Product cards omit the image block when `imageUrl` is null; the five seed products currently have no images.

# Working conventions

- Mọi task mặc định chỉ triển khai và kiểm tra trên máy local. Không thực hiện hoặc chuẩn bị thao tác deploy cho tới khi chủ shop yêu cầu rõ phần deploy. Giữ nguyên cổng backend 2012 và frontend 2003.
- Start with `README.md` and files for the feature being changed; use `rg --files` or targeted `rg` rather than reading the whole repository.
- Keep HTTP handling in controllers, business logic in services, persistence in repositories, and response shapes in DTOs.
- Add only packages that contain code; Spring controllers define routes, so do not create an empty `routes` package.
- Preserve any existing database and backup unless the user explicitly authorizes their deletion. Add new Flyway migration versions for changes; do not edit applied migrations.
- Verify database changes with PostgreSQL and API changes through an HTTP request when the local services are available.
- Các danh sách có thể dài phải phân trang như danh sách sản phẩm: hiện khoảng mục, tổng số, trang hiện tại và nút Trước/Tiếp. Dùng `frontend/src/components/ListPagination.jsx` cho danh sách mới; giữ toàn bộ dữ liệu giỏ hàng/đơn hàng trong phép tính dù giao diện chỉ hiện một trang.
