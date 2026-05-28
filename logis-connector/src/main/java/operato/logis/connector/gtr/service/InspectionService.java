package operato.logis.connector.gtr.service;

import operato.logis.connector.gtr.dto.InspectionRequestDto;
import operato.logis.connector.gtr.dto.InspectionResultDto;
import operato.logis.connector.gtr.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import xyz.elidom.sys.system.service.AbstractQueryService;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class InspectionService extends AbstractQueryService {

    private final WebClient webClient;
    // ObjectMapper도 JSON 변환 로직 삭제로 인해 사실상 필요 없어짐 (다른 용도로 안 쓴다면 삭제 가능)
    private final TokenService tokenService;

    private static final Logger log = LoggerFactory.getLogger(InspectionService.class);
    private final String LOCAL_IMAGE_ROOT_PATH = "\\\\192.168.100.151\\Upload\\";

    public InspectionService(@Qualifier("externalApiWebClient") WebClient webClient,
                             TokenService tokenService) {
        this.webClient = webClient;
        this.tokenService = tokenService;
    }

    /**
     * [메인] 검사 요청 -> DB 저장 -> DTO 반환
     * (리턴 타입 변경: Mono<Map> -> Mono<InspectionResultDto>)
     */
    public Mono<InspectionResultDto> requestInspection(String siteId, InspectionRequestDto dto) {
        return Mono.defer(() -> {
                    GtrToken token = tokenService.getToken();
                    VisionValue visionData = tokenService.getVisionData(dto.getTransactionId());

                    if (visionData == null) {
                        return Mono.error(new IllegalArgumentException("Vision Data not found for ID: " + dto.getTransactionId()));
                    }

//                    saveInspectionRequestData(visionData);

                    MultipartBodyBuilder builder = new MultipartBodyBuilder();
                    String simpleTimestamp = Instant.now().toString();

                    builder.part("transactionId", visionData.getSeqno() + '-' + visionData.getBarcodedata());
                    builder.part("zoneId", "Zone1");
                    builder.part("timestamp", simpleTimestamp);
                    builder.part("serialNumbers", visionData.getBarcodedata() != null ? visionData.getBarcodedata() : "");

                    addFilePart(builder, "front", visionData.getFilenamefront());
                    addFilePart(builder, "back", visionData.getFilenameback());
                    addFilePart(builder, "left", visionData.getFilenameleft());
                    addFilePart(builder, "right", visionData.getFilenameright());
                    addFilePart(builder, "top", visionData.getFilenametop());
                    addFilePart(builder, "bottomLeft", visionData.getFilenamebottomleft());
                    addFilePart(builder, "bottomRight", visionData.getFilenamebottomright());

                    return webClient.post()
                            .uri("/api/v1/inspections/" + siteId)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .header("Authorization", "Bearer " + token.getAccessToken())
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(InspectionResultDto.class);
                })
                // 💡 1. 타임아웃 설정: GTR(AI) 서버가 사진 7장을 분석하고 응답을 주는 데 걸리는 시간 고려
                // AI 검사가 무거울 수 있으므로 넉넉하게 15초로 설정 (이 시간은 재시도 간격과 무관하게 API 응답을 기다리는 최대 시간입니다)
                .timeout(Duration.ofSeconds(15))

                // 💡 2. 초고속 재시도 로직: 실패 시 0.5초 뒤에 다시 시도 (최대 4번 = 총 2초 대기)
                // 2초 안에 해결되면 다음 제품(2~3초 간격)과 충돌하지 않습니다.
                .retryWhen(Retry.fixedDelay(4, Duration.ofMillis(500))
                        .filter(this::isRetryable)
                        .doBeforeRetry(retrySignal ->
                                log.warn("[GTR 재시도] Seq: {}, 횟수: {}/4", dto.getTransactionId(), retrySignal.totalRetries() + 1)
                        )
                )
                .doOnError(ex -> {
                    // 끝내 실패했을 때의 에러 로깅 (400 에러 상세 사유 출력)
                    if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        var we = (org.springframework.web.reactive.function.client.WebClientResponseException) ex;
                        log.error("[GTR 최종 실패] Seq: {}, 사유: {}", dto.getTransactionId(), we.getResponseBodyAsString());
                    } else {
                        log.error("[GTR 최종 실패] Seq: {}, Error: {}", dto.getTransactionId(), ex.getMessage());
                    }
                })
                .map(resultDto -> {
                    saveInspectionData(resultDto);
                    return resultDto;
                });
    }

    @Transactional
    public void saveInspectionRequestData(VisionValue visionData) {
        try {
            GtrInspectionRequest requestEntity = new GtrInspectionRequest();
            requestEntity.setTransactionId(visionData.getSeqno() + '-' + visionData.getBarcodedata());
            requestEntity.setZoneId("Zone1");
            requestEntity.setSerialNumbers(visionData.getBarcodedata() != null ? visionData.getBarcodedata() : "");

            requestEntity.setFileFront(visionData.getFilenamefront());
            requestEntity.setFileBack(visionData.getFilenameback());
            requestEntity.setFileLeft(visionData.getFilenameleft());
            requestEntity.setFileRight(visionData.getFilenameright());
            requestEntity.setFileTop(visionData.getFilenametop());
            requestEntity.setFileBottomLeft(visionData.getFilenamebottomleft());
            requestEntity.setFileBottomRight(visionData.getFilenamebottomright());

            requestEntity.setDomainId(7L); // 기존 로직과 동일하게 도메인 ID 7 셋팅

            // Elidom ORM의 insert 호출
            this.queryManager.insert(requestEntity);
            log.info("검사 요청(Request) 이력 저장 완료: TransactionID={}", visionData.getSeqno() + '-' + visionData.getBarcodedata());

        } catch (Exception e) {
            log.error("검사 요청(Request) DB 저장 실패: TransactionID={}", visionData.getSeqno() + '-' + visionData.getBarcodedata(), e);
        }
    }

    /**
     * [CORE] 검사 결과 DB 저장 (Upsert)
     */
    @Transactional
    public void saveInspectionData(InspectionResultDto dto) {
        try {
            GtrInspectionResult existResult = tokenService.getResultInspection(dto.getTransactionId());

            if (existResult != null) {
                // Update
                log.info("검사 결과 업데이트: TransactionID={}", dto.getTransactionId());
                updateMainFields(existResult, dto);

                this.queryManager.update(existResult,
                        "zoneId", "timestamp", "serialNumbers",
                        "overallResult", "overallConfidence", "overallReason", "overallDamageClasses",
                        "updatedAt", "updaterId");

            } else {
                // Insert
                log.info("검사 결과 신규 등록: TransactionID={}", dto.getTransactionId());
                GtrInspectionResult newResult = new GtrInspectionResult();
                newResult.setTransactionId(dto.getTransactionId());
                newResult.setDomainId(7L);

                updateMainFields(newResult, dto);
                this.queryManager.insert(newResult);
            }

            // 자식 데이터 저장
            saveChildData(dto.getSides(), dto.getTransactionId());

        } catch (Exception e) {
            log.error("검사 결과 DB 저장 실패: TransactionID={}", dto.getTransactionId(), e);
        }
    }

    // [Helper Methods]
    private void updateMainFields(GtrInspectionResult entity, InspectionResultDto dto) {
        entity.setZoneId(dto.getZoneId());

        if (dto.getTimestamp() != null) {
            try {
                entity.setTimestamp(LocalDateTime.ofInstant(Instant.parse(dto.getTimestamp()), ZoneId.systemDefault()));
            } catch (Exception e) {
                log.warn("날짜 파싱 실패: {}", dto.getTimestamp());
            }
        }
        if (dto.getSerialNumbers() != null) {
            entity.setSerialNumbers(String.join(",", dto.getSerialNumbers()));
        }
        if (dto.getOverall() != null) {
            entity.setOverallResult(dto.getOverall().getResult());
            entity.setOverallConfidence(dto.getOverall().getConfidenceScore());
            entity.setOverallReason(dto.getOverall().getReason());
            if (dto.getOverall().getDamageClasses() != null) {
                entity.setOverallDamageClasses(String.join(",", dto.getOverall().getDamageClasses()));
            }
        }
    }

    private void saveChildData(List<InspectionResultDto.SideResultDto> sides, String transactionId) {
        if (sides == null) return;

        for (InspectionResultDto.SideResultDto sideDto : sides) {
            // Side 저장
            GtrSideResult sideEntity = new GtrSideResult();
            sideEntity.setMainTransactionId(transactionId);
            sideEntity.setSideName(sideDto.getSide());
            sideEntity.setSideResult(sideDto.getResult());
            sideEntity.setDomainId(7L);

            this.queryManager.insert(sideEntity);

            // Finding 저장
            if (sideDto.getFindings() != null) {
                for (InspectionResultDto.FindingDto findingDto : sideDto.getFindings()) {
                    GtrInspectionFinding findingEntity = new GtrInspectionFinding();
                    findingEntity.setSideResultId(sideEntity.getId());
                    findingEntity.setTransactionId(transactionId);

                    findingEntity.setDamageClass(findingDto.getDamageClass());
                    findingEntity.setConfidenceScore(findingDto.getConfidenceScore());
                    findingEntity.setReason(findingDto.getReason());
                    findingEntity.setDomainId(7L);

                    List<Integer> bbox = findingDto.getBoundingBox();
                    if (bbox != null && bbox.size() >= 4) {
                        findingEntity.setBboxXMin(bbox.get(0));
                        findingEntity.setBboxYMin(bbox.get(1));
                        findingEntity.setBboxXMax(bbox.get(2));
                        findingEntity.setBboxYMax(bbox.get(3));
                    }
                    this.queryManager.insert(findingEntity);
                }
            }
        }
    }

    // 파일 처리 관련
    private void addFilePart(MultipartBodyBuilder builder, String partName, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) return;
        String dateFolder = extractDateFolder(fileName);
        if (dateFolder == null) return;

        File file = Paths.get(LOCAL_IMAGE_ROOT_PATH, dateFolder, fileName).toFile();
        if (file.exists() && file.canRead()) {
            builder.part(partName, new FileSystemResource(file))
                    .filename(fileName)
                    .contentType(MediaType.IMAGE_JPEG);
        }
    }

    private String extractDateFolder(String fileName) {
        try {
            if (fileName.length() < 8) return null;
            String yyyy = fileName.substring(0, 4);
            String mm = fileName.substring(4, 6);
            String dd = fileName.substring(6, 8);
            Integer.parseInt(yyyy); Integer.parseInt(mm); Integer.parseInt(dd);
            return yyyy + "-" + mm + "-" + dd;
        } catch (Exception e) {
            return null;
        }
    }

    // 400 Bad Request(사진 누락) 이거나 500번대(타겟 서버 불안정) 에러일 때만 재시도
    private boolean isRetryable(Throwable error) {
        if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            var we = (org.springframework.web.reactive.function.client.WebClientResponseException) error;
            return we.getStatusCode().is4xxClientError() || we.getStatusCode().is5xxServerError();
        }
        return false;
    }
}