package com.kareem.lifeos;

/** Keeps internal model terminology and third-person system copy out of LifeOS UI. */
final class UserFacingText {
    private UserFacingText(){}
    static String humanize(String value){
        String x=value==null?"":value.trim();
        x=x.replaceAll("(?i)\\bKarim's\\b","Kareem's");
        x=x.replaceAll("(?i)\\bKarim\\b","Kareem");

        // Handle both "the user" and bare "user" grammar before generic replacement.
        x=x.replaceAll("(?i)\\bthe user's\\b","your");
        x=x.replaceAll("(?i)\\buser's\\b","your");
        x=x.replaceAll("(?i)\\bthe user is\\b","you are");
        x=x.replaceAll("(?i)\\buser is\\b","you are");
        x=x.replaceAll("(?i)\\bthe user was\\b","you were");
        x=x.replaceAll("(?i)\\buser was\\b","you were");
        x=x.replaceAll("(?i)\\bthe user has\\b","you have");
        x=x.replaceAll("(?i)\\buser has\\b","you have");
        x=x.replaceAll("(?i)\\bthe user had\\b","you had");
        x=x.replaceAll("(?i)\\buser had\\b","you had");
        x=x.replaceAll("(?i)\\bthe user needs\\b","you need");
        x=x.replaceAll("(?i)\\buser needs\\b","you need");
        x=x.replaceAll("(?i)\\bthe user should\\b","you should");
        x=x.replaceAll("(?i)\\buser should\\b","you should");
        x=x.replaceAll("(?i)\\bthe user will\\b","you will");
        x=x.replaceAll("(?i)\\buser will\\b","you will");
        x=x.replaceAll("(?i)\\bthe user received\\b","you received");
        x=x.replaceAll("(?i)\\buser received\\b","you received");
        x=x.replaceAll("(?i)\\bthe user receives\\b","you receive");
        x=x.replaceAll("(?i)\\buser receives\\b","you receive");
        x=x.replaceAll("(?i)\\bthe user sent\\b","you sent");
        x=x.replaceAll("(?i)\\buser sent\\b","you sent");
        x=x.replaceAll("(?i)\\bto the user\\b","to you");
        x=x.replaceAll("(?i)\\bfor the user\\b","for you");
        x=x.replaceAll("(?i)\\bthe user\\b","you");
        x=x.replaceAll("(?i)(^|(?<=[.!?]\\s))User\\b","You");
        if(x.regionMatches(true,0,"you ",0,4))x="You "+x.substring(4);
        if(x.regionMatches(true,0,"your ",0,5))x="Your "+x.substring(5);
        return x;
    }
}
