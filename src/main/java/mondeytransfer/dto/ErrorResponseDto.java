package mondeytransfer.dto;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class ErrorResponseDto {
    private final String msg;

    public ErrorResponseDto(final String msg) {
        this.msg = msg;
    }

    public static String printError(final String msg) {
        return "{error:" + msg + '}';
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "{error:" + msg + '}';
    }
}
