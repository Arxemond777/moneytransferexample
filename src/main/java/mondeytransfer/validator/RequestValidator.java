package mondeytransfer.validator;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;

import java.math.BigDecimal;

import static java.util.Objects.isNull;
import static mondeytransfer.dto.ErrorResponseDto.printError;
import static mondeytransfer.enums.HttpStatusesCodeEnum.*;
import static mondeytransfer.enums.Messages.*;

/**
 * Requests validator
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class RequestValidator {

    public static UserDto getByIdPostValidator(final HttpServerResponse response, final UserDto user, final Long id) {
        if (isNull(user)) {
            response.setStatusCode(NOT_FOUND.getCode());
            response.end(printError(String.format(USER_NOT_FOUND, id)));
            return null;
        }

        return user;
    }

    public static Long getByIdValidator(final HttpServerResponse response, final RoutingContext routingContext) {
        response.putHeader("Content-Type", "application/json");
        Long id;
        try {
            id = Long.valueOf(routingContext.request().getParam("id"));
        } catch (NumberFormatException e) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());
            response.end(printError(INCORRECT_DATA));

            return null;
        }

        return id;
    }
    public static TransactionDto transactionValidator(final HttpServerResponse response, final RoutingContext routingContext) {
        response.putHeader("Content-Type", "application/json");

        TransactionDto td;
        try {
            td = Json.decodeValue(routingContext.getBodyAsString(), TransactionDto.class);
        } catch (io.vertx.core.json.DecodeException e) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());

            response.end(printError(EMPTY_DATA));

            return null;
        }

        if (
                isNull(td.getFromId()) || isNull(td.getSentSum()) || BigDecimal.ZERO.compareTo(td.getSentSum()) >= 0 ||
                isNull(td.getToId())
        ) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());
            response.end(printError(INCORRECT_DATA));


            return null;
        }

        if (td.getFromId().equals(td.getToId())) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());
            response.end(printError(TRANSACTION_CANT_BE_SEND_TO_THE_SAME_SENDER));

            return null;
        }

        return td;
    }

    /**
     * This is a validator for new
     *
     * @param response
     * @param routingContext
     * @return User
     */
    public static UserDto addValidator(final HttpServerResponse response, final RoutingContext routingContext) {
        response.putHeader("Content-Type", "application/json");

        UserDto user;
        try {
            user = Json.decodeValue(routingContext.getBodyAsString(), UserDto.class);
        } catch (io.vertx.core.json.DecodeException e) {
            response.setStatusCode(NO_CONTENT.getCode());
            response.end();

            return null;
        }

        if (isNull(user.getId()) || isNull(user.getBalance())) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());
            response.end(printError("Id is empty or balance is empty"));
            return null;
        }

        if (BigDecimal.ZERO.compareTo(user.getBalance()) > 0) {
            response.setStatusCode(UNPROCESSABLE_ENTITY.getCode());
            response.end(printError("Balance less then 0.0"));

            return null;
        }

        return user;
    }
}
