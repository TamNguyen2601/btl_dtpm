package vn.giadung.catalog;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/{scope:me|staff}/orders")
class OrderHistoryController {
    private final OrderWorkflow orders;
    OrderHistoryController(OrderWorkflow orders) { this.orders=orders; }
    record Change(String status) {}

    @GetMapping
    List<Map<String,Object>> list(@PathVariable String scope,@SessionAttribute(value="accountId",required=false) Integer actor,
                                  @RequestParam(required=false) String status) {
        return orders.list(actor,scope.equals("staff"),status);
    }
    @GetMapping("/{id}")
    Map<String,Object> detail(@PathVariable String scope,@SessionAttribute(value="accountId",required=false) Integer actor,
                              @PathVariable int id) {
        return orders.detail(actor,scope.equals("staff"),id);
    }
    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void change(@PathVariable String scope,@SessionAttribute(value="accountId",required=false) Integer actor,
                @PathVariable int id,@RequestBody Change change) {
        orders.change(actor,scope.equals("staff"),id,change.status());
    }
}
