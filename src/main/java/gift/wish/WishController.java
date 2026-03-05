package gift.wish;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gift.auth.AuthenticationResolver;
import gift.member.Member;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/wishes")
public class WishController {
    private final WishService wishService;
    private final AuthenticationResolver authenticationResolver;

    public WishController(WishService wishService, AuthenticationResolver authenticationResolver) {
        this.wishService = wishService;
        this.authenticationResolver = authenticationResolver;
    }

    @GetMapping
    public ResponseEntity<Page<WishResponse>> getWishes(
        @RequestHeader("Authorization") String authorization,
        Pageable pageable
    ) {
        // check auth
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(wishService.findByMemberId(member.getId(), pageable).map(WishResponse::from));
    }

    @PostMapping
    public ResponseEntity<WishResponse> addWish(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody WishRequest request
    ) {
        // check auth
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WishResult result = wishService.add(member.getId(), request);
        WishResponse response = WishResponse.from(result.getWish());

        if (!result.isCreated()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/api/wishes/" + response.id()))
            .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeWish(
        @RequestHeader("Authorization") String authorization,
        @PathVariable Long id
    ) {
        // check auth
        Member member = authenticationResolver.extractMember(authorization);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        wishService.remove(member.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
