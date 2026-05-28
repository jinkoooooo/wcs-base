package operato.logis.connector.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final WebClient webClient;

    // 재시도 관련 상수 정의
    private static final int MAX_RETRY_ATTEMPTS = 2; // 최대 재시도 횟수 (총 3회 시도)
    private static final int TIMEOUT_SECONDS = 5;    // 타임아웃 시간 (초)

    /**
     * 제네릭을 사용한 공통 POST 요청 메소드 (타임아웃 및 재시도 기능 포함)
     *
     * @param url 요청할 전체 URL
     * @param requestBody 요청 시 보낼 객체
     * @param responseType 응답을 변환할 객체의 Class 타입
     * @param <T> 요청 객체 타입
     * @param <R> 응답 객체 타입
     * @return Mono<R> 응답 객체를 포함하는 Mono
     */
    public <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType) {
        // 1. POST 요청 스펙(spec)을 생성합니다.
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.post()
                .uri(url)
                .bodyValue(requestBody);

        // 2. 공통 실행 메소드를 호출합니다.
        return executeRequest(requestSpec, responseType, url);
    }

    /**
     * 제네릭을 사용한 공통 GET 요청 메소드 (타임아웃 및 재시도 기능 포함)
     *
     * @param url 요청할 전체 URL
     * @param responseType 응답을 변환할 객체의 Class 타입
     * @param <R> 응답 객체 타입
     * @return Mono<R> 응답 객체를 포함하는 Mono
     */
    public <R> Mono<R> get(String url, Class<R> responseType) {
        // 1. GET 요청 스펙(spec)을 생성합니다.
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
                .uri(url);

        // 2. 공통 실행 메소드를 호출합니다.
        return executeRequest(requestSpec, responseType, url);
    }

    /**
     * 제네릭을 사용한 공통 DELETE 요청 메소드 (새로 추가됨)
     */
    public <R> Mono<R> delete(String url, Class<R> responseType) {
        WebClient.RequestHeadersSpec<?> requestSpec = webClient.delete()
                .uri(url);
        return executeRequest(requestSpec, responseType, url);
    }

    /**
     * WebClient 요청을 실행하고 공통 로직(에러처리, 타임아웃, 재시도)을 적용합니다.
     */
    private <R> Mono<R> executeRequest(WebClient.RequestHeadersSpec<?> requestSpec, Class<R> responseType, String url) {
        return requestSpec
                .retrieve() // API 호출 및 응답 수신
                .onStatus(
                        httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("API Error: status={}, url={}, body={}", clientResponse.statusCode(), url, errorBody);
                                    return Mono.error(new RuntimeException("API call failed with status: " + clientResponse.statusCode()));
                                })
                )
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof TimeoutException)
                        .doBeforeRetry(context -> log.warn("Request to {} timed out. Retrying... ({}/{})",
                                url, context.totalRetries() + 1, MAX_RETRY_ATTEMPTS))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.warn("Request to {} failed after {} retries.", url, MAX_RETRY_ATTEMPTS); // 여기서 fail 난뒤에 로그에 에러로그가 스택으로 다 찍히는데, 그냥 한줄말 찍히도록 하고싶어.
                            return retrySignal.failure();
                        }))
                .onErrorResume(TimeoutException.class, ex -> {
                    // 최종 타임아웃도 여기서 한 줄만 로그 찍고 Mono.empty()로 흡수
                    log.warn("Request to {} 최종 타임아웃: {}", url, ex.getMessage());
                    return Mono.empty();
                });
    }
}