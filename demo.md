# Kịch bản trình bày TC1 và TC3 trên web Gia Dụng

**Thời lượng gợi ý khoảng 12 phút.** Dùng một khách hàng và một nhân viên xuyên suốt hai module. Khách tạo hai đơn. Đơn A dùng để minh họa hủy và hoàn tồn. Đơn B dùng để minh họa giao hàng và bảo hành. Ghi mã đơn ngay khi đặt thành công để hai vai trò cùng mở đúng đơn.

## Chuẩn bị trước khi trình bày

- Mở [cửa hàng](http://localhost:5173) trong hai phiên độc lập, chẳng hạn Chrome thường cho **khách hàng** và Edge cho **nội bộ**. Trong phiên nội bộ, lần lượt đăng nhập vai trò quản trị viên và nhân viên.
- Với dữ liệu từ [file SQL demo](database/btl_gia_dung_demo.sql), quản trị viên là `quantrivien@gmail.com`. Tài khoản dự phòng là `nhanvien@gmail.com` và `khachhang1@gmail.com`. Mật khẩu ban đầu của các tài khoản mẫu là `12345678`.
- Chọn **Cửa hàng → Còn hàng → Làm mới**. Ghi số lượng còn của ấm siêu tốc và nồi cơm điện. Nếu chỉ còn một chiếc ấm hoặc một nồi cơm, có thể dùng cùng sản phẩm cho cả hai đơn bằng cách hủy A trước khi tạo B. Cả hai sản phẩm đều có bảo hành 12 tháng. Nếu tất cả đã hết, cần chuẩn bị tồn khả dụng trước buổi demo. Khi khởi tạo cơ sở dữ liệu demo mới, tùy chọn `@them_ton_demo=1` trong file SQL sẽ tạo thêm serial để bán.
- Chọn một hậu tố mới cho mỗi lần trình bày, ví dụ `0715`. Dùng `khach.0715@example.test` và `nv.0715@example.test` cho hai tài khoản sẽ tạo. Chỉ đổi mật khẩu và vai trò trên tài khoản demo vừa tạo.

## TC1 · Quản lý tài khoản (4 phút)

**Lời mở đầu:** “Em bắt đầu từ hai người dùng chưa có trong hệ thống. Khách có thể tự đăng ký. Nhân viên cần quản trị viên tạo tài khoản để có quyền xử lý đơn.”

| Bước | Thao tác trên giao diện | Kết quả chỉ cho giảng viên |
|---|---|---|
| 1 · Khách đăng ký (CN01) | Ở cửa sổ khách, chọn **Đăng ký**. Nhập họ tên **Khách Demo**, email khách đã chọn, điện thoại `0901234567`, mật khẩu `Demo12345`. Bấm **Đăng ký**. | Web chuyển sang **Đăng nhập**. Mở lại **Đăng ký** và gửi cùng email một lần nữa để chỉ ra thông báo email đã tồn tại. |
| 2 · Khách sửa hồ sơ (CN02) | Chọn **Đăng nhập** bằng tài khoản vừa tạo. Bấm tên khách trên thanh điều hướng. Điền địa chỉ **12 Nguyễn Trãi, Hà Nội**, đổi điện thoại thành `0901234568`, bấm **Lưu hồ sơ** rồi tải lại trang. | Hồ sơ hiển thị địa chỉ và số mới sau khi tải lại. Thanh điều hướng có **Đơn của tôi**. |
| 3 · Khách đổi mật khẩu (CN01–CN02) | Trong **Đổi mật khẩu**, thử mật khẩu cũ sai và mật khẩu mới `DemoMoi123`, bấm **Lưu mật khẩu**. Thử lại với mật khẩu cũ đúng `Demo12345`. Đăng xuất, thử đăng nhập bằng mật khẩu cũ rồi bằng mật khẩu mới. | Lần đổi đầu bị từ chối. Sau lần đổi thành công, mật khẩu cũ bị từ chối và mật khẩu mới mở được phiên khách. Đăng xuất để bắt đầu TC3 từ giỏ của khách vãng lai. |
| 4 · Quản trị viên tạo nhân viên (CN03) | Ở cửa sổ nội bộ, đăng nhập quản trị viên, mở **Tài khoản nội bộ**. Nhập **Nhân viên Demo**, email nhân viên đã chọn, điện thoại `0902345678`, mật khẩu `Demo12345`. Chọn **Nhân viên → Tạo tài khoản**. | Dòng email mới xuất hiện ở **Quản lý vai trò**. Để minh họa đổi vai trò khi tài khoản chưa có lịch sử, chọn **Đại lý → Lưu vai trò**, tải lại để kiểm tra, sau đó đổi lại **Nhân viên** và lưu. |
| 5 · Bàn giao cho nhân viên | Quản trị viên đăng xuất. Trong cùng cửa sổ, đăng nhập bằng email nhân viên vừa tạo và mở **Xử lý đơn**. | Nhân viên nhìn thấy danh sách đơn chờ. Giữ cửa sổ này để tiếp nhận đơn khách sắp đặt. |

## TC3 · Quản lý đơn hàng (7 phút)

### Đơn A · Khách đặt hàng, nhân viên xác nhận, khách hủy

| Bước | Thao tác trên giao diện | Kết quả chỉ cho giảng viên |
|---|---|---|
| 1 · Chuẩn bị giỏ (CN07) | Ở cửa sổ khách đang đăng xuất, bấm biểu tượng **giỏ có dấu cộng** trên ấm siêu tốc. Mở **Giỏ hàng**, để số lượng **1**, nhập **12 Nguyễn Trãi, Hà Nội**. Bấm **Đăng nhập để đặt hàng** và đăng nhập bằng mật khẩu mới. | Web quay lại giỏ. Chỉ vào sản phẩm, số lượng, địa chỉ và tiền tạm tính. Với giá mẫu, một ấm là **390.000 ₫**. |
| 2 · Đặt đơn (CN07) | Bấm **Xác nhận đặt hàng** một lần. Ghi mã hiện trên thông báo làm **mã A**. Mở **Đơn của tôi → Đơn #A**. | Giỏ đã trống. Đơn ở **Chờ xử lý**, có dòng hàng và tổng tiền. Chưa có serial. Vào **Cửa hàng → Làm mới** và đối chiếu số còn với ban đầu, ví dụ từ 1 xuống 0. |
| 3 · Xác nhận (CN09) | Ở cửa sổ nhân viên, chọn **Xử lý đơn → Làm mới → Đơn #A → Xác nhận đơn**. | Chi tiết chuyển sang **Đã xác nhận**, hiện **Người xác nhận** và serial của chiếc ấm. Đọc serial trên màn hình. Nếu cần tìm lại đơn, đổi bộ lọc **Trạng thái** thành **Tất cả**. |
| 4 · Hủy sau xác nhận (CN08) | Ở cửa sổ khách, mở **Đơn của tôi → Làm mới → Đơn #A**. Chỉ vào serial, bấm **Hủy đơn** và đồng ý trong hộp thoại. | Đơn chuyển sang **Đã hủy**, serial được gỡ khỏi chi tiết. Mở **Cửa hàng → Làm mới**. Số ấm còn có thể đặt trở về mức ban đầu nếu không có giao dịch khác. |

**Lời chuyển:** “Khi khách đặt, hệ thống giữ số lượng. Khi nhân viên xác nhận, đơn mới gắn với một chiếc cụ thể qua serial. Vừa rồi ta đã thấy việc hủy đơn giải phóng chiếc ấm. Với đơn tiếp theo, em sẽ giao đến khi hoàn tất.”

### Đơn B · Giao thành công và chốt bảo hành

| Bước | Thao tác trên giao diện | Kết quả chỉ cho giảng viên |
|---|---|---|
| 1 · Khách đặt đơn mới (CN07) | Thêm **1 nồi cơm điện 1,8 lít** vào giỏ. Nếu muốn minh họa đơn nhiều dòng và còn hàng, thêm **1 bộ chén sứ 4 chiếc**. Nhập địa chỉ, kiểm tra tạm tính rồi đặt. Ghi **mã B**. | Theo giá mẫu, một nồi là **890.000 ₫**. Có thêm bộ chén thì tổng là **1.070.000 ₫**. Đơn B có mã riêng và ở **Chờ xử lý**. |
| 2 · Nhân viên giao hàng (CN09) | Trong **Xử lý đơn**, chọn **Tất cả → Làm mới → Đơn #B**. Bấm **Xác nhận đơn**, chỉ vào các serial rồi bấm **Chuyển sang đang giao**. | Đơn chuyển lần lượt sang **Đã xác nhận**, **Đang giao**. Ở cửa sổ khách, mở lại đơn B sau khi làm mới. Khách theo dõi được trạng thái, nút **Hủy đơn** đã biến mất. |
| 3 · Hoàn tất (CN08–CN09) | Trở về cửa sổ nhân viên, bấm **Hoàn tất đơn**. Ở cửa sổ khách, chọn **Đơn của tôi → Làm mới → Đơn #B**. | Cả hai bên thấy **Hoàn tất**. Bên dưới serial của nồi có **Bảo hành đến** với ngày tương ứng 12 tháng kể từ ngày hoàn tất. Nếu có bộ chén, dòng chén có serial nhưng không có ngày hết bảo hành vì thời hạn mẫu là 0 tháng. |

## Kết thúc · Đối chiếu quy tắc giữa hai module (1 phút)

1. Ở cửa sổ nội bộ, đăng xuất nhân viên và đăng nhập lại quản trị viên. Mở **Tài khoản nội bộ**, tìm đúng email **Nhân viên Demo** đã xác nhận đơn B.
2. Chọn **Khách hàng → Lưu vai trò**. Chỉ thông báo từ chối rồi tải lại trang để thấy vai trò vẫn là **Nhân viên**. Giải thích quy tắc QT13 bằng lần đổi vai trò thành công trước đó và lịch sử xử lý đơn vừa phát sinh.
3. Kết thúc ở cửa sổ khách với **Đơn của tôi → Tất cả**. Chỉ vào đơn A **Đã hủy** và đơn B **Hoàn tất**. Mở B để giảng viên thấy đồng thời người xác nhận, sản phẩm, số tiền, serial và hạn bảo hành.

**Nếu giảng viên hỏi về REST API:** Mở DevTools → **Network** → **Fetch/XHR** trước khi thao tác. Khi khách đặt hàng, chọn yêu cầu `POST /api/v1/orders` để đối chiếu mã đơn trả về với màn hình. Khi nhân viên xác nhận, chọn `PATCH /api/v1/staff/orders/{id}/status` để xem trạng thái gửi lên và phản hồi `204`. Khi khách mở lại chi tiết, `GET /api/v1/me/orders/{id}` trả về trạng thái và serial hiện tại.
