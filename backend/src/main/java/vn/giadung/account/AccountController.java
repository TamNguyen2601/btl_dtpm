package vn.giadung.account;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import vn.giadung.account.AccountService.*;

@RestController
@RequestMapping("/api/v1")
class AccountController {
    private final AccountService accounts;
    AccountController(AccountService accounts) { this.accounts = accounts; }

    private int actor(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("accountId") instanceof Integer id))
            throw new ResponseStatusException(UNAUTHORIZED,"Vui lòng đăng nhập.");
        return id;
    }

    record Login(String email,String password) {}
    record Role(String role) {}

    @PostMapping("/customers") @ResponseStatus(CREATED)
    Account register(@RequestBody Registration data) { return accounts.create(data,"CUSTOMER"); }

    @PostMapping("/sessions") @ResponseStatus(CREATED)
    Account login(@RequestBody Login data,HttpServletRequest request) {
        Account account = accounts.login(data.email(),data.password());
        HttpSession previous = request.getSession(false);
        if (previous != null) previous.invalidate();
        request.getSession(true).setAttribute("accountId",account.id());
        return account;
    }
    @DeleteMapping("/sessions/current") @ResponseStatus(NO_CONTENT)
    void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
    @GetMapping("/me")
    Account me(HttpServletRequest r) { return accounts.account(actor(r)); }
    @PatchMapping("/me")
    Account profile(HttpServletRequest r,@RequestBody Profile data) { return accounts.profile(actor(r),data); }
    @PutMapping("/me/password") @ResponseStatus(NO_CONTENT)
    void password(HttpServletRequest r,@RequestBody Password data) { accounts.password(actor(r),data); }
    @GetMapping("/admin/accounts")
    List<Account> list(HttpServletRequest r) { return accounts.list(actor(r)); }
    @PostMapping("/admin/accounts") @ResponseStatus(CREATED)
    Account internal(HttpServletRequest r,@RequestBody Registration data) { return accounts.internal(actor(r),data); }
    @PatchMapping("/admin/accounts/{id}/role")
    Account role(HttpServletRequest r,@PathVariable int id,@RequestBody Role data) { return accounts.role(actor(r),id,data.role()); }
}
