package gift.order;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import gift.product.Product;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthenticationResolver authenticationResolver;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderController(
        OrderService orderService,
        AuthenticationResolver authenticationResolver,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderService = orderService;
        this.authenticationResolver = authenticationResolver;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        // auth check
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderService.findByMemberId(member.getId(), pageable));
    }

    // order flow:
    // 1. auth check
    // 2. validate option
    // 3. subtract stock
    // 4. deduct points
    // 5. save order
    // 6. cleanup wish
    // 7. send kakao notification
    @PostMapping
    public ResponseEntity<?> createOrder(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody OrderRequest request
    ) {
        // auth check
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        Order order = orderService.create(member, request);

        // best-effort kakao notification (outside transaction)
        sendKakaoMessageIfPossible(member, order);

        OrderResponse response = OrderResponse.from(order);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id()))
            .body(response);
    }

    private void sendKakaoMessageIfPossible(Member member, Order order) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            Product product = order.getOption().getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
