package vn.giadung.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class OrderWorkflow {
    private static final Set<String> STATES = Set.of("PENDING","CONFIRMED","SHIPPING","COMPLETED","CANCELLED","REJECTED");
    private static final Map<String,String> NEXT = Map.of("PENDING","CONFIRMED","CONFIRMED","SHIPPING","SHIPPING","COMPLETED");
    private static final String SUMMARY = """
            SELECT d.ma_don AS orderId,d.ngay_gio AS placedAt,d.trang_thai AS status,
                   d.dia_chi_giao_hang AS address,b.ho_ten AS customer,h.ho_ten AS handler,
                   (SELECT SUM(l.so_luong*l.don_gia) FROM CHI_TIET_DON l WHERE l.ma_don=d.ma_don) AS total
            FROM DON d JOIN TAIKHOAN b ON b.ma_tk=d.ma_nguoi_dat
            LEFT JOIN TAIKHOAN h ON h.ma_tk=d.ma_nguoi_xu_ly
            """;
    private final JdbcTemplate db;
    OrderWorkflow(JdbcTemplate db) { this.db = db; }

    private void authorize(Integer actor, boolean staff, boolean lock) {
        if (actor == null) throw new ResponseStatusException(UNAUTHORIZED,"Vui lòng đăng nhập.");
        String role = db.queryForObject("SELECT loai_tai_khoan FROM TAIKHOAN WHERE ma_tk=?"+(lock?" FOR SHARE":""),String.class,actor);
        boolean allowed = staff ? Set.of("STAFF","ADMIN").contains(role) : "CUSTOMER".equals(role);
        if (!allowed) throw new ResponseStatusException(FORBIDDEN,"Tài khoản chưa có quyền thực hiện.");
    }

    public List<Map<String,Object>> list(Integer actor,boolean staff,String status) {
        authorize(actor,staff,false);
        if (status==null && staff) status="PENDING";
        if (status!=null && !status.isEmpty() && !STATES.contains(status))
            throw new ResponseStatusException(BAD_REQUEST,"Trạng thái không hợp lệ.");
        return db.queryForList(SUMMARY+"""
                WHERE (? OR d.ma_nguoi_dat=?) AND (? IS NULL OR ?='' OR d.trang_thai=?)
                ORDER BY d.ngay_gio DESC,d.ma_don DESC
                """,staff,actor,status,status,status);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> detail(Integer actor,boolean staff,int id) {
        authorize(actor,staff,false);
        var order = db.queryForList(SUMMARY+" WHERE d.ma_don=? AND (? OR d.ma_nguoi_dat=?)",id,staff,actor)
                .stream().findFirst().orElseThrow(OrderWorkflow::missing);
        order.put("items",db.queryForList("""
                SELECT p.ma_sp AS productId,p.ten AS name,l.so_luong AS quantity,l.don_gia AS price
                FROM CHI_TIET_DON l JOIN SAN_PHAM p ON p.ma_sp=l.ma_sp WHERE l.ma_don=? ORDER BY p.ma_sp
                """,id));
        order.put("serials",db.queryForList("""
                SELECT ma_sp AS productId,so_serial AS serial,ngay_het_han_bh AS warrantyUntil
                FROM CHI_TIET_SP WHERE ma_don=? ORDER BY ma_sp,ma_chi_tiet_sp
                """,id));
        return order;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(NOT_FOUND,"Không tìm thấy đơn hàng.");
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void change(Integer actor,boolean staff,int id,String target) {
        authorize(actor,staff,true);
        String current=db.queryForList("SELECT trang_thai FROM DON WHERE ma_don=? AND (? OR ma_nguoi_dat=?) FOR UPDATE",
                String.class,id,staff,actor).stream().findFirst().orElseThrow(OrderWorkflow::missing);
        boolean rejectPending = "REJECTED".equals(target) && "PENDING".equals(current);
        boolean allowed = staff ? rejectPending || (target!=null && target.equals(NEXT.get(current)))
                : "CANCELLED".equals(target) && Set.of("PENDING","CONFIRMED").contains(current);
        if (!allowed) throw new ResponseStatusException(CONFLICT,"Trạng thái đơn hiện tại không cho phép thao tác này.");

        record Line(int product,int quantity) {}
        var lines=db.query("SELECT ma_sp,so_luong FROM CHI_TIET_DON WHERE ma_don=? ORDER BY ma_sp",
                (r,i)->new Line(r.getInt(1),r.getInt(2)),id);
        for (Line line:lines)
            db.queryForObject("SELECT ma_sp FROM SAN_PHAM WHERE ma_sp=? FOR UPDATE",Integer.class,line.product());

        switch (target) {
            case "REJECTED" -> db.update("UPDATE DON SET ma_nguoi_xu_ly=? WHERE ma_don=?",actor,id);
            case "CONFIRMED" -> {
                for (Line line:lines) assign(id,line.product(),line.quantity());
                db.update("UPDATE DON SET ma_nguoi_xu_ly=? WHERE ma_don=?",actor,id);
            }
            case "CANCELLED" -> db.update("UPDATE CHI_TIET_SP SET ma_don=NULL WHERE ma_don=?",id);
            case "COMPLETED" -> db.update("""
                    UPDATE CHI_TIET_SP s JOIN SAN_PHAM p ON p.ma_sp=s.ma_sp
                    SET s.ngay_het_han_bh=CASE WHEN p.bao_hanh_thang>0
                        THEN DATE_ADD(CURRENT_DATE,INTERVAL p.bao_hanh_thang MONTH) END
                    WHERE s.ma_don=?
                    """,id);
            default -> { /* Shipping only changes the order status. */ }
        }
        db.update("UPDATE DON SET trang_thai=? WHERE ma_don=?",target,id);
    }

    private void assign(int order,int product,int quantity) {
        var serials=db.queryForList("""
                SELECT s.ma_chi_tiet_sp FROM CHI_TIET_SP s JOIN PHIEU_NHAP_KHO r ON r.ma_phieu=s.ma_phieu
                WHERE s.ma_sp=? AND s.ma_don IS NULL AND r.trang_thai='RECEIVED'
                ORDER BY r.ngay_gio,r.ma_phieu,s.ma_chi_tiet_sp LIMIT ? FOR UPDATE OF s
                """,Integer.class,product,quantity);
        if(serials.size()!=quantity) throw new ResponseStatusException(CONFLICT,"Thiếu serial cho sản phẩm #"+product+".");
        db.batchUpdate("UPDATE CHI_TIET_SP SET ma_don=? WHERE ma_chi_tiet_sp=?",serials,100,
                (ps,id)->{ps.setInt(1,order);ps.setInt(2,id);});
    }
}
