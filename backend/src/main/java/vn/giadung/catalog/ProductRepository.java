package vn.giadung.catalog;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ProductRepository {
    private final JdbcTemplate jdbc;

    ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Product> findForSale() {
        // Aggregate separately so a join cannot multiply serial counts by order lines.
        String sql = """
            SELECT p.ma_sp, p.ten, p.don_gia_ban,
                   COALESCE(stock.unallocated, 0) - COALESCE(orders.reserved, 0) AS available
            FROM SAN_PHAM p
            LEFT JOIN (
                SELECT s.ma_sp, COUNT(*) AS unallocated
                FROM CHI_TIET_SP s
                JOIN PHIEU_NHAP_KHO r ON r.ma_phieu = s.ma_phieu
                WHERE s.ma_don IS NULL AND r.trang_thai = 'RECEIVED'
                GROUP BY s.ma_sp
            ) stock ON stock.ma_sp = p.ma_sp
            LEFT JOIN (
                SELECT l.ma_sp, SUM(l.so_luong) AS reserved
                FROM CHI_TIET_DON l
                JOIN DON d ON d.ma_don = l.ma_don
                WHERE d.trang_thai = 'PENDING'
                GROUP BY l.ma_sp
            ) orders ON orders.ma_sp = p.ma_sp
            WHERE p.don_gia_ban IS NOT NULL
            ORDER BY p.ma_sp
            """;
        return jdbc.query(sql, (row, index) -> new Product(row.getInt("ma_sp"),
                row.getString("ten"), row.getBigDecimal("don_gia_ban"), row.getLong("available")));
    }
}
