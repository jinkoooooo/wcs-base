package operato.logis.samsung.consts;

public enum DimCheckMode {
    NONE(false, false, false),
    L_ONLY(true,  false, false),
    W_ONLY(false, true,  false),
    H_ONLY(false, false, true),
    LW(true,  true,  false),
    LH(true,  false, true),
    WH(false, true,  true),
    LWH(true,  true,  true);  // 모두 체크

    private final boolean checkL;
    private final boolean checkW;
    private final boolean checkH;

    DimCheckMode(boolean l, boolean w, boolean h) {
        this.checkL = l; this.checkW = w; this.checkH = h;
    }
    public boolean checkL() { return checkL; }
    public boolean checkW() { return checkW; }
    public boolean checkH() { return checkH; }

    /** "H", "W", "HW", "WH", "LH", "LWH" 등 명령 문자열 → 모드 매핑 */
    public static DimCheckMode from(String cmd) {
        if (cmd == null) return NONE;
        String s = cmd.toUpperCase().replaceAll("[^LWH]", "");
        return switch (s) {
            case "L"   -> L_ONLY;
            case "W"   -> W_ONLY;
            case "H"   -> H_ONLY;
            case "LW", "WL" -> LW;
            case "LH", "HL" -> LH;
            case "WH", "HW" -> WH;
            case "LWH", "LHW", "WLH", "WHL", "HLW", "HWL" -> LWH;
            default -> NONE;
        };
    }
}
