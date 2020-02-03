package mondeytransfer;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import mondeytransfer.dto.ErrorResponseDto;
import mondeytransfer.dto.SendTransactionStatusDto;
import mondeytransfer.dto.TransactionDto;
import mondeytransfer.dto.UserDto;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;

import static mondeytransfer.dto.ErrorResponseDto.printError;
import static mondeytransfer.enums.HttpStatusesCodeEnum.*;
import static mondeytransfer.enums.Messages.*;
import static mondeytransfer.enums.Routes.*;
import static mondeytransfer.service.TransactionsService.*;

/**
 * This is our JUnit test for our verticle. The test uses vertx-unit, so we declare a custom runner.
 *
 * @author <a href="mailto:1arxemond1@gmail.com">Yuri Glushenkov</a>
 */
@RunWith(VertxUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestTest {

    private Vertx vertx;
    private Integer port;

    @BeforeClass
    public static void initialize() throws IOException {}

    @AfterClass
    public static void shutdown() {}

    /**
     * Before executing our test, let's deploy our verticle.
     * <p/>
     * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle has successfully
     * completed its start sequence (thanks to `context.asyncAssertSuccess`).
     *
     * @param context the test context.
     */
    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();

        // Let's configure the verticle to listen on the 'test' port (randomly picked).
        // We create deployment options and set the _configuration_ json object:
        final ServerSocket socket = new ServerSocket(0);
        port = socket.getLocalPort();
        socket.close();

        final DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", port)
                );

        // We pass the options as the second parameter of the deployVerticle method.
        vertx.deployVerticle(Launcher.class.getName(), options, context.asyncAssertSuccess());
    }

    /**
     * This method, called after our test, just cleanup everything by closing the vert.x instance
     *
     * @param context the test context
     */
    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    /**
     * Let's ensure that transaction statuses are empty. And our app has started
     *
     * @param context the test context
     */
    @Test
    public void testGetStatuses(TestContext context) {
        defRequest(context, GET_STATUSES, "[]");
    }

    /**
     * Execute transactions transfer
     */
    @Test
    public void executeTransactionsTransfer(final TestContext context) {
        final Async async = context.async();
        final BigDecimal transferSum = new BigDecimal(100.1);
        final TransactionDto ts = new TransactionDto(EXIST_USER_ID, transferSum, EXIST_USER_ID_TWO);
        final SendTransactionStatusDto mock = new SendTransactionStatusDto(ts.getTransactionId());

        final String transaction
                = Json.encodePrettily(ts);

        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION)
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(transaction.length()))
                .handler(res1 -> {
                    context.assertEquals(res1.statusCode(), OK.getCode());
                    context.assertTrue(res1.headers().get("content-type").contains("application/json"));
                    res1.bodyHandler(body1 -> {

                        context.assertNotNull(ts.getTransactionId()); // check auto creation UUID
                        context.assertEquals(body1.toString(), mock.toString());

                        final Async async1 = context.async();
                        final String sender = Json.encodePrettily(new UserDto(EXIST_USER_ID, EXIST_USER_ID_BALANCE)); // check balance of the sender
                        vertx.createHttpClient().get(port, "localhost", GET_BY_ID+"?id="+EXIST_USER_ID)
                                .putHeader("content-type", "application/json")
                                .putHeader("content-length", Integer.toString(sender.length()))
                                .handler(res2 -> {
                                    context.assertEquals(res2.statusCode(), OK.getCode());
                                    context.assertTrue(res2.headers().get("content-type").contains("application/json"));
                                    res2.bodyHandler(body2 -> {
                                        final UserDto userDto = Json.decodeValue(body2.toString(), UserDto.class);
                                        context.assertEquals(userDto.getId(), EXIST_USER_ID);
                                        context.assertEquals(userDto.getBalance(), EXIST_USER_ID_BALANCE.subtract(transferSum)); // - transferSum from sender
                                        async1.complete();
                                    });
                                })
                                .write(sender)
                                .end();

                        final Async async2 = context.async(); // check balance of the receiver
                        final String receiver = Json.encodePrettily(new UserDto(EXIST_USER_ID_TWO, EXIST_USER_ID_BALANCE_TWO));
                        vertx.createHttpClient().get(port, "localhost", GET_BY_ID+"?id="+EXIST_USER_ID_TWO)
                                .putHeader("content-type", "application/json")
                                .putHeader("content-length", Integer.toString(receiver.length()))
                                .handler(res2 -> {
                                    context.assertEquals(res2.statusCode(), OK.getCode());
                                    context.assertTrue(res2.headers().get("content-type").contains("application/json"));
                                    res2.bodyHandler(body3 -> {
                                        final UserDto userDto = Json.decodeValue(body3.toString(), UserDto.class);
                                        context.assertEquals(userDto.getId(), EXIST_USER_ID_TWO);
                                        context.assertEquals(userDto.getBalance(), EXIST_USER_ID_BALANCE_TWO.add(transferSum)); // + transferSum to receiver

                                        final Async async3 = context.async(); // check stats not empty
                                        vertx.createHttpClient().getNow(port, "localhost", GET_STATUSES, response -> {
                                                    context.assertEquals(response.statusCode(), OK.getCode());
                                                    context.assertEquals(response.headers().get("content-type"), "application/json");
                                                    response.bodyHandler(body -> {
                                                        context.assertTrue(!body.toString().equals("[]")); // there are stats
                                                        async3.complete();
                                                    });
                                                });

                                        async2.complete();
                                    });
                                })
                                .write(receiver)
                                .end();

                        async.complete();
                    });
                })
                .write(transaction)
                .end();
    }

    /**
     * Execute transactions and check validations
     */
    @Test
    public void executeTransactionsAndCheckValidations(final TestContext context) {
        final Async async = context.async();

        final String transactionToYourself
                = Json.encodePrettily(new TransactionDto(EXIST_USER_ID, new BigDecimal(100.1), EXIST_USER_ID));

        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(transactionToYourself.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), UNPROCESSABLE_ENTITY.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), ErrorResponseDto.printError(TRANSACTION_CANT_BE_SEND_TO_THE_SAME_SENDER));
                        async.complete();
                    });
                })
                .write(transactionToYourself)
                .end();

        final String emptyTransaction // send an empty transaction
                = Json.encodePrettily(new TransactionDto());
        final Async async1 = context.async();

        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(emptyTransaction.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), UNPROCESSABLE_ENTITY.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), ErrorResponseDto.printError(INCORRECT_DATA));
                        async1.complete();
                    });
                })
                .write(emptyTransaction)
                .end();

        final Async async2 = context.async();
        final String transactionWithSumExeedUserSum // send a transaction with sum exceed sum of the sender
                = Json.encodePrettily(new TransactionDto(EXIST_USER_ID, EXIST_USER_ID_BALANCE.add(BigDecimal.ONE), EXIST_USER_ID_TWO));
        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(transactionWithSumExeedUserSum.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), ErrorResponseDto.printError(USER_DOESNT_HAVE_ENOUGH_MONEY));
                        async2.complete();
                    });
                })
                .write(transactionWithSumExeedUserSum)
                .end();

        final Async async3 = context.async();
        final long unexistedId = -1;
        final String uncreatedSender // send a transaction from an uncreated sender
                = Json.encodePrettily(new TransactionDto(unexistedId, BigDecimal.ONE, EXIST_USER_ID_TWO));
        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(uncreatedSender.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(String.format(USER_NOT_FOUND, unexistedId)));
                        async3.complete();
                    });
                })
                .write(uncreatedSender)
                .end();

        final Async async4 = context.async();
        final String uncreatedReceiver // send a transaction from an uncreated receiver
                = Json.encodePrettily(new TransactionDto(EXIST_USER_ID, BigDecimal.ONE, unexistedId));
        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(uncreatedReceiver.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(String.format(RECEIVER_DOESNT_HAVE_ENOUGH_MONEY, unexistedId)));
                        async4.complete();
                    });
                })
                .write(uncreatedReceiver)
                .end();

        final Async async5 = context.async();
        final long unexistedIdTwo = -2;
        final String uncreatedSenderAndReciver // send a transaction from an uncreated sender and  an uncreated receiver
                = Json.encodePrettily(new TransactionDto(unexistedId, BigDecimal.ONE, unexistedIdTwo));
        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION) // send a transaction to youself
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(uncreatedSenderAndReciver.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(String.format(USER_NOT_FOUND, unexistedId)));
                        async5.complete();
                    });
                })
                .write(uncreatedSenderAndReciver)
                .end();

        final Async async6 = context.async();
        final String tsZeroSum // send a transaction from an uncreated sender and  an uncreated receiver
                = Json.encodePrettily(new TransactionDto(EXIST_USER_ID, BigDecimal.ZERO, EXIST_USER_ID_TWO));
        vertx.createHttpClient().post(port, "localhost", SEND_TRANSACTION)
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(tsZeroSum.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), UNPROCESSABLE_ENTITY.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(INCORRECT_DATA));
                        async6.complete();
                    });
                })
                .write(tsZeroSum)
                .end();
    }

    /**
     * Get by id an uncreated user and check validations
     */
    @Test
    public void getByIdAndCheckValidations(final TestContext context) {
        final Async async1 = context.async();

        final String json = Json.encodePrettily(new UserDto(EXIST_USER_ID, EXIST_USER_ID_BALANCE));

        vertx.createHttpClient().get(port, "localhost", GET_BY_ID+"?id="+EXIST_USER_ID) // get an existing user
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        final UserDto userDto = Json.decodeValue(body.toString(), UserDto.class);
                        context.assertEquals(userDto.getId(), EXIST_USER_ID);
                        context.assertEquals(userDto.getBalance(), EXIST_USER_ID_BALANCE);
                        async1.complete();
                    });
                })
                .write(json)
                .end();

        final Async async2 = context.async();
        final int unexistedUserId = 999;
        vertx.createHttpClient().get(port, "localhost", GET_BY_ID+"?id="+unexistedUserId) // get an unexisting user
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), NOT_FOUND.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(String.format(USER_NOT_FOUND, unexistedUserId)));
                        async2.complete();
                    });
                })
                .write(json)
                .end();

        final Async async3 = context.async();
        vertx.createHttpClient().get(port, "localhost", GET_BY_ID) // send an incorrect query with empty body
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), UNPROCESSABLE_ENTITY.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(INCORRECT_DATA));
                        async3.complete();
                    });
                })
                .write(json)
                .end();

        final Async async4 = context.async();
        vertx.createHttpClient().get(port, "localhost", GET_BY_ID+"?ids=333") // send an incorrect query without id
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), UNPROCESSABLE_ENTITY.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(INCORRECT_DATA));
                        async4.complete();
                    });
                })
                .write(json)
                .end();
    }

    /**
     * Create a new user and check validations cases
     */
    @Test
    public void createANewUserAndCheckValidations(final TestContext context) {
        final Async async1 = context.async();
        final String existingUser = Json.encodePrettily(new UserDto(EXIST_USER_ID, EXIST_USER_ID_BALANCE));

        vertx.createHttpClient().post(port, "localhost", CREATE_A_NEW_USER) // duplicate a user
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(existingUser.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), OK.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertEquals(body.toString(), printError(String.format(USER_EXIST, EXIST_USER_ID)));
                        async1.complete();
                    });
                })
                .write(existingUser)
                .end();


        final long newId = 100L;
        final Async async2 = context.async();
        final String newUser = Json.encodePrettily(new UserDto(newId, new BigDecimal(1000)));

        vertx.createHttpClient().post(port, "localhost", CREATE_A_NEW_USER) // create
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(newUser.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), CREATED.getCode());
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        context.assertTrue(body.toString().equals(""));

                        final Async async3 = context.async();

                        // check only after a new user has been created. because this is async queries
                        vertx.createHttpClient().post(port, "localhost", CREATE_A_NEW_USER) // duplicate the user, which was created on the prev step
                                .putHeader("content-type", "application/json")
                                .putHeader("content-length", Integer.toString(newUser.length()))
                                .handler(response1 -> {
                                    context.assertEquals(response1.statusCode(), OK.getCode());
                                    context.assertTrue(response1.headers().get("content-type").contains("application/json"));
                                    response1.bodyHandler(body1 -> {
                                        context.assertEquals(body1.toString(), printError(String.format(USER_EXIST, newId)));
                                        async3.complete();
                                    });
                                })
                                .write(newUser)
                                .end();

                        async2.complete();
                    });
                })
                .write(newUser)
                .end();
    }

    private void defRequest(final TestContext context, final String urn, final String expectedResponse, final String contentType, final int statusCode) {
        // This test is asynchronous, so get an async handler to inform the test when we are done.
        final Async async = context.async();

        vertx.createHttpClient().getNow(port, "localhost", urn, response -> {
            context.assertEquals(response.statusCode(), statusCode);
            context.assertEquals(response.headers().get("content-type"), contentType);
            response.bodyHandler(body -> {
                context.assertTrue(body.toString().equals(expectedResponse));
                async.complete();
            });
        });
    }

    private void defRequest(final TestContext context, final String urn, final String expectedResponse) {
        defRequest(context, urn, expectedResponse, "application/json", OK.getCode());
    }
}