import { genMessage } from '../helper';
import antdLocale from 'ant-design-vue/es/locale/en_US';
import { getTerminologyApi } from '/@/api/common/terminology';

const modules = import.meta.glob('./en-US/**/*.ts', { eager: true });

const label = new Object();
const menu = new Object();
const title = new Object();
const button = new Object();
const error = new Object();
const text = new Object();

(async function () {
  const terminology = await getTerminologyApi('en-US');
  // const messages = await getMessagesApi('ko-KR');
  const ko_term = terminology['en-US'];

  for (const [key] of Object.entries(ko_term)) {
    const keyarr = key.split('.');
    switch (keyarr[0]) {
      case 'label':
        label[keyarr[1]] = ko_term[key];
        break;
      case 'menu':
        menu[keyarr[1]] = ko_term[key];
        break;
      case 'title':
        title[keyarr[1]] = ko_term[key];
        break;
      case 'button':
        button[keyarr[1]] = ko_term[key];
        break;
      case 'error':
        error[keyarr[1]] = ko_term[key];
        break;
      case 'text':
        text[keyarr[1]] = ko_term[key];
        break;
    }
  }
})();

export default {
  message: {
    ...genMessage(modules, 'en-US'),
    antdLocale,
    label,
    menu,
    title,
    button,
    error,
    text,
    // messages,
  },
  dateLocale: null,
  dateLocaleName: 'en-US',
};
