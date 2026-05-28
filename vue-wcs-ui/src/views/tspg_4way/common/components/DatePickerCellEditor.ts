import { createApp, h, ref, nextTick } from 'vue';
import { DatePicker } from 'ant-design-vue';
import dayjs from 'dayjs';

/**
 * TUI Grid 커스텀 셀 에디터 — ant-design-vue DatePicker
 * 검색 폼(formGenerate.ts)의 date-picker 와 동일한 valueFormat 'YYYY-MM-DD' 사용.
 * 별도 패키지 설치 불필요 — ant-design-vue, dayjs 는 이미 프로젝트 의존성.
 */
class DatePickerCellEditor {
  private el: HTMLDivElement;
  private app: any;
  private value: string | null;

  constructor(props: any) {
    this.value = this.normalize(props.value);

    this.el = document.createElement('div');
    this.el.style.width = '100%';
    this.el.style.height = '100%';

    const dateValue = ref<string | null>(this.value);
    const openRef = ref(false);

    this.app = createApp({
      setup: () => {
        // 셀 진입 직후 캘린더 자동 오픈
        nextTick(() => {
          openRef.value = true;
        });

        return () =>
          h(DatePicker, {
            value: dateValue.value ? dayjs(dateValue.value, 'YYYY-MM-DD') : null,
            valueFormat: 'YYYY-MM-DD',
            format: 'YYYY-MM-DD',
            open: openRef.value,
            allowClear: true,
            style: { width: '100%' },
            // 그리드 셀 안에 갇히지 않도록 body 에 팝업 부착
            getPopupContainer: () => document.body,
            'onUpdate:value': (v: any) => {
              if (!v) {
                dateValue.value = null;
                this.value = null;
              } else if (typeof v === 'string') {
                dateValue.value = v;
                this.value = v;
              } else if (typeof v?.format === 'function') {
                const s = v.format('YYYY-MM-DD');
                dateValue.value = s;
                this.value = s;
              }
            },
            onOpenChange: (isOpen: boolean) => {
              openRef.value = isOpen;
            },
          });
      },
    });

    this.app.mount(this.el);
  }

  /** 다양한 입력 값을 'YYYY-MM-DD' 문자열로 정규화 */
  private normalize(v: any): string | null {
    if (v == null || v === '') return null;
    if (typeof v === 'string') return v.length >= 10 ? v.substring(0, 10) : null;
    if (typeof v?.format === 'function') return v.format('YYYY-MM-DD');
    if (v instanceof Date) {
      const y = v.getFullYear();
      const m = String(v.getMonth() + 1).padStart(2, '0');
      const d = String(v.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }
    return null;
  }

  // ===== TUI Grid 인터페이스 =====
  getElement() {
    return this.el;
  }

  getValue() {
    return this.value ?? '';
  }

  mounted() {
    // 캘린더가 open=true 로 자동 표시되므로 별도 작업 없음
  }

  beforeDestroy() {
    if (this.app) {
      this.app.unmount();
      this.app = null;
    }
  }
}

export default DatePickerCellEditor;
