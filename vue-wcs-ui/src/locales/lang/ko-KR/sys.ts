export default {
  api: {
    operationFailed: '동작실패',
    errorTip: '오류표시',
    errorMessage: '동작실패,시스템이상!',
    timeoutMessage: '로그인 시간이 초과되었습니다, 다시 로그인 해 주세요!',
    apiTimeoutMessage:
      '인터페이스 요청 시간이 초과되었습니다, 페이지를 새로 고치고 다시 시도하십시오!',
    apiRequestFailed: '요청오류 발생 하였습니다 , 기다려 다시 시도하십시오',
    networkException: '네트워크 이상',
    networkExceptionMsg:
      '네트워크 이상이 발생했습니다. 네트워크 연결이 제대로 되어 있는지 확인하십시오!',

    errMsg401: '사용자에게 권한이 없습니다 (네임, 아이디, 비밀번호 오류)!',
    errMsg403: '사용자는 권한은 받았지만 접근은 금지되었습니다.',
    errMsg404: '네트워크 요청 오류, 이 자원을 찾을 수 없습니다!',
    errMsg405: '네트워크 요청 오류, 요청 방법이 허용되지 않았습니다!',
    errMsg408: '네트워크 요청 시간 초과!',
    errMsg500: '서버 오류, 관리자에게 연락하세요!',
    errMsg501: '네트워크 미실현!',
    errMsg502: '네트 워크 오류!',
    errMsg503: '서비스를 사용할 수 없습니다. 서버일시적으로  과부하거나 유지보수중입니다!',
    errMsg504: '네트워크 시간 초과!',
    errMsg505: 'http 버전이 이 요청을 지원하지 않습니다!',
  },
  app: {
    logoutTip: '경고',
    logoutMessage: '로그아웃 하시겠습니까??',
    menuLoading: '메뉴 불러오는 중...',
  },
  errorLog: {
    tableTitle: '오류 로그 목록',
    tableColumnType: '유형',
    tableColumnDate: '시간',
    tableColumnFile: '파일',
    tableColumnMsg: '오류 메세지',
    tableColumnStackMsg: 'stack메세지',

    tableActionDesc: '상세내용',

    modalTitle: '오류 상세 정보',

    fireVueError: '클릭해서 vue 오류 발생',
    fireResourceError: '자원 로딩 오류 단추를 클릭.',
    fireAjaxError: '클릭해서 ajax 오류 발생',

    enableMessage: '`/src/settings/projectSetting.ts` 내의 useErrorHandle=true일때만 실행.',
  },
  exception: {
    backLogin: '로그인 화면으로 돌아가기',
    backHome: '메인 페이지로 돌아가기',
    subTitle403: '이 페이지에 접근할 수 없습니다。',
    subTitle404: '방문한 페이지가 존재하지 않습니다。',
    subTitle500: '서버보고 오류 발생。',
    noDataTitle: '현재 페이지에 데이터 없음',
    networkErrorTitle: '네트워크 오류',
    networkErrorSubTitle: '네트워크 연결이 끊겼습니다. 네트워크를 확인하십시오！',
  },
  lock: {
    unlock: '클릭하여 잠금해제',
    alert: '잠금 화면 비밀 번호 오류',
    backToLogin: '로그인 화면으로 돌아가기',
    entry: '시스템으로 들어가기',
    placeholder: '잠금 비밀 번호 혹은 사용자 비밀 번호를 입력하세요',
  },
  login: {
    backSignIn: '뒤로',
    signInFormTitle: '로그인',
    mobileSignInFormTitle: '핸드폰으로 로그인',
    qrSignInFormTitle: 'QR코드 로그인',
    signUpFormTitle: '로그인',
    forgetFormTitle: '리셋비밀번호',

    signInTitle: 'WCS(Warehouse Control System)',
    signInDesc: '',
    policy: '我xxx 프라이버시 정책에 동의',
    scanSign: `코드 스캔후 '확인'을 클릭하시면 로그인 완료됩니다.`,

    loginButton: '로그인',
    registerButton: '등록',
    rememberMe: '저장하기',
    forgetPassword: '비밀번호를 잊었나요?',
    otherSignIn: '기타로그인방식',

    // notify
    loginSuccessTitle: '로그인 성공',
    loginSuccessDesc: '환영합니다.',
    loginFailUser: '등록된 사용자가 아닙니다.',

    // placeholder
    accountPlaceholder: '계정을 입력해주세요',
    passwordPlaceholder: '비밀번호를 입력해주세요',
    smsPlaceholder: '인증번호를 입력해주세요.',
    mobilePlaceholder: '핸드폰번호를 입력해주세요.',
    policyPlaceholder: '체크후 등록 할수 있습니다',
    diffPwd: '두번 입력한 비밀 번호가 일치 하지 않습니다',

    userName: '계정',
    password: '비밀번호',
    confirmPassword: '확인비밀번호',
    email: '메일',
    smsCode: '문자인증번호',
    mobile: '핸드폰번호',
  },
};
