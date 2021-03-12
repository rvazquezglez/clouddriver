/*
 * Copyright 2018 Pivotal, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.cloudfoundry.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ForkJoinPool;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.lanwen.wiremock.ext.WiremockResolver;

@ExtendWith({WiremockResolver.class})
class HttpCloudFoundryClientTest {
  @Test
  void createRetryInterceptorShouldRetryOnInternalServerErrorsThenTimeOut(
      @WiremockResolver.Wiremock WireMockServer server) throws Exception {
    stubServer(
        server,
        200,
        STARTED,
        "Will respond 502",
        "{\"access_token\":\"token\",\"expires_in\":1000000}");
    stubServer(
        server,
        502,
        "Will respond 502",
        "Will respond 503",
        "{\"errors\":[{\"detail\":\"502 error\"}]}");
    stubServer(
        server,
        503,
        "Will respond 503",
        "Will respond 504",
        "{\"errors\":[{\"detail\":\"503 error\"}]}");
    stubServer(
        server,
        504,
        "Will respond 504",
        "Will respond 200",
        "{\"errors\":[{\"detail\":\"504 error\"}]}");
    stubServer(server, 200, "Will respond 200", "END", "{}");

    HttpCloudFoundryClient cloudFoundryClient =
        new HttpCloudFoundryClient(
            "account",
            "appsManUri",
            "metricsUri",
            "localhost:" + server.port() + "/",
            "user",
            "password",
            true,
            500,
            ForkJoinPool.commonPool(),
            new OkHttpClient.Builder(),
            new SimpleMeterRegistry());

    CloudFoundryApiException thrown =
        assertThrows(
            CloudFoundryApiException.class,
            () -> cloudFoundryClient.getOrganizations().findByName("randomName"),
            "Expected thrown 'Cloud Foundry API returned with error(s): 504 error', but it didn't");

    // 504 means it was retried after 502 and 503
    assertTrue(thrown.getMessage().contains("Cloud Foundry API returned with error(s): 504 error"));
  }

  private void stubServer(
      WireMockServer server, int status, String currentState, String nextState) {
    stubServer(server, status, currentState, nextState, "");
  }

  private void stubServer(
      WireMockServer server, int status, String currentState, String nextState, String body) {
    server.stubFor(
        any(UrlPattern.ANY)
            .inScenario("Retry Scenario")
            .whenScenarioStateIs(currentState)
            .willReturn(
                aResponse()
                    .withStatus(status) // request unsuccessful with status code 500
                    .withHeader("Content-Type", "application/json")
                    .withBody(body))
            .willSetStateTo(nextState));
  }

  //  @Test
  //  void createRetryInterceptorShouldNotRefreshTokenOnBadCredentials() {
  //    Request request = new Request.Builder().url("http://duke.of.url").build();
  //    ResponseBody body =
  //      ResponseBody.create(MediaType.parse("application/octet-stream"), "Bad credentials");
  //    Response response401 =
  //      new
  // Response.Builder().code(401).request(request).body(body).protocol(HTTP_1_1).message("").build();
  //    Interceptor.Chain chain = mock(Interceptor.Chain.class);
  //
  //    when(chain.request()).thenReturn(request);
  //    try {
  //      when(chain.proceed(any())).thenReturn(response401);
  //    } catch (IOException e) {
  //      fail("Should not happen!");
  //    }
  //
  //    HttpCloudFoundryClient cloudFoundryClient =
  //      new HttpCloudFoundryClient(
  //        "account",
  //        "appsManUri",
  //        "metricsUri",
  //        "host",
  //        "user",
  //        "password",
  //        false,
  //        500,
  //        ForkJoinPool.commonPool(),
  //        new OkHttpClient().newBuilder(),
  //        new SimpleMeterRegistry());
  //    Response response = null; // cloudFoundryClient.createRetryInterceptor(chain);
  //
  //    try {
  //      verify(chain, times(1)).proceed(eq(request));
  //    } catch (IOException e) {
  //      fail("Should not happen!");
  //    }
  //    assertThat(response).isEqualTo(response401);
  //  }
  //
  //  @Test
  //  void createRetryInterceptorShouldReturnOnEverythingElse() {
  //    Request request = new Request.Builder().url("http://duke.of.url").build();
  //    Response response502 =
  //      new Response.Builder().code(502).request(request).protocol(HTTP_1_1).message("").build();
  //    Response response200 =
  //      new Response.Builder().code(200).request(request).protocol(HTTP_1_1).message("").build();
  //    Interceptor.Chain chain = mock(Interceptor.Chain.class);
  //
  //    when(chain.request()).thenReturn(request);
  //    try {
  //      when(chain.proceed(any())).thenReturn(response502, response200);
  //    } catch (IOException e) {
  //      fail("Should not happen!");
  //    }
  //
  //    HttpCloudFoundryClient cloudFoundryClient =
  //      new HttpCloudFoundryClient(
  //        "account",
  //        "appsManUri",
  //        "metricsUri",
  //        "host",
  //        "user",
  //        "password",
  //        false,
  //        500,
  //        ForkJoinPool.commonPool(),
  //        new OkHttpClient().newBuilder(),
  //        new SimpleMeterRegistry());
  //    Response response = null; // cloudFoundryClient.createRetryInterceptor(chain);
  //
  //    try {
  //      verify(chain, times(2)).proceed(eq(request));
  //    } catch (IOException e) {
  //      fail("Should not happen!");
  //    }
  //    assertThat(response).isEqualTo(response200);
  //  }

  //  @Test
  //  void protobufDopplerEnvelopeConverter_convertsMultipartResponse() throws ConversionException {
  //    Converter converter = new ProtobufDopplerEnvelopeConverter();
  //
  //    List<Envelope> envelopes = (List<Envelope>) converter.fromBody(new TestingTypedInput(),
  // null);
  //
  //    assertThat(envelopes.size()).isEqualTo(14);
  //  }

  //  class TestingTypedInput implements TypedInput {
  //    private final File multipartProtobufLogs;
  //
  //    TestingTypedInput() {
  //      ClassLoader classLoader = getClass().getClassLoader();
  //      try {
  //        multipartProtobufLogs = new
  // File(classLoader.getResource("doppler.recent.logs").toURI());
  //      } catch (URISyntaxException e) {
  //        throw new RuntimeException(e);
  //      }
  //    }
  //
  //    @Override
  //    public String mimeType() {
  //      return "multipart/x-protobuf;
  // boundary=a7d612f5da24eb116b1c0889c112d0a1beecd7e640d921ad9210100e2f77";
  //    }
  //
  //    @Override
  //    public long length() {
  //      return multipartProtobufLogs.length();
  //    }
  //
  //    @Override
  //    public InputStream in() throws FileNotFoundException {
  //      return new FileInputStream(multipartProtobufLogs);
  //    }
  //  }
}
