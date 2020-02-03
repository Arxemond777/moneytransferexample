package mondeytransfer.dto;

import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

/**
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class UserDto {

    private Long id;
    private BigDecimal balance;

    public UserDto() {

    }

    public UserDto(Long id) {
        this.id = id;
        this.balance = BigDecimal.ZERO;
    }

    public UserDto(Long id, Double balance) {
        this.id = id;
        this.balance = new BigDecimal(balance);
    }

    public UserDto(Long id, String balance) {
        this.id = id;
        this.balance = new BigDecimal(balance);
    }

    public UserDto(Long id, BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }

    public UserDto(JsonObject json) {
        this.id = json.getLong("_id");
        this.balance = new BigDecimal(json.getString("balance"));
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * You don`t need any synchronization here because this method used only a thread choices by the formula
     * ({@link mondeytransfer.dto.TransactionDto#fromId} % {@link mondeytransfer.service.TransactionsService#COUNT_THREADS})
     *
     * @param balance
     */
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", balance=" + balance +
                '}';
    }

    public String response() {
        return "{" + "\"id\":" + id + ", \"balance\": \"" + balance+"\"}";
    }

    /**
     * synchronized for atomicity in order if there were send several transaction from different senders
     * an receiver for all this transactions the same user.
     *
     * @param sentSum
     */
    public synchronized void plusBalance(BigDecimal sentSum) {
        balance = balance.add(sentSum);
    }
}
