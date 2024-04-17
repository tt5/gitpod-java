package ce.chess.integration.messaging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import ce.chess.integration.util.ResourceUtils;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.awaitility.Awaitility;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;


public class MessageStepDef {

  private final ConnectionFactory connectionFactory;
  private final ConsumerWorld consumerWorld;

  public MessageStepDef(ConsumerWorld consumerWorld) {
    this.connectionFactory = TestSetupMessaging.getConnectionFactory(); // non-spring
    this.consumerWorld = Objects.requireNonNull(consumerWorld);
  }

  @After
  public void closeConsumers() {
    consumerWorld.closeAll();
  }

  @When("a message with the content from {string} was published to the exchange {string}")
  public void publishContentToExchange(String resourceName, String exchangeName) throws IOException, TimeoutException {
    String message = ResourceUtils.resourceAsString(resourceName);
    TestMessagePublisher testMessagePublisher = new TestMessagePublisher(connectionFactory);
    testMessagePublisher.publish(exchangeName, message);
    testMessagePublisher.close();
  }

  @When("a message with the content from {string} was published to the exchange {string} with the routing key {string}")
  public void publishContentToExchangeRoutingKey(
      String resourceName, String exchangeName, String routingKey) throws IOException, TimeoutException {
    String message = ResourceUtils.resourceAsString(resourceName);
    TestMessagePublisher testMessagePublisher = new TestMessagePublisher(connectionFactory);
    testMessagePublisher.publish(exchangeName, routingKey, MessageProperties.BASIC, message);
    testMessagePublisher.close();
  }

  @Given("I listen to messages in the exchange {string}")
  public void listenToMessagesInTheNewExchange(String exchangeName) {
    consumerWorld.putOrGet(exchangeName,
        () -> new ToStringConsumer(connectionFactory).consumeFromExchange(exchangeName, "#", true));
  }

  @Given("I listen to messages in the queue {string}")
  public void listenToMessagesInTheQueue(String queueName) {
    consumerWorld.putOrGet(queueName,
        () -> new ToStringConsumer(connectionFactory).consumeFromQueue(queueName));
  }

  @Then("I will eventually get a message on {string}")
  public void eventuallyGetMessage(String listenTo) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .until(consumerWorld.get(listenTo)::hasMessages);
  }

  @Then("I receive no message on {string}")
  public void receiveNoMessageOn(String listenTo) {
    assertThat(consumerWorld.get(listenTo).hasMessages(), is(false));
  }

  @Then("the message has an element {string} equal to {string}")
  public void theMessageWillHaveAnElementEqualTo(String path, String expected) {
    assertThat(
        JsonPath.from(consumerWorld.getCurrentConsumer().getLastMessage()).getString(path),
        is(equalTo(expected)));
  }

  @Then("the message has an element {string} containing {string}")
  public void theMessageHasElementContaining(String path, String expected) {
    assertThat(
        JsonPath.from(consumerWorld.getCurrentConsumer().getLastMessage()).getString(path),
        containsString(expected));
  }

  @Then("the message has the content of {string}")
  public void theMessageWillHaveContentEqualTo(String resourcePath) {
    String expected = ResourceUtils.resourceAsString(resourcePath).strip();
    assertThat(consumerWorld.getCurrentConsumer().getMessages(),
        hasItem(expected));
  }

  @Then("the message has the json content of {string}")
  public void theMessageWillHaveJsonContentEqualTo(String resourcePath) throws JSONException {
    String expected = ResourceUtils.resourceAsString(resourcePath);
    JSONAssert.assertEquals(expected,
        consumerWorld.getCurrentConsumer().getLastMessage(), JSONCompareMode.LENIENT);
  }

  @Then("I can dump the received messages")
  public void dumpMessages() {
    consumerWorld.getCurrentConsumer().getMessages().forEach(System.out::println);
  }

}
