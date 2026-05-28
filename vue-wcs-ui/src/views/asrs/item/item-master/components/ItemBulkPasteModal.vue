<template>
  <div v-if="open" class="item-bulk-modal">
    <div class="item-bulk-modal__backdrop" @click="$emit('close')" />

    <div class="item-bulk-modal__dialog">
      <div class="item-bulk-modal__header">
        <div>
          <h3 class="item-bulk-modal__title">엑셀 일괄 붙여넣기</h3>
          <p class="item-bulk-modal__desc">
            엑셀에서 복사한 탭(tab) 구분 데이터를 그대로 붙여넣고 저장합니다.
          </p>
        </div>

        <div class="item-bulk-modal__header-actions">
          <AsrsActionButton variant="secondary" @click="downloadSampleTemplate">
            샘플 양식 다운로드
          </AsrsActionButton>

          <AsrsActionButton variant="ghost" @click="$emit('close')">
            닫기
          </AsrsActionButton>
        </div>
      </div>

      <div class="item-bulk-modal__guide">
        <strong>컬럼 순서</strong>
        <p>
          itemCode / itemName / categoryCode / operationProfileId / industryType / baseUom /
          handlingUnitType / outboundUnitType / lengthMm / widthMm / heightMm / weightG /
          storageTempType / lotControlYn / expiryControlYn / serialControlYn / partialPickYn /
          mixedLoadYn / fragileYn / heavyYn / quarantineRequiredYn / allocationRuleCode /
          rotationProfileCode / storageGradeSeed / activeYn / extAttr
        </p>
      </div>

      <div class="item-bulk-modal__editor">
        <label class="asrs-ui-label">붙여넣기 영역</label>
        <textarea
          :value="pasteText"
          class="item-bulk-modal__textarea"
          placeholder="엑셀 복사 후 여기에 붙여넣기"
          @input="onInput"
        />
      </div>

      <div class="item-bulk-modal__actions">
        <AsrsActionButton variant="secondary" @click="$emit('parse')">
          미리보기 생성
        </AsrsActionButton>

        <AsrsActionButton
          variant="primary"
          :disabled="!previewRows.length"
          :loading="loadingBulkSave"
          loading-text="저장 중..."
          @click="$emit('save')"
        >
          일괄 저장
        </AsrsActionButton>
      </div>

      <div class="item-bulk-modal__content-grid">
        <AsrsPanel eyebrow="Preview" title="미리보기">
          <template #actions>
            <span class="item-bulk-modal__count-badge">{{ previewRows.length }}건</span>
          </template>

          <div class="asrs-ui-table-wrap">
            <table class="asrs-ui-table">
              <thead>
              <tr>
                <th>Row</th>
                <th>Item Code</th>
                <th>Item Name</th>
                <th>Category</th>
                <th>Temp</th>
                <th>Length</th>
                <th>Width</th>
                <th>Height</th>
                <th>Weight</th>
                <th>Active</th>
              </tr>
              </thead>
              <tbody>
              <tr v-if="!previewRows.length">
                <td colspan="10" class="item-bulk-modal__empty-cell">
                  미리보기 데이터가 없습니다.
                </td>
              </tr>
              <tr v-for="row in previewRows" :key="row.rowNo">
                <td>{{ row.rowNo }}</td>
                <td class="asrs-ui-table__key">{{ row.itemCode }}</td>
                <td>{{ row.itemName }}</td>
                <td>{{ row.categoryCode }}</td>
                <td>{{ row.storageTempType }}</td>
                <td>{{ row.lengthMm }}</td>
                <td>{{ row.widthMm }}</td>
                <td>{{ row.heightMm }}</td>
                <td>{{ row.weightG }}</td>
                <td>{{ row.activeYn }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </AsrsPanel>

        <AsrsPanel eyebrow="Errors" title="오류 목록">
          <template #actions>
            <span class="item-bulk-modal__count-badge">{{ errors.length }}건</span>
          </template>

          <div class="asrs-ui-table-wrap">
            <table class="asrs-ui-table">
              <thead>
              <tr>
                <th>Row</th>
                <th>Item Code</th>
                <th>Message</th>
              </tr>
              </thead>
              <tbody>
              <tr v-if="!errors.length">
                <td colspan="3" class="item-bulk-modal__empty-cell">
                  오류가 없습니다.
                </td>
              </tr>
              <tr v-for="row in errors" :key="`${row.rowNo}-${row.itemCode}`">
                <td>{{ row.rowNo }}</td>
                <td class="asrs-ui-table__key">{{ row.itemCode || '-' }}</td>
                <td>{{ row.message || '-' }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </AsrsPanel>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 엑셀 일괄 붙여넣기 모달.
 *
 * 역할:
 * - 원문 입력
 * - preview 표시
 * - 오류 결과 표시
 * - 저장 이벤트 전달
 * - 샘플 양식(csv) 다운로드
 *
 * 정책:
 * - 1행: 한글 컬럼명 + 필수여부
 * - 2행: 실제 영문 컬럼 키
 * - 3행부터: 샘플 데이터
 */
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import type { ItemBulkPasteErrorRow, ItemBulkPasteRow } from '../types';

defineProps<{
  open: boolean;
  pasteText: string;
  previewRows: ItemBulkPasteRow[];
  errors: ItemBulkPasteErrorRow[];
  loadingBulkSave: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:pasteText', value: string): void;
  (e: 'parse'): void;
  (e: 'save'): void;
  (e: 'close'): void;
}>();

/**
 * textarea 입력 변경
 */
function onInput(event: Event) {
  const value = (event.target as HTMLTextAreaElement).value;
  emit('update:pasteText', value);
}

/**
 * CSV 셀 escape 처리
 */
function escapeCsvCell(value: unknown): string {
  const text = String(value ?? '');
  const escaped = text.replace(/"/g, '""');
  return `"${escaped}"`;
}

/**
 * 샘플 양식 다운로드
 *
 * 구성:
 * - 1행: 한글명(필수여부)
 * - 2행: 영문 key
 * - 3행 이후: 샘플 데이터
 */
function downloadSampleTemplate() {
  const headerDescriptions = [
    '상품코드(필수)',
    '상품명(필수)',
    '카테고리코드(필수)',
    '운영프로파일ID(선택)',
    '업종타입(필수)',
    '기본단위(필수)',
    '취급단위(필수)',
    '출고단위(필수)',
    '가로mm(필수)',
    '세로mm(필수)',
    '높이mm(필수)',
    '중량g(필수)',
    '보관온도타입(필수)',
    'LOT관리여부 Y/N(필수)',
    '유통기한관리여부 Y/N(필수)',
    '시리얼관리여부 Y/N(필수)',
    '부분피킹여부 Y/N(필수)',
    '혼적여부 Y/N(필수)',
    '파손주의여부 Y/N(필수)',
    '중량물여부 Y/N(필수)',
    '격리필요여부 Y/N(필수)',
    '할당규칙코드(필수)',
    '회전프로파일코드(필수)',
    '보관등급시드(필수)',
    '사용여부 Y/N(필수)',
    '확장속성(선택)',
  ];

  const headerKeys = [
    'itemCode',
    'itemName',
    'categoryCode',
    'operationProfileId',
    'industryType',
    'baseUom',
    'handlingUnitType',
    'outboundUnitType',
    'lengthMm',
    'widthMm',
    'heightMm',
    'weightG',
    'storageTempType',
    'lotControlYn',
    'expiryControlYn',
    'serialControlYn',
    'partialPickYn',
    'mixedLoadYn',
    'fragileYn',
    'heavyYn',
    'quarantineRequiredYn',
    'allocationRuleCode',
    'rotationProfileCode',
    'storageGradeSeed',
    'activeYn',
    'extAttr',
  ];

  const sampleRows = [
    [
      'AC-ITEM-100',
      '샘플 상품 100',
      'DEMO_FAST',
      'AC-OP-001',
      'GENERAL',
      'EA',
      'PALLET',
      'FULL',
      '400',
      '300',
      '200',
      '15000',
      'AMBIENT',
      'Y',
      'N',
      'N',
      'Y',
      'N',
      'N',
      'N',
      'N',
      'FIXED',
      'SLOW',
      'C',
      'Y',
      '{"memo":"sample"}',
    ],
    [
      'AC-ITEM-101',
      '샘플 상품 101',
      'DEMO_SLOW',
      '',
      'GENERAL',
      'EA',
      'BOX',
      'PARTIAL',
      '250',
      '180',
      '120',
      '8000',
      'CHILLED',
      'Y',
      'Y',
      'N',
      'Y',
      'Y',
      'N',
      'N',
      'N',
      'FREE',
      'FAST',
      'B',
      'Y',
      '',
    ],
  ];

  const csvLines = [
    headerDescriptions.map(escapeCsvCell).join(','),
    headerKeys.map(escapeCsvCell).join(','),
    ...sampleRows.map((row) => row.map(escapeCsvCell).join(',')),
  ];

  // Excel 한글 깨짐 방지용 BOM
  const csvContent = '\uFEFF' + csvLines.join('\n');

  const blob = new Blob([csvContent], {
    type: 'text/csv;charset=utf-8;',
  });

  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, '0');
  const dd = String(today.getDate()).padStart(2, '0');

  anchor.href = url;
  anchor.download = `item-master-bulk-template-${yyyy}${mm}${dd}.csv`;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}
</script>
