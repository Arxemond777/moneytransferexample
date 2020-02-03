package mondeytransfer.enums;

/**
 * Routes name keeper
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class Routes {
    private Routes() {}

    public static final String
            CREATE_A_NEW_USER = "/addUser",
            GET_ALL = "/getAll",
            GET_BY_ID = "/getById",
            GET_STATUSES = "/getStatuses",
            SEND_TRANSACTION = "/sendTransaction";
}
