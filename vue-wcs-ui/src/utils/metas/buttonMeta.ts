import { useI18n } from '/@/hooks/web/useI18n';

const { t } = useI18n();

export const parseButtons = function (buttonList) {
  if (buttonList) {
    buttonList.forEach(function (button) {
      button.id = button.text + 'Btn';
	  button.text = t(`button.${button.text}`);
	  button.listener = button.id + 'Handler';
    });
  }

  return buttonList;
};
