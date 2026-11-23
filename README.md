# Website bán đồ gia dụng

Ứng dụng gồm danh sách sản phẩm công khai và module TC1 (CN01–CN03). Mở Đăng ký hoặc Đăng nhập ở thanh điều hướng. Sau khi đăng nhập, chọn tên tài khoản để sửa hồ sơ và đổi mật khẩu. Quản trị viên chọn Tài khoản nội bộ để tạo tài khoản và đổi vai trò theo QT13.

Các API tài khoản dùng tiền tố `/api/v1` và cookie phiên JSESSIONID. Gửi header `X-CSRF-Protection: 1` cho mọi yêu cầu ghi. Đăng ký dùng POST /customers, đăng nhập dùng POST /sessions, đăng xuất dùng DELETE /sessions/current. Hồ sơ dùng GET hoặc PATCH /me, mật khẩu dùng PUT /me/password. Quản trị tài khoản dùng GET hoặc POST /admin/accounts và PATCH /admin/accounts/{id}/role.

## Giỏ hàng và đặt đơn (CN07)

Ở danh sách sản phẩm, chọn **Thêm vào giỏ**, mở **Giỏ hàng**, chỉnh số lượng và nhập địa chỉ giao. Khách vãng lai có thể chuẩn bị giỏ rồi đăng nhập để quay lại xác nhận. Giỏ được lưu theo tab. Khi đặt thành công, màn hình hiển thị mã đơn, trạng thái “Chờ xử lý” và tổng tiền đã chốt.

`POST /api/v1/orders` yêu cầu phiên khách hàng và header `X-CSRF-Protection: 1`. Ví dụ nội dung JSON:

```json
{"address":"12 Nguyễn Trãi","items":[{"productId":1,"quantity":1}]}
```

Phản hồi thành công có mã HTTP 201 và các trường `orderId`, `status` (`PENDING`), `total` (VND). API trả 400 khi giỏ hoặc địa chỉ sai, 401 khi thiếu phiên, 403 khi sai vai trò và 409 khi sản phẩm thiếu tồn hoặc ngừng bán. Đơn chờ giữ số lượng. Serial được phân bổ ở bước xác nhận đơn thuộc giai đoạn 4.

## Theo dõi và xử lý đơn (CN08–CN09)

Khách hàng mở **Đơn của tôi**, chọn mã đơn để xem chi tiết và hủy khi đơn đang chờ hoặc đã xác nhận. Nhân viên và quản trị viên mở **Xử lý đơn**. Danh sách mặc định hiển thị đơn chờ. Chọn đơn rồi lần lượt xác nhận, chuyển sang đang giao và hoàn tất. Chi tiết đơn hiển thị người xác nhận, các serial đã phân bổ và ngày hết bảo hành nếu có.

Hai nhóm API dùng đường dẫn `/api/v1/me/orders` cho khách và `/api/v1/staff/orders` cho nhân viên hoặc quản trị viên. Dùng GET để xem danh sách (tham số `status` tùy chọn), GET `/{id}` để xem chi tiết, PATCH `/{id}/status` với nội dung như `{"status":"CONFIRMED"}` để chuyển trạng thái. Khách dùng `CANCELLED` khi hủy. PATCH thành công trả 204, xung đột trạng thái hoặc thiếu serial trả 409.

## 1. Tạo cơ sở dữ liệu thủ công

Mở [database/01_init.sql](database/01_init.sql) trong MySQL Workbench và thực thi toàn bộ file một lần trên MySQL 8.0.16 trở lên, với tên cơ sở dữ liệu `btl_gia_dung` còn trống (chưa tồn tại). File tạo database, đủ 13 bảng InnoDB theo ERD và dữ liệu mẫu cho TC1, TC3. Backend đã tắt khởi tạo SQL tự động. Nếu dùng tên database khác, sửa cả hai lệnh CREATE DATABASE và USE trong file, rồi đặt DB_URL tương ứng.

Nếu đã chạy bản 9 bảng, chọn schema hiện có làm mặc định trong Workbench, bôi đen khối từ `BEGIN ERD EXTENSION` đến `END ERD EXTENSION` và thực thi phần đã chọn. Khối này tạo thêm bốn bảng loại đặc trưng, đặc trưng, đặc trưng sản phẩm và đánh giá. Dùng `SHOW TABLES;` để kiểm tra.

Tài khoản ứng dụng mẫu gồm admin@example.test, staff@example.test, dealer@example.test, an@example.test và binh@example.test, mật khẩu demo chung `Demo12345!`. Các dòng trong TAIKHOAN lưu giá trị băm BCrypt. Tài khoản kết nối MySQL ở bước dưới là tài khoản của máy chủ MySQL.

## 2. Chạy backend

Dùng JDK 17, Maven 3.6.3 trở lên. Điền thông tin MySQL trong `backend/mysql.local.properties`. Nếu vừa clone dự án, tạo file này với nội dung sau rồi điền mật khẩu sau dấu `=`.

```properties
mysql.local.username=root
mysql.local.password=
mysql.local.url=jdbc:mysql://localhost:3306/btl_gia_dung
```

Chạy các lệnh sau trong PowerShell từ thư mục dự án.

```powershell
cd backend
mvn spring-boot:run
```

Nếu dùng IntelliJ IDEA, mở backend/pom.xml dưới dạng Maven project, chọn JDK 17 rồi chạy CatalogApplication với thư mục làm việc là thư mục dự án hoặc thư mục backend. Các biến môi trường DB_URL, DB_USER và DB_PASSWORD được ưu tiên khi có thiết lập. Có thể đặt SERVER_PORT để đổi cổng backend.

## 3. Chạy giao diện

Dùng Node.js 22.12 trở lên. Mở cửa sổ PowerShell thứ hai từ thư mục dự án:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Mở URL Vite in ra (thường là http://localhost:5173). Frontend chuyển tiếp /api đến http://127.0.0.1:8080. Nếu đổi cổng backend, đặt API_TARGET trong cửa sổ frontend trước khi chạy Vite. Dùng `npm.cmd run build` để kiểm tra TypeScript và tạo bản build.

## Hợp đồng API giai đoạn 1

`GET /api/v1/products` là API công khai, trả HTTP 200 và mảng JSON theo mã sản phẩm tăng dần. Mỗi phần tử có productId (số nguyên), name (chuỗi), price (số tiền VND), availableQuantity (số nguyên). Chỉ liệt kê sản phẩm có giá bán, kể cả khi đã hết hàng. Danh sách rỗng trả về []. Lỗi kết nối CSDL trả HTTP 503 và JSON Problem Detail gồm status và detail.

```json
{"productId": 1, "name": "Ấm siêu tốc 1,7 lít", "price": 390000.00, "availableQuantity": 1}
```

| Sản phẩm mẫu | Giá bán (đồng) | Serial chưa phân bổ | Đơn chờ giữ | Tồn khả dụng |
| --- | ---: | ---: | ---: | ---: |
| Ấm siêu tốc 1,7 lít | 390.000 | 1 | 0 | 1 |
| Nồi cơm điện 1,8 lít | 890.000 | 4 | 1 | 3 |
| Bộ chén sứ 4 chiếc | 180.000 | 2 | 0 | 2 |
| Chảo chống dính 24 cm | 320.000 | 0 | 0 | 0 |

Để kiểm tra, mở http://localhost:8080/api/v1/products rồi so sánh bốn giá và số lượng với bảng trên. Trên giao diện, sản phẩm có tồn khả dụng bằng 0 hiển thị “Hết hàng”.
