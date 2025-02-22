package com.postsquad.scoup.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.postsquad.scoup.web.auth.OAuthType;
import com.postsquad.scoup.web.user.domain.OAuthUser;
import com.postsquad.scoup.web.user.domain.User;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.snippet.Attributes.Attribute;
import org.springframework.restdocs.snippet.Snippet;

import java.util.List;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.snippet.Attributes.key;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(RestDocumentationExtension.class)
public class AcceptanceTestBase {

    // temporary token with sub(userId) = 1, exp = 2023-01-01T00:00:00.000(Korean Standard Time)
    protected static final String TEST_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiZXhwIjoxNjcyNDk4ODAwfQ.DXojMeUGIq77XWvQK0luZtZhsi-c6s9qjiiu9vHhkbg";

    protected static final String BASE_URL = "http://localhost";

    protected static final String DEFAULT_RESTDOCS_PATH = "{class_name}/{method_name}/";

    protected static final Snippet ERROR_RESPONSE_FIELDS = responseFields(
            fieldWithPath("timestamp")
                    .type(JsonFieldType.STRING)
                    .description("에러 발생 날짜"),
            fieldWithPath("message")
                    .type(JsonFieldType.STRING)
                    .description("에러 메세지"),
            fieldWithPath("status_code")
                    .type(JsonFieldType.NUMBER)
                    .description("상태 코드"),
            fieldWithPath("errors")
                    .type(JsonFieldType.ARRAY)
                    .description("에러 상세 메세지")
    );

    protected static final Snippet DEFAULT_POST_RESPONSE_FIELDS = responseFields(
            fieldWithPath("id")
                    .type(JsonFieldType.NUMBER)
                    .description("id")
    );

    @LocalServerPort
    protected int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    protected ObjectMapper objectMapper;

    protected RequestSpecification spec;

    protected User testUser = User.builder()
                                  .nickname("nickname")
                                  .email("email@email.com")
                                  .password("password")
                                  .avatarUrl("url")
                                  .username("username")
                                  .oAuthUsers(List.of(OAuthUser.of(OAuthType.NONE, "")))
                                  .build();

    protected String[] ignoringFieldsForResponseWithId = new String[]{"createdDateTime", "modifiedDateTime", "id"};

    protected String[] ignoringFieldsForResponse = new String[]{"createdDateTime", "modifiedDateTime"};

    protected String[] ignoringFieldsForErrorResponse = new String[]{"timestamp"};

    {
        setUpRestAssured();
    }

    @BeforeEach
    void cleanUpDatabase() {
        databaseCleanup.execute();
    }

    @BeforeEach
    void setUpRestDocs(RestDocumentationContextProvider restDocumentation) {
        this.spec = new RequestSpecBuilder()
                .addFilter(documentationConfiguration(restDocumentation)
                                   .operationPreprocessors()
                                   .withRequestDefaults(prettyPrint())
                                   .withResponseDefaults(prettyPrint())
                ).build();
    }

    protected static FieldDescriptor fieldWithPathAndConstraints(String path, Class<?> clazz) {
        FieldDescriptor fieldWithPath = fieldWithPath(path);

        ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(clazz);
        String camelCasePath = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, path);
        Attribute constraints = key("constraints").value(constraintDescriptions.descriptionsForProperty(camelCasePath));
        FieldDescriptor fieldWithPathAndConstraints = fieldWithPath.attributes(constraints);

        return fieldWithPathAndConstraints;
    }

    protected static ParameterDescriptor parameterWithNameAndConstraints(String path, Class<?> clazz) {
        ParameterDescriptor parameterWithName = parameterWithName(path);

        ConstraintDescriptions constraintDescriptions = new ConstraintDescriptions(clazz);
        String camelCasePath = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, path);
        Attribute constraints = key("constraints").value(constraintDescriptions.descriptionsForProperty(camelCasePath));
        ParameterDescriptor fieldWithPathAndConstraints = parameterWithName.attributes(constraints);

        return fieldWithPathAndConstraints;
    }

    private void setUpRestAssured() {
        Jackson2Mapper jackson2Mapper = new Jackson2Mapper((type, charset) -> objectMapper);
        ObjectMapperConfig jackson2ObjectMapperConfig = new ObjectMapperConfig(jackson2Mapper);

        RestAssured.config = RestAssuredConfig.config()
                                              .objectMapperConfig(jackson2ObjectMapperConfig);
    }
}
