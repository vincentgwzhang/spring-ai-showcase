package pl.piomin.services.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pl.piomin.services.model.Person;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("anthropic")
@DirtiesContext
class PersonControllerTestWithAnthropic {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Order(1)
    void testFindAllPersons() {
        ResponseEntity<Person[]> response = restTemplate.getForEntity("/persons", Person[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(response.getBody()).length).isEqualTo(10);
    }

    @Test
    @Order(2)
    void testFindPersonById() {
        int id = 4;
        ResponseEntity<Person> byIdResponse = restTemplate.getForEntity("/persons/" + id, Person.class);
        assertThat(byIdResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(byIdResponse.getBody()).isNotNull();
        assertThat(byIdResponse.getBody().getId()).isEqualTo(id);
    }

}
