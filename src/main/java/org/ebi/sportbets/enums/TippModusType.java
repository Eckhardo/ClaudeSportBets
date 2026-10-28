package org.ebi.sportbets.enums;

public enum TippModusType {

    TIPPMODUS_TOTO("TotoTipp"),
    TIPPMODUS_POINT("PointTipp"),
    TIPPMODUS_RESULT("ResultTipp");

    private final String displayName;

    TippModusType(String displayName) {
        this.displayName = displayName;
    }

    public static TippModusType fromString(String displayName) {

        return switch (displayName) {
            case "TotoTipp" -> TIPPMODUS_TOTO;
            case "PointTipp" -> TIPPMODUS_POINT;
            case "ResultTipp" -> TIPPMODUS_RESULT;
            default -> null;
        };


    }

    public static String fromEnum(TippModusType type) {

        return type.getDisplayName();
    }

    public String getDisplayName() {
        return displayName;
    }

}
