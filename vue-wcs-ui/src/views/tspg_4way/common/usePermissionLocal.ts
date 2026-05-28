/**
 * tspg_4way / kmat_2026 화면 전용 메뉴-단위 권한 hook.
 * 공유 hook(usePermission.ts) 미수정 — 다른 프로젝트 영향 0.
 *
 * 다른 화면들과 동일한 axios 호출 패턴(getCommonGetListApi) 사용.
 * defHttp 의 baseURL / auth 헤더 / 에러 인터셉터를 그대로 활용한다.
 *
 * 백엔드(TspgPermissionController): admin → ["all"], 일반 → 권한 부분 집합,
 * 메뉴 정의 없음 / 권한 없음 → []. 404 는 정상 운영 흐름에 나지 않으므로
 * 인터셉터 자동 logout 트리거되지 않는다.
 *
 * 사용 예:
 *   const { can, isAdmin, loaded } = usePermissionLocal('OutboundInstruction');
 *   <a-button v-if="can('create')" @click="addRow">추가</a-button>
 */
import { ref, computed, onMounted, type Ref } from 'vue';
import { getCommonGetListApi } from '/@/api/common/api';
import { useUserStore } from '/@/store/modules/user';

type Action = 'show' | 'create' | 'update' | 'delete';

const cache = new Map<string, Ref<string[]>>();
const inflight = new Map<string, Promise<string[]>>();

async function fetchActions(menuName: string): Promise<string[]> {
  if (inflight.has(menuName)) return inflight.get(menuName)!;
  // 다른 화면과 동일한 axios 호출 패턴 — backend raw path 만 전달.
  // defHttp 의 baseURL(/api or /rest) 자동 prefix + Authorization 헤더 자동 부착.
  const url = `/wcs/permissions/menu/${encodeURIComponent(menuName)}?_t=${Date.now()}`;
  const p = getCommonGetListApi(url, null)
    .then((data: any) => (Array.isArray(data) ? (data as string[]) : []))
    .catch(() => [] as string[])
    .finally(() => inflight.delete(menuName));
  inflight.set(menuName, p);
  return p;
}

export function usePermissionLocal(menuName: string) {
  if (!cache.has(menuName)) cache.set(menuName, ref<string[]>([]));
  const auths = cache.get(menuName)!;
  const loaded = ref(false);
  const userStore = useUserStore();  // ← 추가

  async function refresh() {
    const list = await fetchActions(menuName);
    auths.value = list;
    loaded.value = true;
  }

  onMounted(() => {
    if (auths.value.length === 0) refresh();
    else loaded.value = true;
  });

  // 관리자: 권한 API 응답 'all' 또는 userStore 의 super/admin role
  const isAdmin = computed(() => {
    if (auths.value[0] === 'all') return true;
    const info: any = userStore.getUserInfo || {};
    const roles: string[] = info.roles || [];
    return roles.includes('super') || roles.includes('admin');
  });

  // 백엔드 계약: ["all"] = 관리자, [...] = 일반(허용 액션 부분집합), [] = 권한 없음
  // 로딩 중에는 false 로 숨겨 권한 확인 전 노출을 방지한다.
  function can(_a: Action): boolean {
    if (isAdmin.value) return true;
    if (!loaded.value) return false;
    return auths.value.includes(_a);
  }

  return { can, isAdmin, loaded, refresh };
}
