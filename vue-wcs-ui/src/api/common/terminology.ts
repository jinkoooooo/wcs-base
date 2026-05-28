import { defHttp } from '/@/utils/http/axios';

export const getTerminologyApi = (locale) => {
  const terminology = defHttp.get(
    {
      url: `/terminologies/resource/${locale}`,
      locale,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
    },
    {
      isTransformResponse: false,
    },
  );

  return terminology;
};
