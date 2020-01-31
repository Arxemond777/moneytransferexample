package mondeytransfer.validator;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;

import java.math.BigDecimal;

import static java.util.Objects.isNull;
import static mondeytransfer.dto.ErrorResponseDto.printError;

/**
 * Requests validator
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
public class RequestValidator {
    private static final int NO_CONTENT = 204;
    private static final int NOT_FOUND = 404;
    private static final int UNPROCESSABLE_ENTITY = 422;

    public static UserDto getByIdPostValidator(final HttpServerResponse response, final UserDto user, final Long id) {
        if (isNull(user)) {
            response.setStatusCode(NOT_FOUND);
            response.end(printError(String.format("The user with id=%d not found", id)));
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
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("Incorrect data"));

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
            response.setStatusCode(UNPROCESSABLE_ENTITY);

            response.end(printError("Empty data"));

            return null;
        }

        if (
                isNull(td.getFromId()) || isNull(td.getSentSum()) || BigDecimal.ZERO.compareTo(td.getSentSum()) >= 0 ||
                isNull(td.getToId())
        ) {
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("Incorrect data"));


            return null;
        }

        if (td.getFromId().equals(td.getToId())) {
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("You can`t send yourself"));

            return null;
        }

        return td;
    }

    public static UserDto deleteValidator(final HttpServerResponse response, final RoutingContext routingContext) {
        response.putHeader("Content-Type", "application/json");

        UserDto user;
        try {
            user = Json.decodeValue(routingContext.getBodyAsString(), UserDto.class);
        } catch (io.vertx.core.json.DecodeException e) {
            response.setStatusCode(NO_CONTENT);
            response.end();

            return null;
        }

        if (isNull(user.getId())) {
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("Id is empty"));
            return null;
        }

        return user;
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
            response.setStatusCode(NO_CONTENT);
            response.end();

            return null;
        }

        if (isNull(user.getId()) || isNull(user.getBalance())) {
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("Id is empty or balance is empty"));
            return null;
        }

        if (BigDecimal.ZERO.compareTo(user.getBalance()) > 0) {
            response.setStatusCode(UNPROCESSABLE_ENTITY);
            response.end(printError("Balance less then 0.0"));

            return null;
        }

        return user;
    }
}
