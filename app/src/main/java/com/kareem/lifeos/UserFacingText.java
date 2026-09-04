package com.kareem.lifeos;

/** Keeps internal model terminology and third-person system copy out of LifeOS UI. */
final class UserFacingText {
    private UserFacingText(){}
    static String humanize(String value){
        String x=value==null?"":value.trim();
        x=x.replaceAll("(?i)\\bKarim's\\b","Kareem's");
        x=x.replaceAll("(?i)\\bKarim\\b","Kareem");
        x=x.replaceAll("(?i)\\bthe user's\\b","your");
        x=x.replaceAll("(?i)\\bthe user is\\b","you are");
        x=x.replaceAll("(?i)\\bthe user has\\b","you have");
        x=x.replaceAll("(?i)\\bthe user needs\\b","you need");
        x=x.replaceAll("(?i)\\bthe user should\\b","you should");
        x=x.replaceAll("(?i)\\bthe user will\\b","you will");
        x=x.replaceAll("(?i)\\bto the user\\b","to you");
        x=x.replaceAll("(?i)\\bfor the user\\b","for you");
        x=x.replaceAll("(?i)\\bthe user\\b","you");
        x=x.replaceAll("(?i)\\buser's\\b","your");
        x=x.replaceAll("(?i)(^|(?<=[.!?]\\s))User\\b","You");
        x=x.replaceAll("(?i)\\bUser received\\b","You received");
        return x;
    }
}
