package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.promineotech.jeep.Constants;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

import lombok.Getter;


class FetchJeepTest {
	


	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles("test")
	@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
			"classpath:flyway/migrations/V1.0__Jeep_data.sql" }, config = @SqlConfig(encoding = "utf-8"))
	class TestsThatDoNotPolluteTheApplicationContext {
		@Autowired
		@Getter
		private TestRestTemplate restTemplate;
		@LocalServerPort
		private int serverPort;
		/*
		 * When given a valid model, trim, and URI; return a success (200) and confirm
		 * the result is as expected.
		 */
		@Test
		void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
			// given: a valid model, trim and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim = "Sport";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When: a connection is made to the URI
			ResponseEntity<List<Jeep>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then: a success (OK - 200) status code is returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

			// AND: the actual list returned is the same as the expected list
			List<Jeep> actual = response.getBody();
			List<Jeep> expected = buildExpected();

			assertThat(actual).isEqualTo(expected);
		}

		/*
		 * When given an unknown trim return not found (404) and confirm the error
		 * message is as expected.
		 */
		@Test
		void testThatAnErrorMessageIsReturnedWhenAnUnknownrimIsSupplied() {
			
			// given: a valid model, unknown trim and valid URI
			JeepModel model = JeepModel.WRANGLER;
			String trim = "Unknown";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then: a not found (404) status code is returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

			// AND: an error message is returned
			Map<String, Object> error = response.getBody();
			assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
		}

		/*
		 * When given an invalid model or trim return bad request (400) and confirm the
		 * error message is as expected.
		 */
		@ParameterizedTest
		@MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
		void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSupplied(String model, String trim, String reason) {
			// given: a valid model, trim and URI
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then: a bad request (400) status code is returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

			// AND: an error message is returned
			Map<String, Object> error = response.getBody();
			assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);
		}

	}

	static Stream<Arguments> parametersForInvalidInput() {
		//@formatter:off
		return Stream.of(
				arguments("WRANGLER", "@!#@$!", "Trim contains non-alphnumeric characters" ),
				arguments("GRAND_CHEROKEE", "C".repeat(Constants.TRIM_MAX_LENGTH +1), "Trim length is too long" ),
				arguments("INVALID", "Sport", "Model is not enum value")
				);
		//@formatter:on
	}

	protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
		// formatter:off
		assertThat(error).containsKey("message").containsEntry("status code", status.value())
				.containsEntry("uri", "/jeeps").containsKey("timestamp")
				.containsEntry("reason", status.getReasonPhrase());
		// formatter:on
	}

	protected List<Jeep> buildExpected() {
		List<Jeep> expectedList = new LinkedList<>();

		//@formatter:off
		expectedList.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(2)
				.wheelSize(17)
				.basePrice(new BigDecimal("28475.00"))
				.build());
		
		expectedList.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(4)
				.wheelSize(17)
				.basePrice(new BigDecimal("31975.00"))
				.build());
		//@formatter:on
		return expectedList;
	}

	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles("test")
	@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
			"classpath:flyway/migrations/V1.0__Jeep_data.sql" }, config = @SqlConfig(encoding = "utf-8"))
	class TestsThatDoPolluteTheApplicationContext {
		@Autowired
		@Getter
		private TestRestTemplate restTemplate;
		@LocalServerPort
		private int serverPort;
		@MockBean
		private JeepSalesService jeepSalesService;

		/*
		 * Unplanned Error Method
		 */
		@Test
		void testThatAnUnplannedErrorResultsInA500Status() {			
			
			// given: a valid model, unknown trim and valid URI
			JeepModel model = JeepModel.WRANGLER;
			String trim = "Sport";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);
			
			doThrow(new RuntimeException("Ouch!")).when(jeepSalesService).fetchJeeps(model, trim);
			

			// When: a connection is made to the URI
			ResponseEntity<Map<String, Object>> response = getRestTemplate().exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});
			

			// Then: an internal server error(500) status code is returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

			// AND: an error message is returned
			Map<String, Object> error = response.getBody();
			assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
					
		}
	}
}