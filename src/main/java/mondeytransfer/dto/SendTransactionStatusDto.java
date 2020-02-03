package mondeytransfer.dto;

import java.util.UUID;

import static mondeytransfer.enums.Messages.TRANSACTION_SUCCESS;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class SendTransactionStatusDto {

    public SendTransactionStatusDto(final UUID uuid) { // this constructor for mock tests
        this.uuid = uuid;
    }

    private String status = TRANSACTION_SUCCESS;
    private final UUID uuid; // can`t be change by business logic

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "{status:'\"" + status + "\"," + "uuid:\"" + uuid + "\"}";
    }
}
