import { computed } from 'vue';
import { useUserStore } from '/@/store/modules/user';
import { RoleEnum } from '/@/enums/roleEnum';

/**
 * MapEditor 권한 컴포저블
 *
 * super 역할 보유 시에만 편집/저장/삭제/타입관리/페이지관리 등이 허용됨.
 */
export function useEditorPermissions() {
  const userStore = useUserStore();
  const roles = computed<string[]>(() => (userStore.getRoleList ?? []) as string[]);
  const isSuper = computed(() => roles.value.includes(RoleEnum.SUPER));

  return {
    isSuper,
    canEnterEditor: isSuper,
    canSave: isSuper,
    canBulkEdit: isSuper,
    canManagePages: isSuper,
    canManageTypes: isSuper,
    canDeleteEquipment: isSuper,
  };
}
