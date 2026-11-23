package vn.giadung.account;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class AccountService {
    record Account(int id, String name, String email, String phone, String address, String role) {}
    record Registration(String name, String email, String phone, String password, String role) {}
    record Profile(String name, String phone, String address) {}
    record Password(String oldPassword, String newPassword) {}

    private static final String SELECT = "SELECT ma_tk,ho_ten,email,so_dien_thoai,dia_chi,loai_tai_khoan FROM TAIKHOAN";
    private static final Set<String> INTERNAL = Set.of("ADMIN","STAFF","DEALER");
    private final JdbcTemplate db;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    AccountService(JdbcTemplate db) { this.db = db; }

    private List<Account> query(String suffix, Object... args) {
        return db.query(SELECT + suffix, (r,i) -> new Account(r.getInt(1),r.getString(2),
                r.getString(3),r.getString(4),r.getString(5),r.getString(6)), args);
    }

    Account account(int id) {
        return query(" WHERE ma_tk=?", id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Không tìm thấy tài khoản."));
    }

    private static String required(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.strip().length() > limit)
            throw new ResponseStatusException(BAD_REQUEST, field + " không hợp lệ.");
        return value.strip();
    }

    private static String email(String value) {
        String email = required(value,100,"Email").toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new ResponseStatusException(BAD_REQUEST,"Email không hợp lệ.");
        return email;
    }

    private String hash(String password) {
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(BAD_REQUEST,"Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte.");
        return encoder.encode(password);
    }

    Account create(Registration data, String role) {
        String email = email(data.email());
        try {
            db.update("INSERT INTO TAIKHOAN(ho_ten,email,so_dien_thoai,mat_khau,loai_tai_khoan) VALUES(?,?,?,?,?)",
                    required(data.name(),100,"Họ tên"),email,required(data.phone(),15,"Số điện thoại"),hash(data.password()),role);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(CONFLICT,"Email đã được sử dụng.");
        }
        return query(" WHERE email=?", email).get(0);
    }

    Account login(String email, String password) {
        return db.query("SELECT ma_tk,mat_khau FROM TAIKHOAN WHERE email=?", (r,i) ->
                new Credential(r.getInt(1),r.getString(2)), email(email)).stream()
                .filter(c -> matches(password,c.hash())).map(c -> account(c.id())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED,"Email hoặc mật khẩu chưa đúng."));
    }
    private record Credential(int id, String hash) {}
    private boolean matches(String value,String hash) {
        return value != null && value.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(value,hash);
    }

    @Transactional
    public Account profile(int id, Profile p) {
        String address = p.address() == null ? "" : p.address().strip();
        if (address.length()>255) throw new ResponseStatusException(BAD_REQUEST,"Địa chỉ quá dài.");
        db.update("UPDATE TAIKHOAN SET ho_ten=?,so_dien_thoai=?,dia_chi=? WHERE ma_tk=?",
                required(p.name(),100,"Họ tên"),required(p.phone(),15,"Số điện thoại"),address.isEmpty()?null:address,id);
        return account(id);
    }

    @Transactional
    public void password(int id, Password p) {
        String old = db.queryForObject("SELECT mat_khau FROM TAIKHOAN WHERE ma_tk=? FOR UPDATE",String.class,id);
        if (!matches(p.oldPassword(),old)) throw new ResponseStatusException(BAD_REQUEST,"Mật khẩu cũ chưa đúng.");
        if (p.oldPassword().equals(p.newPassword())) throw new ResponseStatusException(BAD_REQUEST,"Mật khẩu mới phải khác mật khẩu cũ.");
        db.update("UPDATE TAIKHOAN SET mat_khau=? WHERE ma_tk=?",hash(p.newPassword()),id);
    }

    private void admin(int actor) {
        if (!account(actor).role().equals("ADMIN")) throw new ResponseStatusException(FORBIDDEN,"Chỉ quản trị viên được thực hiện.");
    }
    List<Account> list(int actor) {
        admin(actor);
        return query(" ORDER BY ma_tk");
    }
    @Transactional
    public Account internal(int actor, Registration data) {
        admin(actor);
        if (data.role()==null || !INTERNAL.contains(data.role())) throw new ResponseStatusException(BAD_REQUEST,"Vai trò nội bộ không hợp lệ.");
        return create(data,data.role());
    }

    @Transactional
    public Account role(int actor,int id,String role) {
        // Lock in primary-key order so concurrent demotions see the latest admin set.
        List<Account> accounts = query(" ORDER BY ma_tk FOR UPDATE");
        admin(actor);
        Account target = account(id);
        if (role==null || !(role.equals("CUSTOMER") || INTERNAL.contains(role)))
            throw new ResponseStatusException(BAD_REQUEST,"Vai trò không hợp lệ.");
        if (role.equals(target.role())) return target;
        if (target.role().equals("ADMIN") && accounts.stream().filter(a -> a.role().equals("ADMIN")).count()==1)
            throw new ResponseStatusException(CONFLICT,"Phải giữ lại ít nhất một quản trị viên.");
        boolean conflict = db.queryForObject("""
            SELECT (EXISTS(SELECT 1 FROM DON WHERE ma_nguoi_dat=?) AND ? <> 'CUSTOMER')
                OR (EXISTS(SELECT 1 FROM PHIEU_NHAP_KHO WHERE ma_dai_ly=?) AND ? <> 'DEALER')
                OR ((EXISTS(SELECT 1 FROM DON WHERE ma_nguoi_xu_ly=?)
                 OR EXISTS(SELECT 1 FROM PHIEU_NHAP_KHO WHERE ma_nguoi_lap=?)) AND ? NOT IN ('STAFF','ADMIN'))
            """,Boolean.class,id,role,id,role,id,id,role);
        if (conflict) throw new ResponseStatusException(CONFLICT,"Vai trò mới không phù hợp với lịch sử đơn hoặc phiếu nhập.");
        db.update("UPDATE TAIKHOAN SET loai_tai_khoan=? WHERE ma_tk=?",role,id);
        return account(id);
    }
}
