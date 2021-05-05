package wooteco.subway.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import wooteco.subway.AcceptanceTest;
import wooteco.subway.dto.LineRequest;
import wooteco.subway.dto.LineResponse;

class LineControllerTest extends AcceptanceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @DisplayName("지하철 노선을 생성한다.")
    @Test
    void createLine() throws Exception {
        LineRequest lineRequest = new LineRequest("2호선", "red", 1L, 3L, 7);
        LineResponse lineResponse = new LineResponse(1L, "2호선", "red", new ArrayList<>());

        postLineApi(lineRequest)
            .statusCode(HttpStatus.valueOf(201).value())
            .body(is(OBJECT_MAPPER.writeValueAsString(lineResponse)));
    }

    @DisplayName("전체 노선을 조회한다.")
    @Test
    void showLines() throws Exception {
        LineRequest lineRequest1 = new LineRequest("3호선", "red", 1L, 3L, 7);
        ExtractableResponse<Response> lineResponse1 = postLineApi(lineRequest1)
            .extract();

        LineRequest lineRequest2 = new LineRequest("4호선", "blue", 2L, 3L, 4);
        ExtractableResponse<Response> lineResponse2 = postLineApi(lineRequest2)
            .extract();

        ExtractableResponse<Response> response = RestAssured.given().log().all()
            .when()
            .get("/lines")
            .then().log().all()
            .extract();

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Long> expectedLineIds = Arrays.asList(lineResponse1, lineResponse2).stream()
            .map(it -> Long.parseLong(it.header("Location").split("/")[2]))
            .collect(Collectors.toList());
        List<Long> resultLineIds = response.jsonPath().getList("", LineResponse.class).stream()
            .map(lineResponse -> lineResponse.getId())
            .collect(Collectors.toList());
        Assertions.assertThat(resultLineIds).containsAll(expectedLineIds);
    }

    private ValidatableResponse postLineApi(LineRequest lineRequest) throws JsonProcessingException {
        return RestAssured.given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(OBJECT_MAPPER.writeValueAsString(lineRequest))
            .when()
            .post("/lines")
            .then().log().all();
    }

    @DisplayName("아이디로 노선을 조회한다.")
    @Test
    void showLine() throws JsonProcessingException {
        LineRequest lineRequest = new LineRequest("5호선", "red", 1L, 3L, 7);
        ExtractableResponse<Response> lineResponse = postLineApi(lineRequest)
            .extract();

        long id = Long.parseLong(lineResponse.header("Location").split("/")[2]);
        LineResponse getResponse = new LineResponse(id, "5호선", "red", new ArrayList<>());

        RestAssured.given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when().get("/lines/" + id)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .body(is(OBJECT_MAPPER.writeValueAsString(getResponse)));
    }

    @DisplayName("지하철 노선을 수정한다.")
    @Test
    void editLine() throws JsonProcessingException {
        LineRequest lineRequest = new LineRequest("6호선", "red", 1L, 3L, 7);
        ExtractableResponse<Response> lineResponse = postLineApi(lineRequest)
            .extract();
        long id = Long.parseLong(lineResponse.header("Location").split("/")[2]);
        LineRequest putRequest = new LineRequest("구분당선", "white", 1L, 3L, 7);

        RestAssured.given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(OBJECT_MAPPER.writeValueAsString(putRequest))
            .when().put("/lines/" + id)
            .then().log().all()
            .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("없는 아이디의 지하철 노선은 수정할 수 없다.")
    @Test
    void cannotEditLineWhenNoId() throws JsonProcessingException {
        LineRequest putRequest = new LineRequest("구분당선", "white", 1L, 3L, 7);

        RestAssured.given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(OBJECT_MAPPER.writeValueAsString(putRequest))
            .when().put("/lines/" + 9999)
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("이미 생성된 노선의 이름으로 수정할 수 없다.")
    @Test
    void cannotEditLineWhenDuplicateName() throws JsonProcessingException {
        LineRequest lineRequest1 = new LineRequest("6호선", "red", 1L, 3L, 7);
        LineRequest lineRequest2 = new LineRequest("신분당선", "red", 1L, 3L, 7);
        ExtractableResponse<Response> lineResponse1 = postLineApi(lineRequest1)
            .extract();
        postLineApi(lineRequest2);
        long id = Long.parseLong(lineResponse1.header("Location").split("/")[2]);
        LineRequest putRequest = new LineRequest("신분당선", "white", 1L, 3L, 7);

        RestAssured.given().log().all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(OBJECT_MAPPER.writeValueAsString(putRequest))
            .when().put("/lines/" + id)
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }
}
