package mondeytransfer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import mondeytransfer.dto.SendTransactionStatusDto;
import mondeytransfer.dto.StatusDto;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;
import mondeytransfer.service.TransactionsService;

import java.util.Queue;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static mondeytransfer.enums.Routes.*;
import static mondeytransfer.validator.RequestValidator.*;
import static mondeytransfer.enums.HttpStatusesCodeEnum.*;

/**
 * This is the main app controller
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class AppController {
    private final TransactionsService TS;

    public AppController(final Router router) {
        this.TS = new TransactionsService();

        /**
         * Registry routes
         */
        router.post(CREATE_A_NEW_USER).handler(this::addOne);
        router.get(GET_ALL).handler(this::getAll);
        router.get(GET_BY_ID).handler(this::getById);
        router.get(GET_STATUSES).handler(this::getStatuses);

        router.post(SEND_TRANSACTION).blockingHandler(this::sendTransaction); // work with a LinkedBlockingQueue so can be blocked
    }

    /**
     * This method to a service of statuses
     * to push notification about transaction statuses
     *
     * @param routingContext
     */
    private void getStatuses(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        final Queue<StatusDto> statuses = TS.getStatuses();

        String statusesRes;
        try {
            statusesRes = new ObjectMapper().writeValueAsString(statuses);
        } catch (JsonProcessingException e) {
            response.end(e.getMessage());
            return;
        }

        response.setStatusCode(OK.getCode());
        response.putHeader("Content-Type", "application/json");
        response.end(statusesRes);

    }

    private void getById(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        final Long id = getByIdValidator(response, routingContext);
        if (isNull(id)) return;

        final UserDto user = TS.getById(id);

        if (isNull(getByIdPostValidator(response, user, id))) return;

        response.setStatusCode(OK.getCode());

        response.end(user.response());
    }

    private void sendTransaction(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        final TransactionDto td = transactionValidator(response, routingContext); // first validation
        if (isNull(td)) return;

        /**
         * second  validation basic on current data on the {@link mondeytransfer.service.TransactionsService.STORE} and
         * if success, then treat send the transaction to the {@link TransactionsService#TDQ} queue for transactions
         * processing
         */
        final String msg = TS.sendTransaction(td);

        response.setStatusCode(OK.getCode());

        if (nonNull(msg)) {
            response.end(msg);
            return;
        }

        response.end(
                new SendTransactionStatusDto(td.getTransactionId()).toString() // save transactionId (UUID)
        );
    }

    private void addOne(final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        final UserDto user = addValidator(response, routingContext);
        if (isNull(user)) return;

        response.setStatusCode(OK.getCode());
        final String msg = TS.addOne(user);

        if (nonNull(msg)) {
            response.end(msg);
            return;
        }
        response.setStatusCode(CREATED.getCode());

        response.end();
    }

    private void getAll(final RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.setStatusCode(OK.getCode());
        response.putHeader("Content-Type", "application/json");

        response.end(TS.getAll());
    }
}
