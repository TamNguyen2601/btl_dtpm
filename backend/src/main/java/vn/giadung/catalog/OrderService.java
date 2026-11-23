package vn.giadung.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class OrderService {
    record Item(JsonNode productId, JsonNode quantity) {}
    record Checkout(String address, List<Item> items) {}
    record Receipt(int orderId, String status, BigDecimal total) {}

    private final JdbcTemplate db;
    private final ProductRepository catalog;

    OrderService(JdbcTemplate db, ProductRepository catalog) {
        this.db = db;
        this.catalog = catalog;
    }

    private static int positiveInt(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() <= 0)
            throw new ResponseStatusException(BAD_REQUEST,"Mã sản phẩm và số lượng phải là số nguyên dương.");
        return value.intValue();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Receipt place(int customer, Checkout checkout) {
        String role = db.queryForObject("SELECT loai_tai_khoan FROM TAIKHOAN WHERE ma_tk=? FOR SHARE",String.class,customer);
        if (!"CUSTOMER".equals(role)) throw new ResponseStatusException(FORBIDDEN,"Chỉ khách hàng được đặt đơn.");
        String address = checkout.address() == null ? "" : checkout.address().strip();
        if (address.isBlank() || address.length() > 255)
            throw new ResponseStatusException(BAD_REQUEST,"Địa chỉ giao hàng cần có từ 1 đến 255 ký tự.");
        if (checkout.items() == null || checkout.items().isEmpty())
            throw new ResponseStatusException(BAD_REQUEST,"Giỏ hàng đang trống.");

        Map<Integer,Integer> quantities = new TreeMap<>();
        for (Item item : checkout.items()) {
            if (item == null || quantities.putIfAbsent(positiveInt(item.productId()),positiveInt(item.quantity())) != null)
                throw new ResponseStatusException(BAD_REQUEST,"Dòng sản phẩm trống hoặc trùng mã.");
        }
        for (int id : quantities.keySet()) {
            if (db.queryForList("SELECT ma_sp FROM SAN_PHAM WHERE ma_sp=? FOR UPDATE",Integer.class,id).isEmpty())
                throw new ResponseStatusException(CONFLICT,"Sản phẩm #" + id + " không còn tồn tại.");
        }
        Map<Integer,Product> products = catalog.findForSale().stream()
                .collect(Collectors.toMap(Product::productId, Function.identity()));
        db.update("INSERT INTO DON(ngay_gio,dia_chi_giao_hang,trang_thai,ma_nguoi_dat) VALUES(NOW(),?,'PENDING',?)",address,customer);
        int orderId = db.queryForObject("SELECT LAST_INSERT_ID()",Integer.class);
        BigDecimal total = BigDecimal.ZERO;
        for (var line : quantities.entrySet()) {
            Product product = products.get(line.getKey());
            if (product == null) throw new ResponseStatusException(CONFLICT,"Sản phẩm #" + line.getKey() + " đã ngừng bán.");
            int quantity = line.getValue();
            if (quantity > product.availableQuantity())
                throw new ResponseStatusException(CONFLICT,product.name()+" chỉ còn "+product.availableQuantity()+" chiếc.");
            db.update("INSERT INTO CHI_TIET_DON(ma_don,ma_sp,so_luong,don_gia) VALUES(?,?,?,?)",
                    orderId,product.productId(),quantity,product.price());
            total = total.add(product.price().multiply(BigDecimal.valueOf(quantity)));
        }
        return new Receipt(orderId,"PENDING",total);
    }
}
