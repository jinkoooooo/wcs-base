import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import Icon from '@/components/Icon/Icon.vue';

export const columns: BasicColumn[] = [
  {
    title: 'id',
    dataIndex: 'title',
    width: 200,
    align: 'left',
    ifShow: false,
  },
  {
    title: '메뉴',
    dataIndex: 'title',
    width: 200,
    align: 'left',
  },
  {
    title: '메뉴명칭',
    dataIndex: 'name',
    width: 200,
    align: 'left',
  },
  {
    title: '아이콘',
    dataIndex: 'icon_path',
    width: 80,
    customRender: ({ record }) => {
      return h(Icon, { icon: record.icon_path });
    },
  },
  {
    title: 'Routing',
    dataIndex: 'routing',
  },
  {
    title: '컴퍼넌트',
    dataIndex: 'template',
  },
  {
    title: '정렬순서',
    dataIndex: 'rank',
    width: 80,
  },
  {
    title: '숨김',
    dataIndex: 'hidden_flag',
    width: 80,
    customRender: ({ record }) => {
      const status = record.hidden_flag;
      const enable = ~~status === 0;
      const color = enable ? 'green' : 'red';
      const text = enable ? '사용' : '숨김';
      return h(Tag, { color: color }, () => text);
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'menuName',
    label: '메뉴이름',
    component: 'Input',
    colProps: { span: 8 },
  },
  {
    field: 'status',
    label: '상태',
    component: 'Select',
    componentProps: {
      options: [
        { label: '사용', value: '0' },
        { label: '중지', value: '1' },
      ],
    },
    colProps: { span: 8 },
  },
];
