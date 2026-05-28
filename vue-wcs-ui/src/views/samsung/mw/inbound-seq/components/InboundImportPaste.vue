<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal-panel">
      <div class="page-header">
        <div>
          <h2 class="page-title">입고 리스트 직접 입력</h2>
          <p class="page-desc">엑셀 데이터 영역을 그대로 복사해서 붙여넣고 검토 후 저장</p>
        </div>

        <div class="header-actions">
          <button class="btn btn-ghost" @click="addRow">행 추가</button>
          <button class="btn btn-ghost" @click="addSample">샘플 1행</button>
          <button class="btn btn-ghost" @click="clearRows" :disabled="loading || !rows.length">전체 삭제</button>
          <button class="btn btn-primary" @click="save" :disabled="loading || !validRows.length">저장</button>
          <button class="btn btn-ghost" @click="close">닫기</button>
        </div>
      </div>

      <div class="guide-grid">
        <div class="card">
          <div class="section-title">입력 방법</div>
          <ul class="guide-list">
            <li>엑셀에서 헤더 제외 데이터 영역만 복사</li>
            <li>아래 붙여넣기 영역에 Ctrl+V</li>
            <li>그리드에서 값 확인 후 저장</li>
          </ul>
        </div>

        <div class="card card-accent">
          <div class="section-title">컬럼 순서</div>
          <div class="mono">bl_no / cntr_no / item_type / item_desc / item_code / item_qty / inbound_date</div>
          <div class="guide-note">입고일자는 화면 입력 시 YYYYMMDD, 전송 시 yyyy-MM-dd 변환</div>
        </div>
      </div>

      <div class="middle-grid">
        <div class="card">
          <div class="row-head">
            <div class="section-title">붙여넣기 영역</div>
            <div class="caption">엑셀 데이터 영역 복사 후 바로 붙여넣기</div>
          </div>

          <textarea
            ref="pasteAreaRef"
            v-model="pasteBuffer"
            class="paste-box"
            rows="4"
            placeholder="엑셀 데이터 영역 복사 후 여기에 Ctrl+V"
            @paste="handlePaste"
          />
        </div>

        <div class="card">
          <div class="row-head">
            <div class="section-title">입력 현황</div>
            <div class="caption">붙여넣기 후 자동 집계</div>
          </div>

          <div class="summary-inline">
            <div class="summary-item">
              <span class="summary-label">전체 행</span>
              <span class="summary-value">{{ rows.length }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">저장 가능</span>
              <span class="summary-value ok">{{ validRows.length }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">오류 행</span>
              <span class="summary-value ng">{{ invalidIndexes.length }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card table-card">
        <div class="row-head">
          <div class="section-title">입력 데이터</div>
          <div class="caption">행별 수정 가능. 오류 행은 붉은 배경으로 표시</div>
        </div>

        <div class="table-scroll">
          <table class="import-table">
            <colgroup>
              <col style="width: 70px" />
              <col style="width: 220px" />
              <col style="width: 200px" />
              <col style="width: 130px" />
              <col style="width: 220px" />
              <col style="width: 190px" />
              <col style="width: 120px" />
              <col style="width: 150px" />
              <col style="width: 90px" />
            </colgroup>
            <thead>
            <tr>
              <th>No</th>
              <th>bl_no</th>
              <th>cntr_no</th>
              <th>item_type</th>
              <th>item_desc</th>
              <th>item_code</th>
              <th>item_qty</th>
              <th>inbound_date</th>
              <th>삭제</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="(row, i) in rows" :key="row.rowKey" :class="{ 'row-invalid': !!getError(row) }">
              <td class="cell-center index-cell">{{ i + 1 }}</td>
              <td><input v-model.trim="row.bl_no" class="cell-input" /></td>
              <td><input v-model.trim="row.cntr_no" class="cell-input" /></td>
              <td><input v-model.trim="row.item_type" class="cell-input" /></td>
              <td><input v-model.trim="row.item_desc" class="cell-input" /></td>
              <td><input v-model.trim="row.item_code" class="cell-input" /></td>
              <td><input v-model.trim="row.item_qty" class="cell-input cell-center" /></td>
              <td><input v-model.trim="row.inbound_date" class="cell-input cell-center" /></td>
              <td class="cell-center">
                <button class="btn-delete" @click="removeRow(i)">삭제</button>
              </td>
            </tr>

            <tr v-if="!rows.length">
              <td colspan="9" class="empty-row">
                <div class="empty-title">입력된 데이터 없음</div>
                <div class="empty-desc">엑셀 데이터 복사 후 붙여넣기하거나 행 추가로 직접 입력</div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="invalidIndexes.length" class="card error-box">
        <div class="error-title">검증 오류</div>
        <div class="error-text">아래 행은 저장 대상에서 제외됨: {{ invalidIndexes.join(', ') }}</div>
        <ul class="error-list">
          <li v-for="msg in invalidMessages" :key="msg">{{ msg }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { callApi } from '../../common/api/callApi.js';

interface Row {
  rowKey: string;
  bl_no: string;
  cntr_no: string;
  item_type: string;
  item_desc: string;
  item_code: string;
  item_qty: string;
  inbound_date: string;
}
interface PayloadRow {
  bl_no: string;
  cntr_no: string;
  inbound_status: number;
  inbound_date: string;
  item_code: string;
  item_desc: string;
  item_qty: number;
  item_type: string;
}

const props = withDefaults(defineProps<{ visible: boolean }>(), { visible: false });
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'saved'): void;
}>();

const loading = ref(false);
const pasteBuffer = ref('');
const pasteAreaRef = ref<HTMLTextAreaElement | null>(null);
const rows = ref<Row[]>([]);
let seq = 0;

watch(() => props.visible, (v) => v && setTimeout(() => pasteAreaRef.value?.focus(), 0));

const nextKey = () => `row_${Date.now()}_${++seq}`;
const txt = (v: unknown) => String(v ?? '').trim();
const qty = (v: string) => txt(v).replace(/,/g, '');
const dateDigits = (v: string) => txt(v).replace(/[^0-9]/g, '');
const toApiDate = (v: string) => {
  const d = dateDigits(v);
  return d.length === 8 ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : txt(v);
};

function close() {
  resetState();
  emit('update:visible', false);
}
function emptyRow(): Row {
  return { rowKey: nextKey(), bl_no: '', cntr_no: '', item_type: '', item_desc: '', item_code: '', item_qty: '', inbound_date: '' };
}
function addRow() {
  rows.value.push(emptyRow());
}
function addSample() {
  rows.value.push({
    rowKey: nextKey(),
    bl_no: 'HASLS21251203223',
    cntr_no: 'HLHU8571363',
    item_type: 'VC',
    item_desc: '청소기',
    item_code: 'VR90F01AAG',
    item_qty: '547',
    inbound_date: '20260422',
  });
}
function removeRow(i: number) {
  rows.value.splice(i, 1);
}
function clearRows() {
  if (!confirm('전체 입력 행을 삭제하시겠습니까?')) return;
  rows.value = [];
  pasteBuffer.value = '';
}

function resetState() {
  rows.value = [];
  pasteBuffer.value = '';
  loading.value = false;
}

function parseClipboardText(text: string): Row[] {
  return text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => line.split('\t'))
    .filter((cols) => cols.some((x) => txt(x)))
    .filter((cols) => txt(cols[0]).toLowerCase() !== 'bl_no')
    .map((cols) => ({
      rowKey: nextKey(),
      bl_no: txt(cols[0]),
      cntr_no: txt(cols[1]),
      item_type: txt(cols[2]),
      item_desc: txt(cols[3]),
      item_code: txt(cols[4]),
      item_qty: qty(cols[5] ?? ''),
      inbound_date: dateDigits(cols[6] ?? ''),
    }));
}
function appendRows(list: Row[]) {
  if (list.length) rows.value = [...rows.value, ...list];
}
function handlePaste(e: ClipboardEvent) {
  const text = e.clipboardData?.getData('text/plain') ?? '';
  if (!text) return;
  e.preventDefault();
  appendRows(parseClipboardText(text));
  pasteBuffer.value = '';
}
async function pasteFromClipboard() {
  try {
    const text = await navigator.clipboard.readText();
    if (!text) return alert('클립보드에 텍스트가 없습니다.');
    appendRows(parseClipboardText(text));
  } catch (e) {
    console.error(e);
    alert('클립보드 읽기에 실패했습니다. 브라우저 권한을 확인해주세요.');
  }
}

function getError(row: Row): string {
  if (!row.bl_no) return 'bl_no 없음';
  if (!row.cntr_no) return 'cntr_no 없음';
  if (!row.item_type) return 'item_type 없음';
  if (!row.item_desc) return 'item_desc 없음';
  if (!row.item_code) return 'item_code 없음';
  if (!row.item_qty) return 'item_qty 없음';
  if (!/^\d+$/.test(row.item_qty)) return 'item_qty 숫자 아님';
  if (!row.inbound_date) return 'inbound_date 없음';
  if (dateDigits(row.inbound_date).length !== 8) return 'inbound_date 형식오류(YYYYMMDD 또는 YYYY-MM-DD)';
  return '';
}

const validRows = computed(() => rows.value.filter((r) => !getError(r)));
const invalidIndexes = computed(() => rows.value.map((r, i) => ({ i: i + 1, e: getError(r) })).filter((x) => x.e).map((x) => x.i));
const invalidMessages = computed(() => rows.value.map((r, i) => ({ i: i + 1, e: getError(r) })).filter((x) => x.e).map((x) => `${x.i}행: ${x.e}`));

function buildPayload(list: Row[]): PayloadRow[] {
  return list.map((r) => ({
    bl_no: r.bl_no,
    cntr_no: r.cntr_no,
    inbound_status: 0,
    inbound_date: toApiDate(r.inbound_date),
    item_code: r.item_code,
    item_desc: r.item_desc,
    item_qty: Number(r.item_qty),
    item_type: r.item_type,
  }));
}

async function save() {
  if (!validRows.value.length) return alert('저장할 유효 데이터가 없습니다.');

  try {
    loading.value = true;
    const res = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/import_inbound_delivery`,
      buildPayload(validRows.value),
    );

    const missingCount = Number(res?.missingMaterialCount || 0);
    const missingMaterials: string[] = res?.missingMaterials || [];
    const insertedDeliveryCount = Number(res?.insertedDeliveryCount || 0);
    const insertedJobCount = Number(res?.insertedJobCount || 0);
    const skippedGroupCount = Number(res?.skippedGroupCount || 0);
    const skippedGroups: Array<{ bl_no: string; cntr_no: string }> = res?.skippedGroups || [];

    if (missingCount > 0) {
      const lines = missingMaterials.slice(0, 50).map((x) => `- ${x}`).join('\n');
      return alert(`업로드 중단: 상품마스터에 없는 상품코드(item_code) ${missingCount}건\n\n누락 코드 목록:\n${lines}${missingMaterials.length > 50 ? `\n... 외 ${missingMaterials.length - 50}건` : ''}`);
    }

    if (insertedDeliveryCount === 0 && skippedGroupCount > 0) {
      const list = skippedGroups.slice(0, 10).map((g) => `${g.bl_no}/${g.cntr_no}`).join('\n');
      return alert(`업로드 결과: 저장된 데이터가 없습니다.\n이미 등록된 주문/컨테이너 그룹 ${skippedGroupCount}건이 제외되었습니다.\n\n${list}${skippedGroups.length > 10 ? `\n... 외 ${skippedGroups.length - 10}건` : ''}`);
    }

    if (insertedDeliveryCount > 0 && skippedGroupCount > 0) {
      const list = skippedGroups.slice(0, 10).map((g) => `${g.bl_no}/${g.cntr_no}`).join('\n');
      alert(`업로드 완료\n\n- Job 생성: ${insertedJobCount}건\n- Delivery 저장: ${insertedDeliveryCount}건\n- 제외(이미 존재): ${skippedGroupCount}건\n\n제외 목록(일부):\n${list}${skippedGroups.length > 10 ? `\n... 외 ${skippedGroups.length - 10}건` : ''}`);
    } else {
      alert(`업로드 완료\n\n- Job 생성: ${insertedJobCount}건\n- Delivery 저장: ${insertedDeliveryCount}건`);
    }

    resetState();
    emit('saved');
    emit('update:visible', false);
  } catch (e) {
    console.error(e);
    alert('저장 중 오류가 발생했습니다.');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="less">
.modal-overlay{position:fixed;inset:0;z-index:2000;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(15,23,42,.5);backdrop-filter:blur(4px)}
.modal-panel{width:min(1520px,96vw);height:min(920px,94vh);display:flex;flex-direction:column;gap:12px;padding:20px;border-radius:22px;background:#f8fafc;border:1px solid #e2e8f0;box-shadow:0 24px 60px rgba(15,23,42,.28);overflow:hidden}
.page-header,.row-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}
.page-title{font-size:1.55rem;font-weight:800;color:#0f172a}.page-desc,.caption,.guide-note{margin-top:4px;font-size:.84rem;color:#64748b}
.header-actions{display:flex;flex-wrap:wrap;gap:8px;justify-content:flex-end}
.guide-grid,.middle-grid{display:grid;gap:12px}.guide-grid{grid-template-columns:1.2fr 1fr}.middle-grid{grid-template-columns:1fr 1fr}
.card{padding:14px;border:1px solid #e2e8f0;border-radius:16px;background:#fff;box-shadow:0 1px 2px rgba(15,23,42,.04)}.card-accent{background:linear-gradient(135deg,#eef2ff,#fff);border-color:#c7d2fe}
.section-title{font-size:.93rem;font-weight:700;color:#1e293b}.guide-list{margin:10px 0 0;padding-left:18px;color:#475569;font-size:.89rem;line-height:1.8}.mono{margin-top:10px;font-family:Consolas,Monaco,monospace;font-size:.9rem;line-height:1.6;color:#334155;word-break:break-all}
.paste-box{width:100%;height:88px;min-height:88px;max-height:88px;padding:12px 14px;border:1px solid #dbe3ee;border-radius:12px;outline:none;resize:none;background:#fff;font-size:.9rem;line-height:1.5}.paste-box:focus,.cell-input:focus{border-color:#6366f1;box-shadow:0 0 0 3px rgba(99,102,241,.1)}
.summary-inline{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}
.summary-item{height:88px;padding:12px 14px;border:1px solid #e5e7eb;border-radius:12px;background:#fff;display:flex;flex-direction:column;justify-content:space-between}
.summary-label{font-size:.82rem;color:#64748b}.summary-value{font-size:1.45rem;font-weight:800;color:#0f172a;text-align:right}.summary-value.ok{color:#059669}.summary-value.ng{color:#dc2626}
.table-card{flex:1;min-height:0;display:flex;flex-direction:column}.table-scroll{flex:1;min-height:0;overflow:auto;margin-top:8px;border:1px solid #e5e7eb;border-radius:14px}
.import-table{width:100%;min-width:1280px;border-collapse:separate;border-spacing:0;background:#fff}.import-table thead th{position:sticky;top:0;z-index:2;padding:12px 8px;text-align:center;font-size:.84rem;font-weight:700;color:#334155;background:#f8fafc;border-bottom:1px solid #e5e7eb}.import-table tbody td{padding:10px 8px;border-bottom:1px solid #eef2f7;background:#fff}
.row-invalid td{background:#fff7f7!important}.cell-center{text-align:center}.index-cell{font-weight:700;color:#475569}
.cell-input{width:100%;height:38px;padding:0 12px;border:1px solid #dbe3ee;border-radius:10px;outline:none;background:#fff;font-size:.88rem;color:#111827}
.btn-delete{min-width:58px;height:34px;border:1px solid #fecaca;background:#fff1f2;color:#dc2626;border-radius:10px;cursor:pointer;font-size:.8rem;font-weight:700}.btn-delete:hover{background:#ffe4e6}
.empty-row{text-align:center;padding:44px 16px!important}.empty-title{font-size:.95rem;font-weight:700;color:#334155}.empty-desc{margin-top:6px;font-size:.83rem;color:#64748b}
.error-box{background:linear-gradient(135deg,#fff7f7,#fff);border-color:#fecaca}.error-title{font-size:.9rem;font-weight:800;color:#b91c1c}.error-text{margin-top:6px;font-size:.83rem;color:#7f1d1d}.error-list{margin-top:8px;padding-left:18px;color:#7f1d1d;font-size:.82rem;line-height:1.5}
.btn{display:inline-flex;align-items:center;justify-content:center;height:38px;padding:0 14px;border:1px solid #e5e7eb;border-radius:12px;background:#fff;font-size:.85rem;font-weight:700;color:#334155;cursor:pointer;transition:.15s}.btn:active{transform:scale(.98)}.btn:disabled{opacity:.55;cursor:not-allowed}.btn-ghost:hover:not(:disabled){background:#f8fafc;border-color:#cbd5e1}.btn-primary{background:#5b61f6;border-color:#5b61f6;color:#fff}.btn-primary:hover:not(:disabled){background:#4f46e5;border-color:#4f46e5}
@media (max-width:1200px){.page-header{flex-direction:column}.header-actions{width:100%;justify-content:flex-start}.guide-grid,.middle-grid,.summary-inline{grid-template-columns:1fr}.summary-item{height:auto;min-height:72px}}
</style>
