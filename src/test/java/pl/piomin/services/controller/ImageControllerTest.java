package pl.piomin.services.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ImageControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

//    @Test
    void testDescribe() {
        ResponseEntity<String[]> response = restTemplate.getForEntity("/images/describe", String[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testDescribeImage() {
        // Use a known image id or fallback to a sample (e.g., "fruits")
        String imageId = "fruits";
        ResponseEntity<String> response = restTemplate.getForEntity("/images/describe/" + imageId, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

//    @Test
    void testFindObject() {
        String object = "apple";
        ResponseEntity<byte[]> response = restTemplate.getForEntity("/images/find/" + object, byte[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

//    @Test
//    void testGenerateImage() {
//        String object = "banana";
//        ResponseEntity<byte[]> response = restTemplate.getForEntity("/images/generate/" + object, byte[].class);
//        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
//        assertThat(response.getBody()).isNotNull();
//    }

//    @Test
//    void testGenerateAndMatch() {
//        String object = "orange";
//        ResponseEntity<String> response = restTemplate.getForEntity("/images/generate-and-match/" + object, String.class);
//        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
//        assertThat(response.getBody()).isNotNull();
//    }
}
