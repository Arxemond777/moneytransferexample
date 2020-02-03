package mondeytransfer.enums;

/**
 * Messages keeper
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class Messages {
    private Messages() {
    }

    public static final String
            USER_EXIST = "This user id=%d is already exist",
            USER_NOT_FOUND = "The user with id=%d not found",
            USER_DOESNT_HAVE_ENOUGH_MONEY = "The sender has`t had enough money. Please try later",
            RECEIVER_DOESNT_HAVE_ENOUGH_MONEY = "Receiver with id=%d hasn`t existed",

            TRANSACTION_CANT_BE_SEND_TO_THE_SAME_SENDER = "You can`t send yourself",
            TRANSACTION_SUCCESS = "Request for transaction has been send success",

            EMPTY_DATA = "Empty data",
            INCORRECT_DATA = "Incorrect data";
}
