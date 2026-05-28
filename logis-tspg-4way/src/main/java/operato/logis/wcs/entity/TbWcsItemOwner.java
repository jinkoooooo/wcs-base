package operato.logis.wcs.entity;

import lombok.Getter;
import lombok.Setter;
import xyz.elidom.dbist.annotation.Column;
import xyz.elidom.dbist.annotation.GenerationRule;
import xyz.elidom.dbist.annotation.Index;
import xyz.elidom.dbist.annotation.PrimaryKey;
import xyz.elidom.dbist.annotation.Table;
import xyz.elidom.orm.entity.basic.ElidomStampHook;

/**
 * 고객사(화주사) 마스터 엔티티.
 * tb_inventory_stock.item_owner 의 마스터로, 고객사 코드·명칭·연락처·사업자번호 등 기본 정보를 관리한다.
 */
@Getter
@Setter
@Table(name = "tb_wcs_item_owner", idStrategy = GenerationRule.UUID,
        indexes = {
                @Index(name = "ix_tb_wcs_item_owner_1", columnList = "owner_code", unique = true)
        })
public class TbWcsItemOwner extends ElidomStampHook {

    @PrimaryKey
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    /** 고객사(화주) 코드 — tb_inventory_stock.item_owner 와 매핑 */
    @Column(name = "owner_code", nullable = false, length = 100)
    private String ownerCode;

    /** 고객사(화주) 명칭 */
    @Column(name = "owner_name", nullable = false, length = 200)
    private String ownerName;

    /** 사업자등록번호 */
    @Column(name = "biz_no", length = 20)
    private String bizNo;

    /** 대표자명 */
    @Column(name = "ceo_name", length = 100)
    private String ceoName;

    /** 연락처(전화) */
    @Column(name = "tel_no", length = 20)
    private String telNo;

    /** 이메일 */
    @Column(name = "email", length = 200)
    private String email;

    /** 주소 */
    @Column(name = "address", length = 500)
    private String address;

    /** 상세주소 */
    @Column(name = "address_detail", length = 500)
    private String addressDetail;

    /** 우편번호 */
    @Column(name = "zip_code", length = 10)
    private String zipCode;

    /** 사용 여부 */
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;

    /** 비고 */
    @Column(name = "remarks", length = 1000)
    private String remarks;
}