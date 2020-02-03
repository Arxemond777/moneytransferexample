package mondeytransfer.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class TransactionDto {
    public TransactionDto() {}

    public TransactionDto(final Long fromId, final BigDecimal sentSum, final Long toId) {
        this.fromId = fromId;
        this.sentSum = sentSum;
        this.toId = toId;
    }

    private final UUID transactionId = UUID.randomUUID();

    private Long fromId;
    private BigDecimal sentSum;

    private Long toId;

    public Long getFromId() {
        return fromId;
    }

    public void setFromId(Long fromId) {
        this.fromId = fromId;
    }

    public BigDecimal getSentSum() {
        return sentSum;
    }

    public void setSentSum(BigDecimal sentSum) {
        this.sentSum = sentSum;
    }

    public Long getToId() {
        return toId;
    }

    public void setToId(Long toId) {
        this.toId = toId;
    }

    @Override
    public String toString() {
        return "TransactionDto{" +
                "fromId=" + fromId +
                ", sentSum=" + sentSum +
                ", toId=" + toId +
                '}';
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
