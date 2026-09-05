package com.kareem.lifeos;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Search normalization, candidate scoring and critical-token extraction. Never mutates raw OCR text. */
final class OcrQuality {
    private static final Pattern AR=Pattern.compile("[\\u0600-\\u06FF]");
    private static final Pattern TOKEN=Pattern.compile("(?i)(?:https?://\\S+|[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}|(?:EGP|USD|EUR|AED|SAR|LE|ج\\.?م|جنيه|ريال|دولار|€|\\$|£)\\s*[0-9٠-٩][0-9٠-٩,.]*|[0-9٠-٩][0-9٠-٩,.]*\\s*(?:EGP|USD|EUR|AED|SAR|LE|ج\\.?م|جنيه|ريال|دولار|€|\\$|£)|\\+?[0-9٠-٩][0-9٠-٩ ()-]{7,}[0-9٠-٩]|\\b[0-9٠-٩]{1,2}[/-][0-9٠-٩]{1,2}[/-][0-9٠-٩]{2,4}\\b|\\b[0-9٠-٩]{1,2}:[0-9٠-٩]{2}\\b)");
    private OcrQuality(){}
    static String normalizeForSearch(String x){String s=x==null?"":x.toLowerCase(Locale.ROOT);s=s.replace("ـ","").replaceAll("[\\u064B-\\u065F\\u0670]","");s=s.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي');String ar="٠١٢٣٤٥٦٧٨٩";for(int i=0;i<10;i++)s=s.replace(ar.charAt(i),(char)('0'+i));return s.replaceAll("[^\\p{L}\\p{N}@+./:%$£€]+"," ").replaceAll("\\s+"," ").trim();}
    static float score(String text){String t=text==null?"":text.trim();if(t.isEmpty())return 0;int useful=0,junk=0;for(char c:t.toCharArray()){if(Character.isLetterOrDigit(c))useful++;else if(!Character.isWhitespace(c)&&",.:;!?()[]{}@#%+-_/\\$£€\"'".indexOf(c)<0)junk++;}double density=(double)useful/Math.max(1,t.length()),junkRate=(double)junk/Math.max(1,t.length());double len=Math.min(1.0,t.length()/400.0);return (float)Math.max(0,Math.min(1,.48*density+.32*(1-junkRate)+.20*len));}
    static String scripts(String text){int ar=0,lat=0,dig=0;for(char c:(text==null?"":text).toCharArray()){if(c>='A'&&c<='Z'||c>='a'&&c<='z')lat++;else if(c>='0'&&c<='9'||c>='٠'&&c<='٩')dig++;else if(AR.matcher(String.valueOf(c)).find())ar++;}if(ar>0&&lat>0)return "Arabic + Latin";if(ar>0)return "Arabic";if(lat>0)return "Latin";if(dig>0)return "Numeric";return "Unknown";}
    static List<String> criticalTokens(String text){LinkedHashSet<String> set=new LinkedHashSet<>();Matcher m=TOKEN.matcher(text==null?"":text);while(m.find()&&set.size()<40){String x=m.group().trim();if(!x.isEmpty())set.add(x);}return new ArrayList<>(set);}
    static String mergeDistinct(String a,String b){LinkedHashSet<String> lines=new LinkedHashSet<>();add(lines,a);add(lines,b);StringBuilder out=new StringBuilder();for(String x:lines){if(out.length()>0)out.append('\n');out.append(x);}return out.toString().trim();}
    private static void add(LinkedHashSet<String> set,String text){if(text==null)return;for(String line:text.split("\\r?\\n")){String x=line.trim();if(x.isEmpty())continue;String n=normalizeForSearch(x);boolean duplicate=false;for(String old:set)if(normalizeForSearch(old).equals(n)){duplicate=true;break;}if(!duplicate)set.add(x);}}
}
