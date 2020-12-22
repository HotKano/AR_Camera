package kr.anymobi.cameraarproject.util;

public class CommConst {
    ///////////////////////////////////////////////////////////
    //// 일반 상수
    ///////////////////////////////////////////////////////////
    public static final boolean DEBUGGING = true;

    ///////////////////////////////////////////////////////////
    //// 퍼미션 상수
    ///////////////////////////////////////////////////////////
    public static final int DEF_MY_PERMISSIONS_REQUEST_SEND_SMS = 6000; // 문자메시지 퍼미션 상수.
    public static final int DEF_MY_PERMISSION_SAVE_FILE = DEF_MY_PERMISSIONS_REQUEST_SEND_SMS + 1; // 파일 저장 권한 퍼미션 상수.
    public static final int PERMISSION_ALL = DEF_MY_PERMISSION_SAVE_FILE + 1; // 전체 권한 상수
    public static final int PERMISSION_ALL_NOT = PERMISSION_ALL + 1; // 전체 권한 상수
    public static final int DEF_CROP_FROM_CAMERA = PERMISSION_ALL_NOT + 1; // 크롭 카메라
    public static final int HANDLER_MSG_UPLOAD_SUS = DEF_CROP_FROM_CAMERA + 1; // 업로드 성공 핸들러
    public static final int HANDLER_MSG_SERVER_RESPONSE_FAILURE = HANDLER_MSG_UPLOAD_SUS + 1;
    public static final int CAMERA_SUS = HANDLER_MSG_SERVER_RESPONSE_FAILURE + 1;
    public static final int CAMERA_FAIL = CAMERA_SUS + 1;
}
