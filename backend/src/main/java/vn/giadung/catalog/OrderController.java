package vn.giadung.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
class OrderController {
    private final OrderService orders;

    OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    OrderService.Receipt place(@SessionAttribute(name="accountId", required=false) Integer actor,
                               @RequestBody OrderService.Checkout checkout) {
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Vui lòng đăng nhập.");
        return orders.place(actor,checkout);
    }
}
