package snowflakeid;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StepDefinitions {
    LocalDateTime datetime;
    Long machineId;

    Long actualId;
    Long actualSequenceNumber;

    SnowflakeId instance = SnowflakeId.getInstance(null);

    @ParameterType(".*")
    public LocalDateTime datetime(String dateTimeString) {
        System.out.println("dateTimeString: " + dateTimeString);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    @Given("today is {datetime} UTC")
    public void todayIs(LocalDateTime datetime) {
        System.out.println("Date: " + datetime);
        this.datetime = datetime;
    }

    @And("machineId is {string}")
    public void machineIdIs(String machineId) {
        System.out.println("MachineId: " + machineId);
        this.machineId = Long.valueOf(machineId);
    }

    @And("previous sequence number is {int}")
    public void previousSequenceNumberIs(int previousSequenceNumber) {
        System.out.println("Previous Sequence Number: " + previousSequenceNumber);
    }

    @Given("previous sequence number is NULL")
    public void previous_sequence_number_is_null() {
        // Write code here that turns the phrase above into concrete actions
        System.out.println("Previous Sequence Number: NULL");
    }

    @When("a new distributed ID is generated")
    public void aNewDistributedIDIsGenerated() throws NoSuchFieldException, IllegalAccessException {
        System.out.println("WHEN New distributed ID is generated with machineId " + machineId);
        setMachineId(instance, machineId);
        Clock fixedClock = Clock.fixed(datetime.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        setClock(instance, fixedClock);
        actualId = instance.nextId();
        actualSequenceNumber = actualId & 0xFFF;
    }

    @Then("it should be {string}")
    public void itShouldBe(String newId) {
        System.out.println("THEN New distributed ID is " + newId);
        assertEquals(newId, actualId.toString());
    }

    @And("the new sequence number should be {int}")
    public void theNewSequenceNumberShouldBe(int newSequenceNumber) {
        System.out.println("New Sequence Number: " + newSequenceNumber);
        assertEquals(newSequenceNumber, actualSequenceNumber);
    }

    private void setMachineId(SnowflakeId instance, long machineId) throws NoSuchFieldException, IllegalAccessException {
        Field machineIdField = SnowflakeId.class.getDeclaredField("machineId");
        machineIdField.setAccessible(true);
        machineIdField.set(instance, machineId);
    }

    private void setClock(SnowflakeId instance, Clock clock) throws NoSuchFieldException, IllegalAccessException {
        Field clockField = SnowflakeId.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(instance, clock);
    }
}
