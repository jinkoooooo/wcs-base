import { defHttp } from '/@/utils/http/axios';
import { useUserStore } from '/@/store/modules/user';
import { notification } from 'ant-design-vue';

/**
 * 범용 파일 첨부 셀 Renderer.
 *
 * 메뉴 메타에 grid_editor='file-upload' 만 지정하면 모든 화면에서 재사용.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 핵심 약속: 셀의 값(row[컬럼명]) 자체가 file_attachment.id
 * ─────────────────────────────────────────────────────────────────────────────
 *  - 업로드 성공 → setValue(rowKey, columnName, file_id)
 *  - 저장 시 페이지는 row[컬럼명] 으로 file_id 한 번에 접근
 *  - 파일명은 캐시(업로드 직후) 또는 "📎 첨부됨" 라벨로 표시
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 메뉴 메타 매핑 (gridMeta.ts 에서 renderer.options 로 전달)
 * ─────────────────────────────────────────────────────────────────────────────
 *   ref_type         'pdf' | 'excel' | 'image' | 그 외 직접 문자열 → input accept
 *   ref_related      'category=qc_test' 같이 업로드 카테고리 지정 (선택)
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * 동작
 * ─────────────────────────────────────────────────────────────────────────────
 *  단일 클릭 — 셀에 값(file_id) 있으면 다운로드, 없으면 파일 선택 → 업로드
 *  더블 클릭 — 교체 업로드 (이미 첨부된 파일 교체는 관리자만 허용)
 * ─────────────────────────────────────────────────────────────────────────────
 */

// 업로드 직후 파일명을 셀에 보여주기 위한 모듈 캐시 (file_id → file_name).
// 페이지 리로드 후엔 빈 상태에서 시작하지만 정상 — '📎 첨부됨' 라벨이 fallback.
const fileNameCache = new Map<string, string>();

export default class FileUploadCellRenderer {
  el: HTMLElement;
  props: any;
  // 더블클릭 vs 단일클릭 구분
  private clickTimer: any = null;

  constructor(props: any) {
    this.props = props;
    const el = document.createElement('div');
    el.style.cssText =
      'display:flex;align-items:center;height:100%;cursor:pointer;padding:0 4px;overflow:hidden;';

    el.addEventListener('click', (e) => {
      e.stopPropagation();
      if (this.clickTimer) return;
      this.clickTimer = setTimeout(() => {
        this.clickTimer = null;
        this.handleSingleClick();
      }, 220);
    });
    el.addEventListener('dblclick', (e) => {
      e.stopPropagation();
      if (this.clickTimer) {
        clearTimeout(this.clickTimer);
        this.clickTimer = null;
      }
      this.handleDoubleClick();
    });

    this.el = el;
    this.render(props);
  }

  getElement() {
    return this.el;
  }

  render(props: any) {
    this.props = props;
    const fileId = props.value;
    const hasFile = !!fileId;
    const displayName = hasFile ? fileNameCache.get(String(fileId)) : null;

    let label: string;
    let color: string;
    if (hasFile && displayName) {
      label = `📎 ${displayName}`;
      color = '#1677ff';
    } else if (hasFile) {
      // 일반 사용자에겐 '교체' 안내 문구 숨김 (교체는 admin 만 가능).
      const tail = this.isAdmin()
        ? ' (클릭하여 다운로드, 더블클릭하여 교체)'
        : ' (클릭하여 다운로드)';
      label = `📎 첨부됨${tail}`;
      color = '#1677ff';
    } else {
      label = '📎 클릭하여 파일 첨부';
      color = '#ff4d4f';
    }
    this.el.innerHTML = `<span style="color:${color};white-space:nowrap;overflow:hidden;text-overflow:ellipsis;width:100%;">${escapeHtml(
      label,
    )}</span>`;
  }

  // 단일 클릭: 파일 있으면 다운로드, 없으면 파일 선택
  private handleSingleClick() {
    const fileId = this.props.value;
    if (fileId) {
      const displayName = fileNameCache.get(String(fileId)) || 'file';
      this.downloadFile(String(fileId), displayName);
      return;
    }
    this.pickAndUpload();
  }

  // defHttp 로 blob 다운로드 → 임시 URL 로 새 창 오픈.
  // window.open 직접 호출은 Vite proxy 미적용으로 실패함.
  private async downloadFile(fileId: string, displayName: string) {
    try {
      const res: any = await defHttp.get(
        { url: `/wcs/file-attachment/${encodeURIComponent(fileId)}`, responseType: 'blob' },
        { isTransformResponse: false, isReturnNativeResponse: true },
      );
      const blob: Blob = res?.data;
      if (!blob) {
        console.error('[FileUploadCellRenderer] download empty', res);
        return;
      }
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = displayName || 'file';
      a.target = '_blank';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      console.error('[FileUploadCellRenderer] download failed', e);
    }
  }

