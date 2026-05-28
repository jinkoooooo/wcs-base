import { FormSchema } from '/@/components/Table';
import { getParentMenuList } from '/@/api/sys/menu';
import { getCommonCodeByName } from '/@/api/common/api';
import { useI18n } from '/@/hooks/web/useI18n';
const { t } = useI18n();

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    show: false,
  },
  {
    field: 'parent_id',
    label: t('label.parent'),
    component: 'ApiSelect',
    componentProps: {
      api: getParentMenuList,
      labelField: 'text',
      valueField: 'value',
      immediate: true,
    },
  },
  {
    field: 'name',
    label: t('label.name'),
    component: 'Input',
    required: true,
  },
  {
    field: 'rank',
    label: t('label.rank'),
    component: 'Input',
    required: true,
  },
  {
    field: 'category',
    label: t('label.category'),
    component: 'ApiSelect',
    componentProps: {
      // more details see /src/components/Form/src/components/ApiSelect.vue
      api: getCommonCodeByName,
      params: 'MENU_CATEGORY',
      // use name as label
      labelField: 'text',
      // use id as value
      valueField: 'value',
      // not request untill to select
      immediate: true,
    },
    defaultValue: 'STANDARD',
    required: true,
  },
  {
    field: 'menu_type',
    label: t('label.menu_type'),
    component: 'ApiSelect',
    componentProps: {
      // more details see /src/components/Form/src/components/ApiSelect.vue
      api: getCommonCodeByName,
      params: 'MENU_TYPE',
      // use name as label
      labelField: 'text',
      // use id as value
      valueField: 'value',
      // not request untill to select
      immediate: true,
    },
    defaultValue: 'SCREEN',
    required: true,
  },
  {
    field: 'resource_type',
    label: t('label.resource_type'),
    component: 'Select',
    componentProps: {
      options: [
        {
          label: 'ENTITY',
          value: 'ENTITY',
        },
        {
          label: 'DIY_SERVICE',
          value: 'DIY_SERVICE',
        },
        {
          label: 'DIY_GRID',
          value: 'DIY_GRID',
        },
      ],
    },
    defaultValue: 'ENTITY',
  },
  /*{
    field: 'routing_type',
    label: t('label.routing_type'),
    component: 'ApiSelect',
    componentProps: {
      // more details see /src/components/Form/src/components/ApiSelect.vue
      api: getCommonCodeByName,
      params: 'ROUTING_TYPE',
      // use name as label
      labelField: 'text',
      // use id as value
      valueField: 'value',
      // not request untill to select
      immediate: true,
    },
  },
  */
  {
    field: 'routing',
    label: t('label.routing'),
    component: 'Input',
  },
  {
    field: 'template',
    label: t('label.path'),
    component: 'Input',
  },
  // {
  //   field: 'resource_type',
  //   label: t('label.resource_type'),
  //   component: 'ApiSelect',
  //   componentProps: {
  //     api: getCommonCodeByName,
  //     params: 'RESOURCE_TYPE',
  //     labelField: 'text',
  //     valueField: 'value',
  //     immediate: true,
  //   },
  // },
  {
    field: 'resource_name',
    label: t('label.resource_name'),
    component: 'Input',
  },
  {
    field: 'resource_url',
    label: t('label.resource_url'),
    component: 'Input',
  },
  {
    field: 'grid_save_url',
    label: t('label.grid_save_url'),
    component: 'Input',
  },
  /* {
    field: 'detail_layout',
    label: t('label.detail_layout'),
    component: 'Input',
  }, 
  {
    field: 'id_field',
    label: t('label.id_field'),
    component: 'Input',
  },
   */
  {
    field: 'desc_field',
    label: t('label.desc_field'),
    component: 'Input',
  },
  {
    field: 'pagination',
    label: t('label.pagination'),
    component: 'Input',
  },
  /* {
    field: 'items_prop',
    label: t('label.items_prop'),
    component: 'Input',
    defaultValue: 'items',
  }, 
  {
    field: 'total_prop',
    label: t('label.total_prop'),
    component: 'Input',
    defaultValue: 'total',
  },
  */
  {
    field: 'fixed_columns',
    label: t('label.fixed_columns'),
    component: 'Input',
    defaultValue: 0,
  },
  {
    field: 'icon_path',
    label: t('label.icon'),
    component: 'IconPicker',
  },
  {
    field: 'hidden_flag',
    label: '숨김 여부',
    component: 'RadioButtonGroup',
    defaultValue: '0',
    componentProps: {
      options: [
        { label: '사용', value: '0' },
        { label: '숨김', value: '1' },
      ],
    },
  },
  // {
  //   field: 'description',
  //   label: t('label.description'),
  //   component: 'InputTextArea',
  // },
];
