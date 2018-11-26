package org.coolreader.crengine;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class StrUtils {

    public static String readableFileSize(long size) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "Bi", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static boolean isEmptyStr(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean euqalsIgnoreNulls(String s1, String s2, boolean doTrim) {
        return getNonEmptyStr(s1,doTrim).equals(getNonEmptyStr(s2,doTrim));
    }

    public static String getNonEmptyStr(String s, boolean doTrim) {
        String res = s;
        if (isEmptyStr(res)) res=""; else
            if (doTrim) res = res.trim();
        return res;
    }

    public static String updateTextPre(String s, boolean bOneLine) {
        String str = s;
        if (str != null) {
            if (str.length() > 2) {
                if ((str.startsWith("\"")) && (str.endsWith("\""))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("(")) && (str.endsWith(")"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("[")) && (str.endsWith("]"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("{")) && (str.endsWith("}"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("<")) && (str.endsWith(">"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("'")) && (str.endsWith("'"))) {
                    str = str.substring(1, str.length() - 1);
                }
                boolean bOneQuote = false;
                if (
                        (str.replace("\"","").length()) == (str.length() - 1)
                   )
                    bOneQuote = true;
                if (
                        (str.replace("'","").length()) == (str.length() - 1)
                   )
                    bOneQuote = true;
                if (
                        (str.startsWith(",")) ||
                                (str.startsWith(".")) ||
                                (str.startsWith(":")) ||
                                (str.startsWith(";")) ||
                                (str.startsWith("/")) ||
                                (str.startsWith("\\")) ||
                                ((str.startsWith("\""))&&(bOneQuote)) ||
                                ((str.startsWith("'"))&&(bOneQuote)) ||
                                (str.startsWith("\n")) ||
                                (str.startsWith("\r")) ||
                                (str.startsWith("-"))
                        ) {
                    str = str.substring(1, str.length());
                }
                if (
                        (str.endsWith(",")) ||
                                (str.endsWith(".")) ||
                                (str.endsWith(":")) ||
                                (str.endsWith(";")) ||
                                (str.endsWith("/")) ||
                                (str.endsWith("\\")) ||
                                ((str.endsWith("\""))&&(bOneQuote)) ||
                                ((str.endsWith("'"))&&(bOneQuote)) ||
                                (str.endsWith("!")) ||
                                (str.endsWith("?")) ||
                                (str.endsWith("\n")) ||
                                (str.endsWith("\r")) ||
                                (str.endsWith("-"))
                        ) {
                    str = str.substring(0, str.length() - 1);
                }
            }
        }
        while (str.contains("  ")) {
            str = str.replaceAll("  ", " ");
        }
        while (str.contains("\n\n")) {
            str = str.replaceAll("\n\n", "\n");
        }
        while (str.contains("\r\r")) {
            str = str.replaceAll("\r\r", "\r");
        }
        while (str.contains("\r\n\r\n")) {
            str = str.replaceAll("\r\n\r\n", "\r\n");
        }
        if (bOneLine) {
            str = str.replaceAll("\r\n","; ");
            str = str.replaceAll("\r","; ");
            str = str.replaceAll("\n","; ");
        }
        while (str.contains("; ;")) {
            str = str.replaceAll("; ;", ";");
        }
        return str.trim();
    }

    public static String textShrinkLines(String s, boolean bOneLine) {
        String str = s;
        while (str.matches("  ")) {
            str = str.replaceAll("  ", " ");
        }
        while (str.matches("\n\n")) {
            str = str.replaceAll("\n\n", "\n");
        }
        while (str.matches("\r\r")) {
            str = str.replaceAll("\r\r", "\r");
        }
        while (str.matches("\r\n\r\n")) {
            str = str.replaceAll("\r\n\r\n", "\r\n");
        }
        if (bOneLine) {
            str = str.replaceAll("\r\n","; ");
            str = str.replaceAll("\r","; ");
            str = str.replaceAll("\n","; ");
        }
        while (str.contains("; ;")) {
            str = str.replaceAll("; ;", ";");
        }
        return str.trim();
    }

    public static String updateText(String s, boolean bOneLine) {
        String str = ""; String str2 = s;
        while (!str.equals(str2)) {
            str = str2;
            str2 = updateTextPre(str2, bOneLine);
        }
        return str2.trim();
    }

    public static String dictWordCorrection(String s){
        String str = s;
        if(str != null){
            if (str.length()>2) {
                if (
                        ((str.substring(1,2).equals("'")) || (str.substring(1,2).equals("’")))
                                &&
                                (!str.toLowerCase().substring(0,1).equals("i"))
                        ) {
                    str = str.substring(2,str.length());
                }
            }
        }
        return str;
    }

    public static String replacePuncts(String str, boolean bAddSpace) {
        String s = str;
        if (bAddSpace) {
            s = s
                    .replace(",",", ")
                    .replace(".",". ")
                    .replace(";","; ")
                    .replace(":",": ")
                    .replace("-","- ")
                    .replace("/","/ ")
                    .replace("?","? ")
                    .replace("!","! ")
                    .replace("\\","\\ ");
        } else {
            s = s.replace(",","")
                    .replace(".","")
                    .replace(";","")
                    .replace(":","")
                    .replace("-","")
                    .replace("!","")
                    .replace("?","")
                    .replace("/","")
                    .replace("\\","");
        }
        return s;
    }

    public static String updateDictSelText(String str) {
        String s = replacePuncts(str, true);
        String res = "";
        String [] arrS = s.split(" ");
        for (String ss: arrS) {
            String repl = ss.trim().toLowerCase()
                    .replace("me","smb")
                    .replace("he","smb")
                    .replace("she","smb")
                    .replace("you","smb")
                    .replace("him","smb")
                    .replace("her","smb")
                    .replace("thee","smb")
                    .replace("they","smb")
                    .replace("we","smb")
                    .replace("it","smb")
                    .replace("us","smb")
                    .replace("them","smb")
                    .replace("my","smb's")
                    .replace("mine","smb's")
                    .replace("yours","smb's")
                    .replace("your","smb's")
                    .replace("thy","smb's")
                    .replace("his","smb's")
                    .replace("its","smb's")
                    .replace("our","smb's")
                    .replace("ours","smb's")
                    .replace("their","smb's")
                    .replace("theirs","smb's")
                    .replace("hers","smb's");
            if (repl.equals("smb")||repl.equals("smb's")||
                    repl.equals("smb.")||repl.equals("smb's.")||
                    repl.equals("smb!")||repl.equals("smb's!")||
                    repl.equals("smb?")||repl.equals("smb's?")||
                    repl.equals("smb,")||repl.equals("smb's,")||
                    repl.equals("smb:")||repl.equals("smb's:")||
                    repl.equals("smb;")||repl.equals("smb's;")||
                    repl.equals("smb;")||repl.equals("smb's;")||
                    repl.equals("smb/")||repl.equals("smb's/")||
                    repl.equals("smb\\")||repl.equals("smb's\\")) res=res+" "+repl; else res=res.trim()+" "+ss.trim();
        }
        return res;
    }

    public static <T> List<T> stringToArray(String s, Class<T[]> clazz) {
        T[] arr = new Gson().fromJson(s, clazz);
        return Arrays.asList(arr); //or return Arrays.asList(new Gson().fromJson(s, clazz)); for a one-liner
    }

    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
        put("^\\d{4}$", "yyyy");
        put("^\\d{8}$", "yyyyMMdd");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        put("^\\d{2}-\\d{1,2}-\\d{1,2}$", "yy-MM-dd");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
        put("^\\d{1,2}/\\d{1,2}/\\d{2}$", "MM/dd/yy");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        put("^\\d{12}$", "yyyyMMddHHmm");
        put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
        put("^\\d{14}$", "yyyyMMddHHmmss");
        put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
    }};

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     * @see SimpleDateFormat
     */
    public static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }

    public static long parseDateLong(String sDate) {
        TimeZone tz = java.util.TimeZone.getDefault();
        Calendar c = Calendar.getInstance(tz);
        java.util.Date d = parseDate(sDate);
        if (d==null) return 0;
        c.setTime(d);
        return c.getTimeInMillis();
    }

    public static java.util.Date parseDate(String sDate)
    {
        String sDateFormat = determineDateFormat(sDate);
        if (sDateFormat!=null) {
            java.util.Date d = new java.util.Date();
            //System.out.println(sDateFormat);
            boolean passed1 = true;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(sDateFormat);
                sdf.setLenient(false);
                d = sdf.parse(sDate);
            } catch (ParseException e) {
                passed1 = false;
            }
            boolean passed2 = true;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(sDateFormat);
                sdf.setLenient(true);
                d = sdf.parse(sDate);
            } catch (ParseException e) {
                passed2 = false;
            }
            if (passed1 && passed2) { // all is ok
                return d;
            }
            if ((!passed1) && passed2) { // try to swap MM with DD
                if (sDateFormat.contains("dd")&&sDateFormat.contains("MM")&&(!sDateFormat.contains("ddd"))&&(!sDateFormat.contains("MMM"))) {
                    String sDateFormat2 = sDateFormat.replace("MM", "~~");
                    sDateFormat2 = sDateFormat2.replace("dd", "MM");
                    sDateFormat2 = sDateFormat2.replace("~~", "dd");
                    passed1 = true;
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(sDateFormat2);
                        sdf.setLenient(false);
                        d = sdf.parse(sDate);
                        return d;
                    } catch (ParseException e) {
                        passed1 = false;
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } else return null;
        return null;
    }

    public static String stripExtension (String str) {
        // Handle null case specially.
        if (str == null) return null;
        // Get position of last '.'.
        int pos = str.lastIndexOf(".");
        // If there wasn't any '.' just return the string as is.
        if (pos == -1) return str;
        // Otherwise return the string, up to the dot.
        return str.substring(0, pos);
    }


    public static String ellipsize(String title, int size) {
        if (StrUtils.isEmptyStr(title)) {
            return "";
        }
        if (title.length() <= size) {
            return title;
        }

        String substring = title.substring(0, size);
        if (substring.endsWith(" ")) {
            return substring;
        }
        return substring + " ...";

    }


}