<!--
  박스 QR 정보 화면.

  - QR 안에 있는 박스/파렛트 바코드를 path param (`code`) 으로 받아
    백엔드 `/rest/wcs/qr/scan` 을 호출하여 정보를 표시한다.
  - 박스: 박스 메타(품목/Lot/수량/생산·유통기한) + 사용기한 색상 배지
          + 같은 SKU+lot 의 rack 전체 재고 집계
  - 파렛트: 파렛트 메타 + 박스 리스트 (각 박스 잔량/유통기한 배지)
-->
<template>
  <div class="box-qr-view">
    <header class="qr-header">
      <h2>QR 정보</h2>
      <div class="qr-code">{{ code }}</div>
    </header>

    <div v-if="loading" class="qr-loading">조회 중...</div>

    <div v-else-if="error" class="qr-error">
      <p>조회 실패: {{ error }}</p>
      <button class="btn" @click="reload">재시도</button>
    </div>

    <template v-else-if="data && data.type === 'BOX'">
      <section class="info-card">
        <div class="info-card-title">
          <span class="badge badge-type">박스</span>
          <span class="info-card-subtitle">{{ data.boxBarcode }}</span>
          <span
            v-if="data.expiryStatus"
            class="badge"
            :class="expiryClass(data.expiryStatus)"
          >
            {{ expiryLabel(data.expiryStatus) }}
            <template v-if="data.daysToExpiry != null">
              · D{{ data.daysToExpiry >= 0 ? '-' : '+' }}{{ Math.abs(data.daysToExpiry) }}
            </template>
          </span>
        </div>

        <dl class="info-grid">
          <div><dt>파렛트</dt><dd>{{ data.palletBarcode || '-' }}</dd></div>
          <div><dt>박스 순번</dt><dd>{{ data.boxSeq ?? '-' }}</dd></div>
          <div><dt>품목코드</dt><dd>{{ data.itemCode || '-' }}</dd></div>
          <div><dt>품목명</dt><dd>{{ data.itemName || '-' }}</dd></div>
          <div><dt>Lot</dt><dd>{{ data.lotNo || '-' }}</dd></div>
          <div><dt>박스상태</dt><dd>{{ boxStatusLabel(data.boxStatus) }}</dd></div>
          <div><dt>총수량</dt><dd>{{ data.totalQty ?? 0 }} {{ data.uom || '' }}</dd></div>
          <div><dt>출고 누적</dt><dd>{{ data.pickedQty ?? 0 }}</dd></div>
          <div><dt>잔량</dt><dd>{{ data.remainingQty ?? 0 }}</dd></div>
          <div><dt>생산일자</dt><dd>{{ data.produceDate || '-' }}</dd></div>
          <div><dt>사용기한</dt><dd>{{ data.expiryDate || '-' }}</dd></div>
          <div><dt>시험의뢰번호</dt><dd>{{ data.testRequestNo || '-' }}</dd></div>
          <div><dt>시험번호</dt><dd>{{ data.testNo || '-' }}</dd></div>
        </dl>
      </section>

      <section class="info-card">
        <div class="info-card-title">
          <span class="badge badge-aggregate">동일 SKU + Lot 전체 재고</span>
        </div>
        <dl class="info-grid">
          <div>
            <dt>총 재고</dt>
            <dd>{{ rackTotalQty }} {{ data.uom || '' }}</dd>
          </div>
          <div>
            <dt>재고 행 수</dt>
            <dd>{{ rackRowCount }}</dd>
          </div>
          <div>
            <dt>SKU</dt>
            <dd>{{ data.itemCode || '-' }}</dd>
          </div>
          <div>
            <dt>Lot</dt>
            <dd>{{ data.lotNo || '-' }}</dd>
          </div>
        </dl>
      </section>
    </template>

    <template v-else-if="data && data.type === 'PALLET'">
      <section class="info-card">
        <div class="info-card-title">
          <span class="badge badge-type pallet">파렛트</span>
          <span class="info-card-subtitle">{{ data.palletBarcode }}</span>
        </div>
        <dl class="info-grid">
          <div><dt>호스트 주문</dt><dd>{{ data.hostOrderKey || '-' }}</dd></div>
          <div><dt>존</dt><dd>{{ data.eqGroupId || '-' }}</dd></div>
          <div><dt>화주</dt><dd>{{ data.ownerCode || '-' }}</dd></div>
          <div><dt>입고일자</dt><dd>{{ data.inboundDate || '-' }}</dd></div>
          <div><dt>박스 수</dt><dd>{{ data.boxCount ?? 0 }}</dd></div>
          <div><dt>총 잔량</dt><dd>{{ data.totalRemaining ?? 0 }}</dd></div>
        </dl>
      </section>

      <section class="info-card" v-if="(data.boxes || []).length">
        <div class="info-card-title">
          <span class="badge badge-aggregate">박스 리스트</span>
        </div>
        <table class="boxes-table">
          <thead>
            <tr>
              <th>#</th>
              <th>박스바코드</th>
              <th>품목</th>
              <th>Lot</th>
              <th>총</th>
              <th>출고</th>
              <th>잔량</th>
              <th>사용기한</th>
              <th>상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(b, i) in data.boxes || []" :key="b.boxId">
              <td>{{ b.boxSeq ?? i + 1 }}</td>
              <td>{{ b.boxBarcode }}</td>
              <td>{{ b.itemCode || '-' }}</td>
              <td>{{ b.lotNo || '-' }}</td>
              <td>{{ b.totalQty ?? 0 }}</td>
              <td>{{ b.pickedQty ?? 0 }}</td>
              <td>{{ b.remainingQty ?? 0 }}</td>
              <td>
                <span
                  v-if="b.expiryStatus"
                  class="badge sm"
                  :class="expiryClass(b.expiryStatus)"
                >
                  {{ expiryLabel(b.expiryStatus) }}
                </span>
              </td>
              <td>{{ boxStatusLabel(b.boxStatus) }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import {
  scanQr,
  type QrBoxScanResponse,
  type QrPalletScanResponse,
  type QrScanResponse,
} from "../api/QrTabletApi";

const route = useRoute();
const code = computed<string>(() => String(route.params.code || ""));

const loading = ref(false);
const error = ref<string | null>(null);
const data = ref<QrScanResponse | null>(null);

async function load() {
  if (!code.value) {
    error.value = "code 파라미터가 비어있습니다";
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    data.value = await scanQr(code.value);
  } catch (e: any) {
    error.value = e?.message || String(e);
    data.value = null;
  } finally {
    loading.value = false;
  }
}

function reload() {
  load();
}

onMounted(load);
watch(code, load);

// =========================================================================
// 표시 헬퍼
// =========================================================================

const rackTotalQty = computed(() => {
  const d = data.value as QrBoxScanResponse | null;
  const v = d?.aggregateBySkuLot?.total_qty;
  return typeof v === "number" ? v : 0;
});

const rackRowCount = computed(() => {
  const d = data.value as QrBoxScanResponse | null;
  const v = d?.aggregateBySkuLot?.row_count;
  return typeof v === "number" ? v : 0;
});

function expiryClass(status?: string): string {
  switch (status) {
    case "NORMAL":  return "badge-expiry-normal";
    case "WARN":    return "badge-expiry-warn";
    case "ALERT":   return "badge-expiry-alert";
    case "EXPIRED": return "badge-expiry-expired";
    default:        return "badge-expiry-unknown";
  }
}

function expiryLabel(status?: string): string {
  switch (status) {
    case "NORMAL":  return "정상";
    case "WARN":    return "주의";
    case "ALERT":   return "경고";
    case "EXPIRED": return "만료";
    default:        return "미상";
  }
}

function boxStatusLabel(s?: number): string {
  switch (s) {
    case 0:  return "미발행";
    case 1:  return "발행";
    case 2:  return "스캔완료";
    case 9:  return "출고완료";
    case 99: return "폐기";
    default: return String(s ?? "-");
  }
}
</script>

<style scoped>
.box-qr-view {
  padding: 16px;
  max-width: 960px;
  margin: 0 auto;
  font-family: -apple-system, "Helvetica Neue", Arial, sans-serif;
}

.qr-header {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.qr-header h2 { margin: 0; font-size: 1.2rem; }
.qr-code {
  font-family: monospace;
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 6px;
  color: #333;
}

.qr-loading, .qr-error { padding: 24px; text-align: center; color: #666; }
.qr-error .btn {
  margin-top: 12px;
  padding: 6px 14px;
  border: 1px solid #888;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
}

.info-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}
.info-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.info-card-subtitle {
  font-weight: 600;
  font-family: monospace;
  color: #111;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px 16px;
  margin: 0;
}
.info-grid > div { display: flex; flex-direction: column; }
.info-grid dt { color: #6b7280; font-size: 0.85rem; }
.info-grid dd { margin: 0; font-size: 1rem; color: #111; }

.boxes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}
.boxes-table th, .boxes-table td {
  border-bottom: 1px solid #f0f0f0;
  padding: 6px 8px;
  text-align: left;
}
.boxes-table th { background: #f9fafb; color: #374151; }

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.78rem;
  font-weight: 600;
  background: #e5e7eb;
  color: #1f2937;
}
.badge.sm { padding: 1px 6px; font-size: 0.72rem; }

.badge-type { background: #2563eb; color: #fff; }
.badge-type.pallet { background: #7c3aed; }
.badge-aggregate { background: #0e7490; color: #fff; }

.badge-expiry-normal  { background: #d1fae5; color: #065f46; }
.badge-expiry-warn    { background: #fef3c7; color: #92400e; }
.badge-expiry-alert   { background: #fed7aa; color: #9a3412; }
.badge-expiry-expired { background: #fecaca; color: #991b1b; }
.badge-expiry-unknown { background: #e5e7eb; color: #4b5563; }
</style>
