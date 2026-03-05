package gift.order;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;

@Transactional
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
	private final WishRepository wishRepository;
    private final MemberRepository memberRepository;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
		WishRepository wishRepository,
        MemberRepository memberRepository
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
		this.wishRepository = wishRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    public Order create(Member member, OrderRequest request) {
        // validate option
        Option option = optionRepository.findById(request.optionId())
            .orElseThrow(() -> new NoSuchElementException("옵션이 존재하지 않습니다."));

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        int price = option.getProduct().getPrice() * request.quantity();
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        Order order = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // cleanup wish
        wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
            .ifPresent(wishRepository::delete);

        return order;
    }
}
