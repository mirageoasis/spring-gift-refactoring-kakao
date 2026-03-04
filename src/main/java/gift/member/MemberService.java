package gift.member;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;

@Transactional
@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public MemberService(MemberRepository memberRepository, JwtProvider jwtProvider) {
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
    }

    public TokenResponse register(MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        Member member = memberRepository.save(new Member(request.email(), request.password()));
        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(MemberRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = jwtProvider.createToken(member.getEmail());
        return new TokenResponse(token);
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Member not found. id=" + id));
    }

    public Member create(String email, String password) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
        return memberRepository.save(new Member(email, password));
    }

    public void update(Long id, String email, String password) {
        Member member = findById(id);
        member.update(email, password);
    }

    public void chargePoint(Long id, int amount) {
        Member member = findById(id);
        member.chargePoint(amount);
    }

    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
