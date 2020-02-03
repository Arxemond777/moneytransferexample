package mondeytransfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import mondeytransfer.dto.StatusDto;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;
import mondeytransfer.enums.StatusEnum;
import mondeytransfer.model.TransactionStore;
import mondeytransfer.other.CustomThreadFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.isNull;
import static mondeytransfer.dto.ErrorResponseDto.printError;
import static mondeytransfer.enums.Messages.*;

/**
 * The service for works with transactions
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class TransactionsService {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionsService.class);
    public final static long EXIST_USER_ID = 1L; // for tests
    public final static long EXIST_USER_ID_TWO = 2L; // for tests
    public final static BigDecimal EXIST_USER_ID_BALANCE = new BigDecimal(1000L); // for tests
    public final static BigDecimal EXIST_USER_ID_BALANCE_TWO = new BigDecimal(2000L); // for tests

    /**
     * This map contains <NUMBER_OF_THREAD, LinkedBlockingQueue<TransactionDto>> in order to get rid of synchronization
     * in the following way: this service get a new TransactionDto it put it in the concrete
     * LinkedBlockingQueue<TransactionDto>> (LBQ). The service choice LBQ in the method
     * {@link TransactionsService#sendTransaction(mondeytransfer.dto.TransactionDto)}. The service get
     * mondeytransfer.dto.TransactionDto#fromId from the received TransactionDto and choice the required queue by the
     * formula (mondeytransfer.dto.TransactionDto#fromId % mondeytransfer.service.TransactionsService#COUNT_THREADS).
     * <p>
     * After it threads run in LBQ {@link TransactionsService#runQueueExecutor()} are treating this queues. Each thread
     * has his own LBQ. Hereby you don`t need to synchronized for each {@link UserDto}, because you have your SENDER
     * users in a separate queue, and this queue is treating sequentially only by his own thread.
     * <p>
     * Though you can scale your horizontal service increase {@link TransactionsService#COUNT_THREADS} and CPU/Memory.
     * Although it counting automatically now based a concrete machine where it was run, in the real app you can just
     * put this variable {@link {@link TransactionsService#COUNT_THREADS}} to the config file on get it from the env var
     */
    private final Map<Integer, LinkedBlockingQueue<TransactionDto>> TDQ_MAP = new HashMap<>();
    private final Queue<StatusDto> statusDtos = new LinkedList<>(); // this is transaction statuses queue

    private final Map<Long, UserDto> TS_STORE;

    private final int CORES_AVAILABLE = Runtime.getRuntime().availableProcessors() / 4;
    private final int COUNT_THREADS = CORES_AVAILABLE < 2 ? 2 : CORES_AVAILABLE;

    /**
     * 1) Init some test data
     * 2) Run threads for transactions processing in background
     */
    public TransactionsService() {
        this.TS_STORE = new TransactionStore().getSTORE();
        initData();
        runQueueExecutor(); // run transactions handler
    }

    /**
     * This method is control {@link TransactionsService#TDQ_MAP} processing of new transactions. Threads spin in
     * a while-loop and consume data from {@link TransactionsService#TDQ_MAP}. As soon as some a transaction put in it,
     * a one of thread took it and treat it.
     * <p>
     * During treating if an error with data consistence appeared the thread write it to
     * {@link TransactionsService#statusDtos} and send this error to this queue. If the transaction executed success it
     * also write the queue a msg about success.
     */
    private void runQueueExecutor() {

        final ExecutorService es = Executors.newFixedThreadPool(COUNT_THREADS, new CustomThreadFactory("Transactions handler for the queue="));

        for (Integer i = 0; i < COUNT_THREADS; i++) {
            final LinkedBlockingQueue<TransactionDto> tdq = new LinkedBlockingQueue<>(20_000);
            TDQ_MAP.put(i, tdq);

            es.execute(() -> {

                while (true) {
                    try {
                        final TransactionDto op = tdq.take(); // block and wait, if there aren`t transaction tasks

                        final UserDto senderUser = TS_STORE.getOrDefault(op.getFromId(), new UserDto(0L));
                        final UserDto receiverUser = TS_STORE.getOrDefault(op.getToId(), new UserDto(0L));

                        LOGGER.info("{0} take {1}", Thread.currentThread().getName(), op);

                        /**
                         * This check just in case, because it a normal working system you can`t send ZERO, it
                         * should be filtered in validation
                         */
                        if (senderUser.getBalance().compareTo(BigDecimal.ZERO) <= 0
                                || receiverUser.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                            LOGGER.error(
                                    "transaction {0} has failed because data has been corrupted" +
                                            "or the sender don`t have enough money",
                                    op);
                            statusDtos.add(new StatusDto(op.getTransactionId(), StatusEnum.CRITICAL_SYSTEM_ERROR, "Data has been corrupted or the sender don`t have enough money"));

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

                        /**
                         * transfer money. the main part of the app
                         */
                        senderUser.setBalance(res);
                        receiverUser.plusBalance(op.getSentSum());

                        statusDtos.add(new StatusDto(op.getTransactionId(), StatusEnum.SUCCESS));


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        LOGGER.error("Transaction error in the thread={0}, reason='{1}'",
                                Thread.currentThread().getName(),
                                e.getMessage()
                                );
                    }
                }
            });
        }
    }

    public String getAll() {
        try {
            return new ObjectMapper().writeValueAsString(TS_STORE);
        } catch (Exception e) {
            LOGGER.error("Serialize error in getAll()");
            return printError(String.format("An error occurred during serialization %s", e.getMessage()));
        }
    }

    public UserDto getById(final Long id) {
        return TS_STORE.get(id);
    }

    public String addOne(final UserDto user) {
        return isNull(TS_STORE.putIfAbsent(user.getId(), user)) ? null : printError(String.format(USER_EXIST, user.getId()));
    }

    private void initData() {
        final UserDto first = new UserDto(EXIST_USER_ID, EXIST_USER_ID_BALANCE);
        final UserDto second = new UserDto(EXIST_USER_ID_TWO, EXIST_USER_ID_BALANCE_TWO);
        final UserDto third = new UserDto(3L, 3000.0);
        TS_STORE.put(first.getId(), first);
        TS_STORE.put(second.getId(), second);
        TS_STORE.put(third.getId(), third);
    }

    /**
     * This method validate data and if success send to {@link TransactionsService#TDQ_MAP} on processing
     *
     * @param td - transaction dto
     * @return - some error during the validation if exist else null then validation is succes
     */
    public String sendTransaction(final TransactionDto td) {

        final UserDto user = TS_STORE.get(td.getFromId());

        if (isNull(user))
            return printError(String.format(USER_NOT_FOUND, td.getFromId()));

        if (user.getBalance().compareTo(td.getSentSum()) < 0)
            return printError(USER_DOESNT_HAVE_ENOUGH_MONEY);

        if (!TS_STORE.containsKey(td.getToId()))
            return printError(String.format(RECEIVER_DOESNT_HAVE_ENOUGH_MONEY, td.getToId()));

        try {
            TDQ_MAP.get((int) (td.getFromId() % COUNT_THREADS)).put(td);
        } catch (InterruptedException e) {
            LOGGER.error("A transaction interrupted {0}", td);
        }

        return null;
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