  // 더블 클릭: 교체용 파일 선택 — 이미 첨부된 파일의 교체는 admin 만 허용
  private handleDoubleClick() {
    if (this.props.value && !this.isAdmin()) {
      notification.warning({
        message: '권한 부족',
        description: '이미 첨부된 파일의 교체는 관리자만 가능합니다.',
      });
      return;
    }
    this.pickAndUpload();
  }

  // 파일 선택 → 즉시 업로드 → 셀 값(=file_id) 갱신
  private pickAndUpload() {
    const opts = this.options();
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = opts.accept;
    input.style.display = 'none';
    input.addEventListener('change', async () => {
      const f = input.files?.[0];
      document.body.removeChild(input);
      if (!f) return;

      try {
        // 기존 첨부가 있으면 백엔드 replace 엔드포인트(POST /wcs/file-attachment/{id}) 호출 → id 유지.
        // 없으면 신규 업로드(POST /wcs/file-attachment). prefix 부착은 Axios.uploadFile 이 담당.
        const existingId = this.props.value;
        const url = existingId
          ? `/wcs/file-attachment/${encodeURIComponent(String(existingId))}`
          : '/wcs/file-attachment';
        const res: any = await defHttp.uploadFile(
          { url },
          {
            name: 'file',
            file: f,
            data: opts.category ? { category: opts.category } : {},
          },
        );
        const payload = res?.data ?? res;
        const fileId = payload?.id;
        const fileName = payload?.file_name ?? f.name;
        if (!fileId) {
          console.error('[FileUploadCellRenderer] upload response missing id', payload);
          return;
        }
        // 파일명은 디스플레이용으로만 캐시 — 저장/전송 데이터에는 영향 없음.
        fileNameCache.set(String(fileId), String(fileName));
        // 셀 값 = file_id. 이 한 줄로 row dirty 마킹 + tui-grid 영속 + 페이지 접근 가능.
        // columnName 은 trim — 메뉴 메타에 trailing space 가 섞여도 페이지 측 키와
        // 일치하도록 정규화 (실제 관측된 케이스 방어).
        const columnName = String(this.props.columnInfo.name || '').trim();
        this.props.grid.setValue(this.props.rowKey, columnName, fileId);

        // replace 엔드포인트는 동일 file_id 를 반환 → setValue 가 값 동일로 no-op →
        // tui-grid 가 cell 재렌더를 스킵. 파일명만 바뀐 경우를 화면에 즉시 반영하려면
        // 수동 render 호출이 필요하다. (신규 업로드는 값이 변하므로 자동 재렌더됨.)
        if (existingId && String(existingId) === String(fileId)) {
          this.render(this.props);
        }

        // 영속 검증 + 진단 로그.
        //   gridMeta.ts 가 file-upload 컬럼을 grid_rank 무관 등록하므로 정상 케이스엔 영속됨.
        //   영속 실패는 극단 케이스만 발생.
        const after = this.props.grid.getRow(this.props.rowKey);
        const persisted = !!after && String(after[columnName]) === String(fileId);
        if (!persisted) {
          console.warn(
            `[FileUploadCellRenderer] 영속 실패 — 컬럼 "${columnName}" 이 tui-grid 에 ` +
              `등록되지 않았습니다. 메뉴 메타에 grid_editor='file-upload' 가 설정되어 ` +
              `있는지 확인하세요. (현재 row 값: ${after?.[columnName]})`,
          );
        } else {
          console.info(
            `[FileUploadCellRenderer] 업로드 완료 — rowKey=${this.props.rowKey} ` +
              `column="${columnName}" file_id="${fileId}" file_name="${fileName}"`,
          );
        }
      } catch (e) {
        console.error('[FileUploadCellRenderer] upload failed', e);
      }
    });
    document.body.appendChild(input);
    input.click();
  }

  // 첨부된 PDF 교체는 admin 만 허용. usePermissionLocal.ts 의 isAdmin 판정 규칙과 일치.
  private isAdmin(): boolean {
    try {
      const info: any = useUserStore().getUserInfo || {};
      const roles: string[] = info.roles || [];
      return roles.includes('super') || roles.includes('admin');
    } catch {
      return false;
    }
  }

  private options() {
    const raw = this.props.columnInfo?.renderer?.options || {};
    return {
      accept: raw.accept || '*',
      category: raw.category || '',
    };
  }
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => {
    switch (c) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '"':
        return '&quot;';
      default:
        return '&#39;';
    }
  });
}
