package operato.logis.lms.service.impl.dashboard;

import operato.logis.lms.LmsConstants;
import operato.logis.lms.dto.dashboard.ImageRequest;
import operato.logis.lms.entity.dashboard.StatusBoardDmt;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class StatusBoardDmtService extends AbstractQueryService {

    /**
     * Update DMT Image
     */
    public void uploadDmtImage(String lcId, String modelType, String dimension, String fileName) {
        // 기존 DMT 정보 조회
        StatusBoardDmt dmt = findOne(lcId, modelType);

        // 기존 이미지가 존재할 경우 해당 파일 삭제 후 파일 이름 업데이트
        if (LmsConstants.DIMENSION_2D.equals(dimension)) {
            if (ValueUtil.isNotEmpty(dmt.getImage2d())) {
                deleteDmtImage(dmt.getImage2d());
            }

            dmt.setImage2d(fileName);
            this.queryManager.update(dmt, "image2d");
        } else if (LmsConstants.DIMENSION_3D.equals(dimension)) {
            if (ValueUtil.isNotEmpty(dmt.getImage3d())) {
                deleteDmtImage(dmt.getImage3d());
            }

            dmt.setImage3d(fileName);
            this.queryManager.update(dmt, "image3d");
        }
    }

    /**
     * Delete DMT Image
     */
    private void deleteDmtImage(String fileName) {
        if (ValueUtil.isEmpty(fileName)) {
            return;
        }

        File file = new File(LmsConstants.STATUS_BOARD_IMAGE_PATH, fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Select DMT by LC_ID, MODEL_TYPE
     */
    public StatusBoardDmt findOne(String lcId, String modelType) {
        Query condition = OrmUtil.newConditionForExecution(LmsConstants.DOMAIN_ID);
        condition.addFilter("lcId", lcId);
        condition.addFilter("modelType", modelType);
        return this.queryManager.selectByCondition(StatusBoardDmt.class, condition);
    }

    /**
     * DMT Image 업로드
     */
    public ResponseEntity<String> uploadImage(ImageRequest request) {
        // 1. imageData가 비어있다면 기존 이미지 삭제 후 종료
        String base64Image = request.getImageData();
        if (base64Image == null || base64Image.isEmpty()) {
            uploadDmtImage(request.getLcId(), request.getModelType(), request.getDimension(), "");
            return ResponseEntity.ok("이미지 삭제 성공");
        }

        try {
            // 2. Base64 데이터 URL에서 순수 Base64 데이터 부분만 분리
            // "data:image/png;base64,iVBORw0KG..." -> "iVBORw0KG..."
            String[] parts = base64Image.split(",");
            if (parts.length != 2) {
                return ResponseEntity.badRequest().body("잘못된 형식의 이미지 데이터입니다.");
            }
            String imageString = parts[1];

            // 이미지 확장자 추출 (e.g., "data:image/jpeg;base64" -> "jpeg")
            String extension = parts[0].split("/")[1].split(";")[0];

            // 3. Base64 문자열을 byte 배열로 디코딩
            byte[] imageBytes = Base64.getDecoder().decode(imageString);

            // 4. 저장할 경로와 파일명 설정
            Path uploadPath = Paths.get(LmsConstants.STATUS_BOARD_IMAGE_PATH);

            // 5. 경로가 존재하지 않으면 폴더 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // 하위 폴더까지 모두 생성
            }

            // 6. 유니크한 파일명 생성 (파일 덮어쓰기 방지)
            String fileName = UUID.randomUUID() + "." + extension;
            Path filePath = uploadPath.resolve(fileName);

            // 7. byte 배열을 파일로 저장
            Files.write(filePath, imageBytes);

            // 8. DMT 업데이트
            uploadDmtImage(request.getLcId(), request.getModelType(), request.getDimension(), fileName);

            // 9. 성공 응답 반환 (저장된 파일 경로 또는 URL을 반환할 수도 있음)
            return ResponseEntity.ok("이미지 업로드 성공: " + fileName);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("이미지 저장 중 오류가 발생했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("잘못된 Base64 문자열입니다.");
        }
    }

    /**
     * DMT Image 다운로드
     */
    public ResponseEntity<Resource> downloadImage(String lcId, String modelType, String dimension) {
        // 1. 불러올 이미지 파일명 조회
        StatusBoardDmt dmt = findOne(lcId, modelType);
        String fileName = null;
        if (ValueUtil.isNotEmpty(dmt) && LmsConstants.DIMENSION_2D.equals(dimension)) {
            fileName = dmt.getImage2d();
        } else if (ValueUtil.isNotEmpty(dmt) && LmsConstants.DIMENSION_3D.equals(dimension)) {
            fileName = dmt.getImage3d();
        }

        // 1-1 등록된 파일이 없으면 Default 이미지 반환
        if (ValueUtil.isEmpty(fileName)) {
            dmt = findOne(LmsConstants.DEFAULT_LC_ID, modelType);
            if (LmsConstants.DIMENSION_2D.equals(dimension)) {
                fileName = dmt.getImage2d();
            } else if (LmsConstants.DIMENSION_3D.equals(dimension)) {
                fileName = dmt.getImage3d();
            }
        }

        // 1-2. 등록된 파일이 없으면 빈 이미지(byte[])로 생성
        if (ValueUtil.isEmpty(fileName)) {
            return returnEmptyByte();
        }

        try {
            // 2. 기본 경로와 파일명을 조합하여 파일의 전체 경로를 생성합니다.
            Path rootLocation = Paths.get(LmsConstants.STATUS_BOARD_IMAGE_PATH);
            Path filePath = rootLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // 3. 파일이 실제로 존재하고 읽을 수 있는지 확인합니다.
            if (resource.exists() && resource.isReadable()) {
                // 4. 파일의 MIME 타입을 동적으로 결정합니다. (e.g., image/png, image/jpeg)
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream"; // 타입을 모를 경우 기본값
                }

                // 5. 이미지를 ResponseEntity에 담아 반환합니다.
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                // 3-1. 실제 파일이 없으면 빈 이미지(byte[])로 생성
                return returnEmptyByte();
            }
        }
        catch (MalformedURLException e) {
            // 파일 경로가 잘못된 경우
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            // 파일 타입을 읽는 중 오류 발생 시
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> returnEmptyByte() {
        byte[] emptyImage = new byte[0];
        ByteArrayResource resource = new ByteArrayResource(emptyImage);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG) // 빈 PNG
                .contentLength(emptyImage.length)
                .body(resource);
    }
}