import * as XLSX from 'xlsx';

// ✅ 템플릿 다운로드 (public에 둔 파일을 그대로 다운로드)
export async function downloadTemplate(url = '/excel/inbound_import_template.xlsx', filename = 'inbound_import_template.xlsx') {
  const res = await fetch(url)
  if (!res.ok) throw new Error('템플릿 파일을 불러오지 못했습니다.')

  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(a.href)
}

export type ImportRow = {
  bl_no: string
  cntr_no: string
  item_type?: string
  item_desc?: string
  item_code: string
  item_qty: number
  inbound_date: string // "YYYY-MM-DD"
}

const EXPECT_HEADERS = [
  'bl_no',
  'cntr_no',
  'item_type',
  'item_desc',
  'item_code',
  'item_qty',
  'inbound_date',
]

const REQUIRED_KEYS = ['bl_no', 'cntr_no', 'item_code', 'item_qty', 'inbound_date'] as const

export async function parseInboundImportXlsx(file: File): Promise<ImportRow[]> {
  const buf = await file.arrayBuffer()

  // ✅ 날짜셀을 Date로 읽기
  const wb = XLSX.read(buf, { type: 'array', cellDates: true })
  const ws = wb.Sheets[wb.SheetNames[0]]
  if (!ws) return []

  // ✅ 2차원 배열로 읽기 (헤더/데이터를 우리가 직접 컨트롤)
  const table = XLSX.utils.sheet_to_json<any[]>(ws, {
    header: 1,
    defval: '',
    blankrows: false,
  })

  if (!table || table.length < 2) return []

  // 1) 헤더(A~G)만 사용
  const headerRow = (table[0] ?? []).slice(0, 7).map((v: any) => normalizeHeader(v))

  // ✅ 헤더 검증(정확히 DB컬럼명으로 들어왔는지)
  // - 완전 일치 강제하고 싶으면 아래 if 사용
  const headerJoined = headerRow.join('|')
  const expectJoined = EXPECT_HEADERS.join('|')
  if (headerJoined !== expectJoined) {
    throw new Error(
      `[엑셀 헤더 오류]\n` +
      `A~G 헤더는 아래 순서/이름이어야 합니다.\n` +
      `기대: ${EXPECT_HEADERS.join(', ')}\n` +
      `실제: ${headerRow.join(', ')}`
    )
  }

  // 2) 데이터 행(A~G) 파싱
  const result: ImportRow[] = []
  const errors: string[] = []

  for (let i = 1; i < table.length; i++) {
    const excelRowNo = i + 1 // 엑셀 기준 줄번호(1부터)
    const row = (table[i] ?? []).slice(0, 7)

    // 빈줄 스킵 (A~G가 전부 빈 값이면)
    if (row.every((v: any) => asString(v) === '')) continue

    const obj: any = {}
    for (let c = 0; c < headerRow.length; c++) {
      obj[headerRow[c]] = row[c]
    }

    // ✅ 필드 정규화/변환
    const dto: any = {}
    dto.bl_no = asString(obj.bl_no)
    dto.cntr_no = asString(obj.cntr_no)
    dto.item_type = asString(obj.item_type)
    dto.item_desc = asString(obj.item_desc)
    dto.item_code = asString(obj.item_code)

    // user가 "item_qty"라고 적었지만 실제 헤더는 item_qty라서 item_qty로 처리
    dto.item_qty = asInt(obj.item_qty)

    // inbound_date: "YYYYMMDD" / "YYYY-MM-DD" / Date 모두 허용 -> "YYYY-MM-DD"
    dto.inbound_date = normalizeInboundDate(obj.inbound_date)

    // ✅ 필수값 검증
    for (const k of REQUIRED_KEYS) {
      if (k === 'item_qty') {
        if (!Number.isFinite(dto.item_qty) || dto.item_qty <= 0) {
          errors.push(`[${excelRowNo}행] item_qty는 1 이상의 숫자여야 합니다.`)
        }
        continue
      }

      if (!dto[k] || String(dto[k]).trim() === '') {
        errors.push(`[${excelRowNo}행] ${k} 필수값이 누락되었습니다.`)
      }
    }

    // ✅ 추가 검증(원하면 더 강화 가능)
    if (dto.bl_no && dto.bl_no.length > 50) errors.push(`[${excelRowNo}행] bl_no 길이가 너무 깁니다(>50).`)
    if (dto.cntr_no && dto.cntr_no.length > 20) errors.push(`[${excelRowNo}행] cntr_no 길이가 너무 깁니다(>20).`)
    if (dto.item_code && dto.item_code.length > 100) errors.push(`[${excelRowNo}행] item_code 길이가 너무 깁니다(>100).`)

    // 에러가 있으면 result에는 안 넣고 다음 행
    if (errors.some(e => e.startsWith(`[${excelRowNo}행]`))) continue

    result.push(dto as ImportRow)
  }

  if (errors.length) {
    throw new Error(
      `[엑셀 검증 실패] 총 ${errors.length}건\n` +
      errors.slice(0, 50).join('\n') +
      (errors.length > 50 ? `\n... 외 ${errors.length - 50}건` : '')
    )
  }

  return result
}

/** -------- helpers -------- */

function normalizeHeader(v: any) {
  return asString(v).trim()
}

function asString(v: any): string {
  if (v === null || v === undefined) return ''
  // 숫자도 문자열로 유지 (bl_no 등)
  return String(v).trim()
}

function asInt(v: any): number {
  if (v === null || v === undefined || v === '') return NaN
  const n = Number(String(v).replace(/,/g, '').trim())
  if (!Number.isFinite(n)) return NaN
  return Math.trunc(n)
}

/**
 * inbound_date 허용:
 * - "20251206" (YYYYMMDD)
 * - "2025-12-06" (YYYY-MM-DD)
 * - Date (엑셀 날짜셀)
 */
function normalizeInboundDate(v: any): string {
  if (v instanceof Date && !isNaN(v.getTime())) {
    return toYmd(v)
  }

  const s = asString(v)
  if (!s) return ''

  // YYYY-MM-DD
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s

  // YYYYMMDD
  if (/^\d{8}$/.test(s)) {
    const y = s.slice(0, 4)
    const m = s.slice(4, 6)
    const d = s.slice(6, 8)
    const yyyyMmDd = `${y}-${m}-${d}`
    if (!isValidYmd(yyyyMmDd)) return ''
    return yyyyMmDd
  }

  // 기타 포맷은 불허(필요하면 여기 확장)
  return ''
}

function toYmd(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function isValidYmd(s: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false
  const [y, m, d] = s.split('-').map(Number)
  const dt = new Date(y, m - 1, d)
  return dt.getFullYear() === y && dt.getMonth() === m - 1 && dt.getDate() === d
}
