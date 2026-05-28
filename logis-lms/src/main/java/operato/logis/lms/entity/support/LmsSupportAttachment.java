package operato.logis.lms.entity.support;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.OrmConstants;

@Getter
@Setter
@Table(name = "lms_support_attachment", idStrategy = GenerationRule.UUID, notnullFields = "id,originFileName,storedFileName,extension,filePath,size,refId")
public class LmsSupportAttachment extends xyz.elidom.orm.entity.basic.ElidomStampHook {

    private static final long serialVersionUID = 7175925313343532536L;

    @PrimaryKey
    @Column(name = "id", nullable = false, length = OrmConstants.FIELD_SIZE_NAME, generator = GenerationRule.UUID)
    private String id;

    @Column(name = "origin_file_name", nullable = false)
    private String originFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "extension", nullable = false, length = 10)
    private String extension;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "ref_id", nullable = false, length = 64)
    private String refId; // support_id 또는 res_id

    public LmsSupportAttachment() {}

    public LmsSupportAttachment(String originFileName, String storedFileName, String extension, String filePath, Long size, String refId) {
        this.originFileName = originFileName;
        this.storedFileName = storedFileName;
        this.extension = extension;
        this.filePath = filePath;
        this.size = size;
        this.refId = refId;
    }
}