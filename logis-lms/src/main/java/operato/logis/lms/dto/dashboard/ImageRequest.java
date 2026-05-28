package operato.logis.lms.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageRequest {
    private String imageData;

    private String lcId;

    private String modelType;

    private String dimension;
}