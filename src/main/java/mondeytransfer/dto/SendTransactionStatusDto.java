package mondeytransfer.dto;

import java.util.UUID;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class SendTransactionStatusDto {
    private String status = "Request for transaction has been send success";
    private UUID uuid = UUID.randomUUID();

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
