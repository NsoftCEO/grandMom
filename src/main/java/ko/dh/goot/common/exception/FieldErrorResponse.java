package ko.dh.goot.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class FieldErrorResponse {
    private String field;
    private String message;
}
