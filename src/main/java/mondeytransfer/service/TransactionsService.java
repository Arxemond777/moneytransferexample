package mondeytransfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import mondeytransfer.dto.StatusDto;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;
import mondeytransfer.enums.StatusEnum;
import mondeytransfer.other.CustomThreadFactory;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.isNull;
import static mondeytransfer.dto.ErrorResponseDto.printError;

/**
 * The service for works with transactions
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class TransactionsService {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionsService.class);
    private final Map<Long, UserDto> STORE = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<TransactionDto> TDQ = new LinkedBlockingQueue<>(100_000);
    private final Queue<StatusDto> statusDtos = new LinkedList<>();

    /**
     * 1) Init some test data
     * 2) Run threads for transactions processing in background
     */
    public TransactionsService() {
        initData();
        runQueueExecutor(); // run transactions handler
    }

    public String getAll() {
        try {
            return new ObjectMapper().writeValueAsString(STORE);
        } catch (Exception e) {
            return printError(String.format("An error occurred during serialization %s", e.getMessage()));
        }
    }

    public UserDto getById(final Long id) {
        return STORE.get(id);
    }

    public String addOne(final UserDto user) {
        return isNull(STORE.putIfAbsent(user.getId(), user)) ? null : printError(String.format("This user id=%d is already exist ", user.getId()));
    }

    public void deleteById(final Long id) {
        STORE.remove(id);
    }

    private void initData() {
        final UserDto first = new UserDto(1L, 1000.0);
        final UserDto second = new UserDto(2L, 2000.0);
        final UserDto third = new UserDto(3L, 3000.0);
        STORE.put(first.getId(), first);
        STORE.put(second.getId(), second);
        STORE.put(third.getId(), third);
    }

    /**
     * This method validate data and if success send to {@link TransactionsService#TDQ} on processing
     *
     * @param td - transaction dto
     * @return - some error during the validation if exist else null then validation is succes
     */
    public String sendTransaction(final TransactionDto td) {
        final UserDto user = STORE.get(td.getFromId());

        if (isNull(user) || user.getBalance().compareTo(td.getSentSum()) < 0)
            return printError("the sender has`t had enough money. Please try later");

        if (!STORE.containsKey(td.getToId()))
            return printError(String.format("Receiver with id=%d hasn`t existed", td.getToId()));

        try {
            TDQ.put(td);
        } catch (InterruptedException e) {
            LOGGER.error("A transaction interrupted {0}", td);
        }

        return null;
    }

    /**
     * This method is control {@link TransactionsService#TDQ} processing
     * of new transactions. Threads spin in a while-loop and consume
     * data from {@link TransactionsService#TDQ}. As soon as some a
     * transaction put in it, a one of thread took it and treat it.
     *
     * During treating if an error with data consistence appeared
     * the thread write it to {@link TransactionsService#statusDtos}
     * and send this error to this queue. If the transaction executed
     * success it also write the queue a msg about success.
     */
    private void runQueueExecutor() {
        final int c = Runtime.getRuntime().availableProcessors() / 2;
        final int countThreads = c < 2 ? 2 : c;

        final ExecutorService es = Executors.newFixedThreadPool(countThreads, new CustomThreadFactory("Transactions handler"));

        for (int i = 0; i < countThreads; i++)
            es.execute(() -> {
                while (true) {
                    try {
                        final TransactionDto op = TDQ.take(); // block and wait, if there aren`t transaction tasks

                        final UserDto senderUser = STORE.getOrDefault(op.getFromId(), new UserDto(0L));
                        final UserDto receiverUser = STORE.getOrDefault(op.getToId(), new UserDto(0L));

                        LOGGER.info("{0} take {1}", Thread.currentThread().getName(), op);

                        synchronized (senderUser) {
                            if (senderUser.getBalance().compareTo(BigDecimal.ZERO) <= 0 || receiverUser.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                                LOGGER.warn(
                                        "transaction {0} has failed because a sender " +
                                                "or a receiver has been deleted or data has been corrupted" +
                                                "or the sender don`t have enough money",
                                        op);
                                statusDtos.add(new StatusDto(op.getTransactionId(), StatusEnum.ERROR, "Data has been corrupted or the sender don`t have enough money"));

                                continue;
                            }

                            final BigDecimal res = senderUser.getBalance().subtract(op.getSentSum());

                            if (res.compareTo(BigDecimal.ZERO) < 0) { // second check the sender`s balance
                                LOGGER.warn(
                                        "User {0} don`t have enough money",
                                        op.getFromId());
                                statusDtos.add(new StatusDto(op.getTransactionId(), StatusEnum.ERROR, "The sender don`t have enough money"));

                                continue;
                            }

                            senderUser.setBalance(res);
                            receiverUser.plusBalance(op.getSentSum());

                            statusDtos.add(new StatusDto(op.getTransactionId(), StatusEnum.SUCCESS));
                        }


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    /**
     * This service return data a push-notify service
     *
     * @return - statuses for a push-notify service
     */
    public Queue<StatusDto> getStatuses() {
        final Queue<StatusDto> tmp = new LinkedList<>(this.statusDtos);
        this.statusDtos.clear();
        return tmp;
    }
}