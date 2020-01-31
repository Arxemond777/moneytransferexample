package mondeytransfer.dto;

import mondeytransfer.enums.StatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class StatusDto {
    public StatusDto() {}
    public StatusDto(UUID uuid, StatusEnum se) {
        this(uuid, se, null);
    }

    public StatusDto(UUID uuid, StatusEnum se, String message) {
        this.uuid = uuid;
        this.statusEnum = se;
        this.message = message;
    }

    private UUID uuid;
    private StatusEnum statusEnum;
    /**
     * message - if there was {@link StatusEnum#ERROR} with description
     */
    private String message;
    private final LocalDateTime dateTime = LocalDateTime.now();

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public StatusEnum getStatusEnum() {
        return statusEnum;
    }

    public void setStatusEnum(StatusEnum statusEnum) {
        this.statusEnum = statusEnum;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getMessage() {
        return message;
    }
}
