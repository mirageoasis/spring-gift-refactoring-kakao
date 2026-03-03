package gift;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import gift.member.MemberController;
import gift.option.OptionController;
import gift.product.ProductController;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElement(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ControllerAdvice(assignableTypes = {MemberController.class, OptionController.class, ProductController.class})
    static class IllegalArgumentHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
