package mondeytransfer.enums;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public enum HttpStatusesCodeEnum {
    NO_CONTENT(204), NOT_FOUND(404), UNPROCESSABLE_ENTITY(422),
    OK(200), CREATED(201);

    private final int code;

    HttpStatusesCodeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
